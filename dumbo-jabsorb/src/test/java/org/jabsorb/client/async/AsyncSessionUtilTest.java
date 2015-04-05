/**
 *
 */
package org.jabsorb.client.async;

import java.util.concurrent.Future;

import junit.framework.TestCase;

import org.jabsorb.JSONSerializer;
import org.jabsorb.client.Session;
import org.json.JSONObject;

/**
 * @author matthijs
 *
 */
public class AsyncSessionUtilTest extends TestCase {

	/**
	 * Test method for {@link org.jabsorb.client.async.AsyncSessionUtil#ToSyncSession(org.jabsorb.client.async.AsyncSession)}.
	 */
	public void testToSyncSession() throws Exception {
		final AsyncSession asyncSession = new DummyAsyncSession(1000, "testToSyncSession");

		final Session session = AsyncSessionUtil.toSyncSession(asyncSession);

		long time = System.currentTimeMillis();
		final JSONObject response = session.sendAndReceive(null);

		time = System.currentTimeMillis() - time;

		assertEquals("testToSyncSession", response.get(JSONSerializer.RESULT_FIELD));
		assertTrue("Should take at least 1000 ms", time >= 1000);
	}

	/**
	 * Test method for {@link org.jabsorb.client.async.AsyncSessionUtil#ToAsyncSession(org.jabsorb.client.Session)}.
	 * @throws Exception
	 */
	public void testToAsyncSession() throws Exception {
		final Session session = new DummySession(1000, "testToAsyncSession");

		final AsyncSession asyncSession = AsyncSessionUtil.toAsyncSession(session);

		long time = System.currentTimeMillis();
		final Future<JSONObject> future = asyncSession.send(null);

		final long returnTime = System.currentTimeMillis() - time;
		assertTrue("Should return almost immediately", returnTime <= 100); // XXX: might be dangerous to assume?
		assertEquals("testToAsyncSession", future.get().get(JSONSerializer.RESULT_FIELD));

		time = System.currentTimeMillis() - time;
		assertTrue("Should take at least 1000 ms", time >= 1000);
	}

	/**
	 * Test method for {@link org.jabsorb.client.async.AsyncSessionUtil#ToAsyncSession(org.jabsorb.client.Session)}.
	 * @throws Exception
	 */
	public void testToAsyncSessionCallback() throws Exception {
		final Session session = new DummySession(1000, "testToAsyncSessionCallback");

		final AsyncSession asyncSession = AsyncSessionUtil.toAsyncSession(session);

		final long time = System.currentTimeMillis();

		final boolean[] callbackCalled = new boolean[] { false };

		final Future<JSONObject> future = asyncSession.send(null, new AsyncResultCallback<AsyncSession, JSONObject, JSONObject>() {
			public void onAsyncResult(final AsyncSession source, final Future<JSONObject> result, final JSONObject context) {
				try {
					assertEquals("testToAsyncSessionCallback", result.get().get(JSONSerializer.RESULT_FIELD));
				} catch (final Exception e) {
					throw new RuntimeException(e);
				}

				final long resultTime = System.currentTimeMillis() - time;
				assertTrue("Should take at least 1000 ms", resultTime >= 1000);

				callbackCalled[0] = true;
			}
		});

		final long returnTime = System.currentTimeMillis() - time;
		assertTrue("Should return almost immediately", returnTime <= 100); // XXX: might be dangerous to assume?

		future.get(); Thread.sleep(100); // otherwise the method would be done even before the callback is called
		assertTrue("Callback should have been called", callbackCalled[0]);
	}

	public void testUnwrapSession() throws Exception {
		final Session session = new DummySession(1000, "testUnwrapSession");
		assertSame(session, AsyncSessionUtil.toSyncSession(AsyncSessionUtil.toAsyncSession(session)));
	}

	public void testUnwrapAsyncSession() throws Exception {
		final AsyncSession session = new DummyAsyncSession(1000, "testUnwrapSession");
		assertSame(session, AsyncSessionUtil.toAsyncSession(AsyncSessionUtil.toSyncSession(session)));
	}
}
