package com.kohlschutter.dumbo.jacline.json;

import com.kohlschutter.jacline.annotations.JsIgnoreType;
import com.kohlschutter.jacline.lib.coding.CodingException;
import com.kohlschutter.jacline.lib.coding.CodingServiceProvider;
import com.kohlschutter.jacline.lib.coding.KeyDecoder;
import com.kohlschutter.jacline.lib.coding.KeyEncoder;

@JsIgnoreType
public class CodingServiceProviderOrgJsonImpl implements CodingServiceProvider {

  static final CodingServiceProviderOrgJsonImpl INSTANCE = new CodingServiceProviderOrgJsonImpl();

  private CodingServiceProviderOrgJsonImpl() {
  }

  @Override
  public KeyDecoder keyDecoder(String expectedCodedType, Object encoded) throws CodingException {
    throw new UnsupportedOperationException("FIXME");
    // return new JSONKeyDecoder(this, expectedCodedType, encoded);
  }

  @Override
  public KeyEncoder keyEncoder(String type) {
    return new JSONKeyEncoder(type);
  }
}
