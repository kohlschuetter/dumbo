package org.jabsorb.serializer.response.flat;

import java.util.HashMap;
import java.util.Map;

import org.jabsorb.serializer.MarshallException;
import org.jabsorb.serializer.ProcessedObject;
import org.jabsorb.serializer.SerializerState;
import org.jabsorb.serializer.UnmarshallException;
import org.jabsorb.serializer.response.results.SuccessfulResult;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Serializes objects into a flat style. This means that all JSONObjects exist
 * at the top level. This prevents duplication and circular references from
 * occuring.
 * 
 * @author William Becker
 */
public class FlatSerializerState implements SerializerState
{
  /**
   * The start indentifier of an index
   */
  public static final String INDEX_PREFIX = "_$Inx$_";

  /**
   * Adds the values contained in map, to the object o.
   * 
   * @param o The object to which the values are added
   * @param result The primary value
   * @param key The key under which the primary value is to be stored
   * @param map The values to store in the object
   * @return The object o, which was passed in.
   * @throws JSONException If an error happens when the values in map are added
   *           to the object
   */
  public static JSONObject addValuesToObject(JSONObject o, final Object result,
      final String key, final Map<Integer, FlatProcessedObject> map)
      throws JSONException
  {
    o.put(key, result);
    for (FlatProcessedObject p : map.values())
    {
      o.put(p.getIndex().getIndex(), p.getActualSerialized());
    }
    return o;
  }

  /**
   * The current index value
   */
  private int index;

  /**
   * All values which are known to be marshalled to JSONObjects
   */
  private final Map<Integer, FlatProcessedObject> marshalledObjects;

  /**
   * All values which either aren't to be marshalled to JSONObjects or it is
   * unknown to what they will be unmarshalled
   */
  private final Map<Integer, FlatProcessedObject> nonMarshalledObjects;

  /**
   * Creates a new FlatSerializerState
   */
  public FlatSerializerState()
  {
    this.marshalledObjects = new HashMap<Integer, FlatProcessedObject>();
    this.nonMarshalledObjects = new HashMap<Integer, FlatProcessedObject>();
    this.index = 1;
  }

  public Object checkObject(Object parent, Object currentObject, Object ref)
      throws MarshallException
  {
    FlatProcessedObject o = getProcessedObject(currentObject);
    if (o != null)
    {
      return o.getSerialized();
    }
    push(null, currentObject, null);
    return null;
  }

  public JSONObject createObject(String key, Object json) throws JSONException
  {
    final JSONObject toReturn = new JSONObject();
    if (json != null)
    {
      FlatSerializerState.addValuesToObject(toReturn, json, key,
          this.marshalledObjects);
    }
    return toReturn;
  }

  public SuccessfulResult createResult(Object requestId, Object json)
  {
    return new FlatResult(requestId, json, this.marshalledObjects);
  }

  /**
   * Get the JSONObject for the given marshalled object
   * 
   * @param o The java object
   * @return A JSON object for the java object.
   */
  public JSONObject getJSONObject(Object o)
  {
    JSONObject j = (JSONObject) marshalledObjects.get(
        new Integer(System.identityHashCode(o))).getObject();
    return j;
  }

  public FlatProcessedObject getProcessedObject(Object object)
  {
    final int key = System.identityHashCode(object);
    if (this.marshalledObjects.containsKey(key))
    {
      return this.marshalledObjects.get(key);
    }
    return this.nonMarshalledObjects.get(key);
  }

  public void pop() throws MarshallException
  {
    //Nothing to do
  }

  public Object push(Object parent, Object obj, Object ref)
  {
    final int identity = new Integer(System.identityHashCode(obj));
    final Object toReturn;
    final FlatProcessedObject po;
    if (obj instanceof JSONObject)
    {
      JSONObject val = new JSONObject();
      String _index = nextIndex();
      po = new FlatProcessedObject(val, _index);

      toReturn = _index;
      if (!this.marshalledObjects.containsKey(identity))
      {
        this.marshalledObjects.put(identity, po);
      }
    }
    else
    {
      po = new FlatProcessedObject(obj);
      toReturn = obj;
      if (!this.nonMarshalledObjects.containsKey(identity))
      {
        this.nonMarshalledObjects.put(identity, po);
      }
    }
    return toReturn;
    //throw new MarshallException("Object already marshalled.");
  }

  public void setMarshalled(Object marshalledObject, Object java)
  {
    if (marshalledObject instanceof JSONObject)
    {
      FlatProcessedObject o = getProcessedObject(java);
      o.setIndexValue(nextIndex());
      final int key = System.identityHashCode(java);
      if (this.nonMarshalledObjects.containsKey(key))
      {
        this.marshalledObjects.put(key, this.nonMarshalledObjects.remove(key));
      }
    }
  }

  public void setSerialized(Object source, Object target)
      throws UnmarshallException
  {
    final ProcessedObject po = this.getProcessedObject(source);
    if (po != null)
    {
      po.setSerialized(target);
    }
  }

  public void store(Object object)
  {
    if (object instanceof JSONObject)
    {
      FlatProcessedObject p = new FlatProcessedObject(object, nextIndex());
      final int identity = System.identityHashCode(object);
      if (!marshalledObjects.containsKey(identity))
      {
        marshalledObjects.put(identity, p);
      }
    }
  }

  /**
   * Creates the next index value
   * 
   * @return The next index value
   */
  private String nextIndex()
  {
    return FlatSerializerState.INDEX_PREFIX + index++;
  }
}
