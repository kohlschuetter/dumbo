/**
 *
 */
package org.jabsorb.client.async;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.jabsorb.client.Session;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author matthijs
 *
 */
public class AsyncSessionUtil {
	private static final Logger log = LoggerFactory.getLogger(AsyncSessionUtil.class);

	public static Session toSyncSession(final AsyncSession asyncSession) {
		// unwrap if possible
		if (asyncSession instanceof AsyncedSyncSession) {
			return ((AsyncedSyncSession)asyncSession).getSession();
		}

		return new SyncedAsyncSession(asyncSession);
	}

	public static AsyncSession toAsyncSession(final Session session) {
		// unwrap if possible
		if (session instanceof SyncedAsyncSession) {
			return ((SyncedAsyncSession)session).getAsyncSession();
		}

		return new AsyncedSyncSession(session);
	}

	private static class SyncedAsyncSession implements Session {
		private final AsyncSession asyncSession;

		public SyncedAsyncSession(final AsyncSession asyncSession) {
			this.asyncSession = asyncSession;
		}

		/**
		 * @return the asyncSession
		 */
		public AsyncSession getAsyncSession() {
			return asyncSession;
		}

		public JSONObject sendAndReceive(final JSONObject message) {
			final Future<JSONObject> result = asyncSession.send(message);

			JSONObject response = null;
			try {
				response = result.get();
			} catch (final InterruptedException e) {
				log.error("sendAndReceive was interrupted", e);
			} catch (final ExecutionException e) {
				log.error("sendAndReceive could not properly execute", e);
			}

			return response;
		}

		public void close() {
			asyncSession.close();
		}
	}

	private static class AsyncedSyncSession implements AsyncSession {
		private final Session session;

		public AsyncedSyncSession(final Session session) {
			this.session = session;
		}

		/**
		 * @return the session
		 */
		public Session getSession() {
			return session;
		}

		public Future<JSONObject> send(final JSONObject request) {
			return send(request, null);
		}

		public Future<JSONObject> send(final JSONObject request, final AsyncResultCallback<AsyncSession, JSONObject, JSONObject> callback) {
			final SettableFuture<JSONObject> result = new SettableFuture<JSONObject>();

			new Thread() {
				@Override
				public void run() {
					final JSONObject response = session.sendAndReceive(request);

					result.set(response);

					if (callback != null) {
						try {
							callback.onAsyncResult(AsyncedSyncSession.this, result, request);
						} catch (final Exception e) {
							throw new RuntimeException(e);
						}
					}
				}
			}.start();

			return result;
		}

		public void close() {
			session.close();
		}
	}
}
