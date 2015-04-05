/**
 *
 */
package org.jabsorb.client.async;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.TestCase;

/**
 * @author matthijs
 *
 */
public class SettableFutureTest extends TestCase {

	/**
	 * Test method for {@link org.jabsorb.client.async.SettableFuture#get()}.
	 * @throws Exception
	 */
	public void testGet() throws Exception {
		final SettableFuture<String> future = new SettableFuture<String>();
		assertFalse("future should not be already done", future.isDone());

		runThread(future, "result", 1000);

		long time = System.currentTimeMillis();
		assertEquals("result", future.get());

		time = System.currentTimeMillis() - time;
		assertTrue("At least a second should have passed", time >= 1000);
	}

	/**
	 * Test method for {@link org.jabsorb.client.async.SettableFuture#get(long, java.util.concurrent.TimeUnit)}.
	 */
	public void testGetLongTimeUnit() throws Exception {
		final SettableFuture<String> future = new SettableFuture<String>();
		assertFalse("future should not be already done", future.isDone());

		runThread(future, "result", 2000);

		long time = System.currentTimeMillis();

		try {
			future.get(500, TimeUnit.MILLISECONDS);

			fail("TimeoutException should have been thrown");
		} catch (final TimeoutException e) {
			// yai!
		}

		time = System.currentTimeMillis() - time;
		assertTrue("At least 500 ms should have passed", time >= 500);
		assertTrue("Less than 2 s should have passed", time < 2000);
	}

	private void runThread(final SettableFuture<String> future, final String result, final int sleep) {
		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(1000); // work for a second
				} catch (final InterruptedException e) {
					throw new RuntimeException(e);
				}
				future.set("result");
			}
		}.start();
	}

}
