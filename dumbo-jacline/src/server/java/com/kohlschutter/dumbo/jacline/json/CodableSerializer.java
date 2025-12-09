package com.kohlschutter.dumbo.jacline.json;

import java.util.Collection;
import java.util.Set;

import com.kohlschutter.dumborb.JSONSerializer;
import com.kohlschutter.dumborb.serializer.MarshallException;
import com.kohlschutter.dumborb.serializer.ObjectMatch;
import com.kohlschutter.dumborb.serializer.Serializer;
import com.kohlschutter.dumborb.serializer.SerializerState;
import com.kohlschutter.dumborb.serializer.UnmarshallException;
import com.kohlschutter.jacline.lib.coding.Codable;
import com.kohlschutter.jacline.lib.coding.CodingException;
import com.kohlschutter.jacline.lib.coding.CodingServiceProvider;

public class CodableSerializer implements Serializer {
  private static final Set<Class<?>> EMPTY = Set.of();

  private static final CodingServiceProvider ORG_JSON_CSP =
      CodingServiceProviderOrgJsonImpl.INSTANCE;

  @Override
  public boolean canSerialize(Class<?> clazz, Class<?> jsonClazz) {
    if (clazz != null && Codable.class.isAssignableFrom(clazz)) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public Collection<Class<?>> getJSONClasses() {
    return EMPTY;
  }

  @Override
  public Collection<Class<?>> getSerializableClasses() {
    return EMPTY;
  }

  @Override
  public void setOwner(JSONSerializer ser) {
  }

  @Override
  public Object marshall(SerializerState state, Object p, Object o) throws MarshallException {
    if (!(o instanceof Codable)) {
      throw new MarshallException("Cannot marshall class " + o);
    }

    Codable c = (Codable) o;
    try {
      Object obj = c.encode(ORG_JSON_CSP);
      return obj;
    } catch (CodingException e) {
      throw new MarshallException("Cannot encode Codable", e);
    }
  }

  @Override
  public ObjectMatch tryUnmarshall(SerializerState state, Class<?> clazz, Object json)
      throws UnmarshallException {
    return null; // FIXME
  }

  @Override
  public Object unmarshall(SerializerState state, Class<?> clazz, Object json)
      throws UnmarshallException {
    return null; // FIXME
  }
}
