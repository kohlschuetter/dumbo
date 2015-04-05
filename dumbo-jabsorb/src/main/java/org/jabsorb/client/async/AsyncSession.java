/**
 *
 */
package org.jabsorb.client.async;

import java.util.concurrent.Future;

import org.json.JSONObject;

/**
 * @author matthijs
 *
 */
public interface AsyncSession {
	public Future<JSONObject> send(JSONObject request);

	public Future<JSONObject> send(JSONObject request, AsyncResultCallback<AsyncSession, JSONObject, JSONObject> callback);

	public void close();
}
