/**
 *
 */
package org.jabsorb.client.async;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <p>Simple implementation of Future<T>. When the result of the computation is
 * set on this object all get operations are notified and return the result. Cancellation is not supported, {@link #cancel(boolean)} always returns false</p>
 * <p>Example:
 * <pre>
 * final SettableFuture<String> future = new SettableFuture<String>();
 *
 * new Thread() {
 * 	&#x40;Override
 * 	public void run() {
 * 		Thread.sleep(3000); // do a computation
 * 		future.set("result");
 * 	}
 * }.start();
 *
 * System.out.println(future.get()); // blocks for three seconds
 * </pre>
 * @author matthijs
 *
 */
public class SettableFuture<T> implements Future<T> {

	private T result;
	private boolean done;

	/**
	 * Set the result of the completed operation
	 * @param result the result
	 */
	public synchronized void set(final T result) {
		this.result = result;
		done = true;
		notifyAll();
	}

	/**
	 * Always returns false, since cancellation is not supported by this implementation
	 */
	public boolean cancel(final boolean mayInterruptIfRunning) {
		return false;
	}

	public boolean isCancelled() {
		return false;
	}

	public boolean isDone() {
		return done;
	}

	public synchronized T get() throws InterruptedException, ExecutionException {
		while (!isDone()) {
			// release monitor (synchronisation lock) and wait for notification
			wait();
		}

		return result;
	}

	public synchronized T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		long nanos = unit.toNanos(timeout);

		final long deadline = System.nanoTime() + nanos;
		while (!isDone() && nanos > 0) {
			TimeUnit.NANOSECONDS.timedWait(this, nanos);

			nanos = deadline - System.nanoTime();
		}

		if (isDone()) {
			return result;
		}

		throw new TimeoutException("Could not get result within specified time");
	}
}
