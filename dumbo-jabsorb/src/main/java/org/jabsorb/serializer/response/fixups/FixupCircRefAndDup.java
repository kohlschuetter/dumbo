package org.jabsorb.serializer.response.fixups;

import java.util.List;

import org.jabsorb.serializer.MarshallException;

/**
 * Use this class to make fixups for circular references and all duplicates (primitive and
 * non-primitive).
 * 
 * @author William Becker
 */
public class FixupCircRefAndDup extends FixupCircRefOnly
{
  @Override
  public Object duplicateFound(List<Object> originalLocation, Object ref, Object java)
      throws MarshallException
  {
    return this.addFixUp(originalLocation, ref);
  }
}
