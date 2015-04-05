package org.jabsorb.serializer.response.flat;

import java.util.Map;

import org.jabsorb.JSONSerializer;
import org.jabsorb.serializer.response.results.SuccessfulResult;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Puts all the objects and indexes into a results
 * 
 * @author William Becker
 */
class FlatResult extends SuccessfulResult
{
  /**
   * Maps the hash codes of objects to results
   */
  private final Map<Integer, FlatProcessedObject> map;

  /**
   * Creates a new FlatResult
   * 
   * @param id The id of the message
   * @param jsonResult The main message to send
   * @param map Contains the other objects to put in the result
   */
  public FlatResult(Object id, Object jsonResult,
      Map<Integer, FlatProcessedObject> map)
  {
    super(id, jsonResult);
    this.map = map;
  }

  @Override
  public JSONObject createOutput() throws JSONException
  {
    JSONObject o = this._createOutput();
    Object result = getResult();
    if (result != null)
    {
      FlatSerializerState.addValuesToObject(o, result, JSONSerializer.RESULT_FIELD, 
          this.map);
    }
    return o;
  }
}
