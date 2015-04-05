package org.jabsorb.serializer.request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple request parser that just returns the argument array without doing
 * anything
 */
public class DefaultRequestParser extends RequestParser
{
  @Override
  public JSONArray unmarshallArray(final JSONObject jsonReq, final String key)
      throws JSONException
  {
    return jsonReq.getJSONArray(key);
  }

  @Override
  public JSONObject unmarshallObject(JSONObject jsonReq, String key)
      throws JSONException
  {
    return jsonReq.getJSONObject(key);
  }
}
