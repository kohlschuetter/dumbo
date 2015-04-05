/**
 *
 */
package org.jabsorb.client.async;

import java.util.concurrent.Future;

import org.jabsorb.serializer.response.results.SuccessfulResult;
import org.json.JSONException;
import org.json.JSONObject;

class DummyAsyncSession implements AsyncSession {
	private final int duration;
	private final String response;

	public DummyAsyncSession(final int duration, final String response) {
		this.duration = duration;
		this.response = response;
	}

	public void close() {
		// nothing
	}

	public Future<JSONObject> send(final JSONObject request, final AsyncResultCallback<AsyncSession, JSONObject, JSONObject> callback) {
		final SettableFuture<JSONObject> future = new SettableFuture<JSONObject>();

		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(duration);
				} catch (final InterruptedException e) {
					throw new RuntimeException(e);
				}

				try {
					final JSONObject response = new SuccessfulResult("1", DummyAsyncSession.this.response).createOutput();

					future.set(response);
					if (callback != null) {
						callback.onAsyncResult(DummyAsyncSession.this, future, request);
					}
				} catch (final JSONException e) {
					throw new RuntimeException(e);
				}
			}
		}.start();

		return future;
	}

	public Future<JSONObject> send(final JSONObject request) {
		return send(request, null);
	}
}