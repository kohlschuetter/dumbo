/**
 *
 */
package org.jabsorb.client.async;

import java.util.concurrent.Future;

/**
 * This interface specifies a method that is called when the asynchronous
 * operation is completed.
 *
 * @author matthijs
 *
 */
public interface AsyncResultCallback<S,R,C> {
	/**
	 * Method to be called when the asynchronous operation is completed
	 * @param source The source of the callback, usually the object that contains the asynchronous operation
	 * @param result The result of the operation
	 * @param context Optional context data
	 */
	public void onAsyncResult(S source, Future<R> result, C context);
}
