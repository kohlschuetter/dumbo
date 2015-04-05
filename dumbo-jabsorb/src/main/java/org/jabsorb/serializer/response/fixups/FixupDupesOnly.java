package org.jabsorb.serializer.response.fixups;

import java.util.List;

import org.jabsorb.serializer.MarshallException;

/**
 * Use this class to make fixups for all duplicates (primitive and
 * non-primitive).
 * 
 * @author William Becker
 */
public class FixupDupesOnly extends UsingFixups
{
  @Override
  public Object circularReferenceFound(List<Object> originalLocation, Object ref,Object java)
      throws MarshallException
  {
    return null;
  }

  @Override
  public Object duplicateFound(List<Object> originalLocation, Object ref,Object java)
      throws MarshallException
  {
    return this.addFixUp(originalLocation, ref);
  }

}
