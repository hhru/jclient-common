package ru.hh.jclient.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import static java.util.Optional.ofNullable;
import javax.annotation.Nullable;

public final class JsonTypeConverter {
  private static final JavaType[] EMPTY_PARAMETERS = {};

  private JsonTypeConverter() {
  }

  @Nullable
  public static <T> JavaType convertClassToJavaType(ObjectMapper objectMapper, Class<T> jsonClass) {
    return ofNullable(objectMapper)
        .flatMap(mapper -> ofNullable(jsonClass).map(clazz -> mapper.getTypeFactory().constructSimpleType(clazz, EMPTY_PARAMETERS)))
        .orElse(null);
  }

  @Nullable
  public static <T> JavaType convertReferenceToJavaType(ObjectMapper objectMapper, TypeReference<T> jsonClass) {
    return ofNullable(objectMapper)
        .flatMap(mapper -> ofNullable(jsonClass).map(clazz -> mapper.getTypeFactory().constructType(clazz)))
        .orElse(null);
  }
}
