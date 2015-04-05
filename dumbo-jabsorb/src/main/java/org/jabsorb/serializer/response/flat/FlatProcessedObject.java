package org.jabsorb.serializer.response.flat;

import org.jabsorb.serializer.ProcessedObject;
import org.json.JSONObject;

/**
 * Contains data for each flatten object
 * 
 * @author William Becker
 */
class FlatProcessedObject extends ProcessedObject
{
  /**
   * The index that will be assigned to this object
   */
  private Index index;

  /**
   * Creates a new FlatProcessedObject
   * 
   * @param object The object that is being processed
   * @param index The unique identifier for the object
   */
  public FlatProcessedObject(Object object, String index)
  {
    super(object);
    this.index = new Index(index);
  }

  /**
   * Creates a new FlatProcessedObject. The index must be set later.
   * 
   * @param object The object that is being processed
   */
  public FlatProcessedObject(Object object)
  {
    super(object);
    this.index = new Index();
  }

  /**
   * Sets the index
   * 
   * @param index The value the index should take
   */
  public void setIndexValue(String index)
  {
    this.index.setIndex(index);
  }

  /**
   * Gets the index for the object. Note that it may not be set yet.
   * 
   * @return The index for the object
   */
  public Index getIndex()
  {
    return index;
  }

  @Override
  public Object getSerialized()
  {
    final Object o = getActualSerialized();
    if ((o == null) || (o instanceof JSONObject))
    {
      return getIndex();
    }
    return o;
  }

  /**
   * Since getSerialized() gets the index if necessary, this is used to get the
   * real serialized value when it is directly needed.
   * 
   * @return The actual serialized value.
   */
  public Object getActualSerialized()
  {
    return super.getSerialized();
  }
}
