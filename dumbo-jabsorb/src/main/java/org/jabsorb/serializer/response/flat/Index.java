package org.jabsorb.serializer.response.flat;

/**
 * Allows for late allocation of an object's index
 * 
 * @author William Becker
 */
class Index
{
  /**
   * The index of the object
   */
  private String index;

  /**
   * Creates a new index. The value will be set later.
   */
  public Index()
  {
    this(null);
  }

  /**
   * Creates a new index
   * 
   * @param index The index of the object
   */
  public Index(String index)
  {
    this.index = index;
  }

  /**
   * Gets the index
   * 
   * @return The index
   */
  public String getIndex()
  {
    return index;
  }

  /**
   * Sets the index
   * 
   * @param index The value to set
   */
  public void setIndex(String index)
  {
    this.index = index;
  }

  @Override
  public String toString()
  {
    return index;
  }
}
