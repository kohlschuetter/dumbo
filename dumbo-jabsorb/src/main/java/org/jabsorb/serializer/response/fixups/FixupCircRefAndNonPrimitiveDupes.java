package org.jabsorb.serializer.response.fixups;

import java.util.List;

import org.jabsorb.serializer.MarshallException;

/**
 * Use this class to make fixups for circluar references and non-primative
 * duplicates.
 * 
 * @author William Becker
 */
public class FixupCircRefAndNonPrimitiveDupes extends FixupCircRefAndDup
{
  @Override
  public Object duplicateFound(List<Object> originalLocation, Object ref, Object java)
      throws MarshallException
  {
    if (!isPrimitive(java))
    {
      return super.duplicateFound(originalLocation, ref, java);
    }
    return null;
  }
}
