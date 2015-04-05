package org.jabsorb.serializer.response.fixups;

import java.util.List;

import org.jabsorb.serializer.MarshallException;

/**
 * Use this class to make fixups for non-primitive duplicates.
 * 
 * @author William Becker
 */
public class FixupNonPrimitiveDupesOnly extends FixupDupesOnly
{
  @Override
  public Object duplicateFound(List<Object> originalLocation, Object ref, Object java)
      throws MarshallException
  {
    if (!isPrimitive(java))
    {
      return super.duplicateFound(originalLocation, ref,java);
    }
    return null;
  }  
}
