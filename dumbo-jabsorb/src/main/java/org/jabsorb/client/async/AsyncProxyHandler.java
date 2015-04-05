/**
 *
 */
package org.jabsorb.client.async;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jabsorb.JSONSerializer;
import org.jabsorb.client.ErrorResponse;
import org.jabsorb.serializer.MarshallException;
import org.jabsorb.serializer.SerializerState;
import org.jabsorb.serializer.response.results.FailedResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AsyncProxyHandler implements InvocationHandler {
		// AsyncProxy methods
		private static final Method METHOD_GET_FUTURE_RESULT;
		private static final Method METHOD_SET_RESULT_CALLBACK;

		static {
			try {
				METHOD_GET_FUTURE_RESULT = AsyncProxy.class.getDeclaredMethod("getFutureResult", new Class<?>[0]);
				METHOD_SET_RESULT_CALLBACK = AsyncProxy.class.getDeclaredMethod("setResultCallback", AsyncResultCallback.class);
			} catch (final Exception e) {
				throw new ExceptionInInitializerError(e);
			}
		}

		private final String proxyKey;
		private final AsyncSession session;
		private final JSONSerializer serializer;

		public AsyncProxyHandler(final String proxyKey, final AsyncSession session, final JSONSerializer serializer) {
			this.proxyKey = proxyKey;
			this.session = session;
			this.serializer = serializer;
		}

		private Future<Object> futureResult;

		private AsyncResultCallback<Object, Object, Method> resultCallback;

		@SuppressWarnings("unchecked")
		public Object invoke(final Object proxyObj, final Method method, final Object[] args) throws Exception {
			assert (proxyObj instanceof AsyncProxy) : "Proxy object is not created by AsyncClient?";

			final String methodName = method.getName();

			if (methodName.equals("hashCode")) {
				return new Integer(System.identityHashCode(proxyObj));
			} else if (methodName.equals("equals")) {
				return (proxyObj == args[0] ? Boolean.TRUE : Boolean.FALSE);
			} else if (methodName.equals("toString")) {
				return proxyObj.getClass().getName() + '@'
						+ Integer.toHexString(proxyObj.hashCode());
			} else if (METHOD_GET_FUTURE_RESULT.equals(method)) {
				return futureResult;
			} else if (METHOD_SET_RESULT_CALLBACK.equals(method)) {

				setResultCallback((AsyncResultCallback<Object, Object, Method>) args[0]);
				return null;
			}

			return doInvoke(proxyObj, method, args);
		}

		/**
		 * Invokes a method for the asynchronous client and returns null immediately
		 *
		 * @param objectTag
		 *            (optional) the name of the object to invoke the method on. May
		 *            be null.
		 * @param method
		 *            The method to call.
		 * @param args
		 *            The arguments to the method.
		 * @param returnType
		 *            What should be returned
		 * @return Always null
		 * @throws Exception
		 *             JSONObject, UnmarshallExceptions or Exceptions from invoking
		 *             the method may be thrown.
		 */
		private Object doInvoke(final Object proxyObject, final Method method, final Object[] args) throws Exception {
			// Create a final reference that can be used when this method
			// returns. We don't know if the original callback will still be
			// the same.
			final AsyncResultCallback<Object, Object, Method> currentCallback = resultCallback;

			final ExceptionSettableFuture<Object> future = new ExceptionSettableFuture<Object>();
			setFutureResult(future);

			final JSONObject message = createInvokeMessage(proxyKey, method.getName(), args);

			final AsyncResultCallback<AsyncSession, JSONObject, JSONObject> jsonResultCallback = new AsyncResultCallback<AsyncSession, JSONObject, JSONObject>() {
				public void onAsyncResult(final AsyncSession source, final Future<JSONObject> response, final JSONObject request) {
					// get the response
					try {
						final JSONObject responseMessage = response.get();

						// convert the response
						final Object resultObject = convertResponseMessage(responseMessage, method.getReturnType());

						// set the result onto the future that is used
						future.set(resultObject);
					} catch (final ExecutionException e) {
						// deal with exceptions in the future
						future.setException(e);
					} catch (final Exception e) {
						// deal with exceptions in the future
						future.setException(new ExecutionException(e));
					}

					// call the callback that was set when invoke was called
					currentCallback.onAsyncResult(proxyObject, future, method);
				}
			};

			// invoke the method by sending the message
			session.send(message, jsonResultCallback);

			// return null as fast as you can
			return null;
		}

		/**
		 * Gets the id of the next message
		 *
		 * @return The id for the next message.
		 */
		protected String getId() {
			return UUID.randomUUID().toString();
		}

		/**
		 * Generate and throw exception based on the data in the 'responseMessage'
		 *
		 * @param responseMessage
		 *            The error message
		 * @throws JSONException
		 *             Rethrows the exception in the repsonse.
		 */
		protected void processException(final JSONObject responseMessage) throws JSONException {
			final JSONObject error = (JSONObject) responseMessage.get("error");
			if (error != null) {
				final Integer code = new Integer(error.has("code") ? error.getInt("code") : 0);
				final String trace = error.has("trace") ? error.getString("trace") : null;
				final String msg = error.has("msg") ? error.getString("msg") : null;
				throw new ErrorResponse(code, msg, trace);
			}
			throw new ErrorResponse(new Integer(FailedResult.CODE_ERR_PARSE),
					"Unknown response:" + responseMessage.toString(2), null);
		}

		protected JSONObject createInvokeMessage(final String objectTag, final String methodName, final Object[] args) throws MarshallException, JSONException {
			JSONObject message;
			String methodTag = objectTag == null ? "" : objectTag + ".";
			methodTag += methodName;

			if (args != null) {
				final SerializerState state = serializer.createSerializerState();
				final Object params = serializer.marshall(state, /* parent */
						null, args, JSONSerializer.PARAMETER_FIELD);

				message = state.createObject(JSONSerializer.PARAMETER_FIELD, params);
			} else {
				message = new JSONObject();
				message.put(JSONSerializer.PARAMETER_FIELD, new JSONArray());
			}

			message.put(JSONSerializer.METHOD_FIELD, methodTag);
			message.put(JSONSerializer.ID_FIELD, getId());

			return message;
		}

		protected Object convertResponseMessage(final JSONObject responseMessage, final Class<?> returnType) throws Exception {
			if (!responseMessage.has(JSONSerializer.RESULT_FIELD)) {
				processException(responseMessage);
			}

			final Object rawResult = serializer.getRequestParser().unmarshall(responseMessage, JSONSerializer.RESULT_FIELD);

			if (returnType.equals(Void.TYPE)) {
				return null;
			} else if (rawResult == null) {
				processException(responseMessage);
			}

			final SerializerState state = serializer.createSerializerState();
			final Object toReturn = serializer.unmarshall(state, returnType, rawResult);

			return toReturn;
		}

		/**
		 * @param futureResult the futureResult to set
		 */
		private synchronized void setFutureResult(final Future<Object> futureResult) {
			// Synchronize setting the futureResult so that calling a method and
			// getting the futureResult in one synchronized block returns the
			// futureResult for that call. Other calling threads not holding the
			// monitor will have to wait
			this.futureResult = futureResult;
		}

		/**
		 * @param resultCallback the resultCallback to set
		 */
		private synchronized void setResultCallback(final AsyncResultCallback<Object, Object, Method> resultCallback) {
			// Synchronize setting the resultCallback so that calling a method and
			// setting the resultCallback in one synchronized block set the
			// resultCallback for that call. Other calling threads not holding the
			// monitor will have to wait
			this.resultCallback = resultCallback;
		}

		private static class ExceptionSettableFuture<T> extends SettableFuture<T> {
			private ExecutionException exception;

			@Override
			public synchronized T get() throws InterruptedException, ExecutionException {
				if (exception != null) {
					throw exception;
				}

				return super.get();
			}

			@Override
			public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
				if (exception != null) {
					throw exception;
				}

				return super.get(timeout, unit);
			}



			/**
			 * @param exception the exception to set
			 */
			public void setException(final ExecutionException exception) {
				this.exception = exception;
			}

		}
	}