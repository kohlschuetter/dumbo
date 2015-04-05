/**
 *
 */
package org.jabsorb.client.async;

import org.jabsorb.client.Session;
import org.jabsorb.serializer.response.results.SuccessfulResult;
import org.json.JSONException;
import org.json.JSONObject;

class DummySession implements Session {
	private final int duration;
	private final String response;


	public DummySession(final int duration, final String response) {
		super();
		this.duration = duration;
		this.response = response;
	}

	public void close() {
		// nothing
	}

	public JSONObject sendAndReceive(final JSONObject message) {
		try {
			Thread.sleep(duration);
			return new SuccessfulResult("1", DummySession.this.response).createOutput();
		} catch (final InterruptedException e) {
			throw new RuntimeException(e);
		} catch (final JSONException e) {
			throw new RuntimeException(e);
		}
	}
}