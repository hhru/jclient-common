package ru.hh.jclient.common.metrics;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class SimpleJsonBuilderTest {

  private static ObjectMapper objectMapper;

  @BeforeClass
  public static void init() {
    objectMapper = new ObjectMapper()
      .findAndRegisterModules()
      .setSerializationInclusion(JsonInclude.Include.NON_NULL)
      .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, false);
  }

  @Test
  public void toJsonTest() throws JsonProcessingException {
    var currentTime = System.currentTimeMillis();
    SimpleJsonBuilder jsonBuilder = new SimpleJsonBuilder();
    jsonBuilder.put("string", "hh-dict");
    jsonBuilder.put("emptyString", "");
    jsonBuilder.put("null", null);
    jsonBuilder.put("int", currentTime);
    jsonBuilder.put("float", 5.1f);
    var jsonFromBuilder = jsonBuilder.build();

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("string", "hh-dict");
    data.put("emptyString", "");
    data.put("null", null);
    data.put("int", currentTime);
    data.put("float", 5.1f);
    var jsonFromMapper = objectMapper.writeValueAsString(data);

    assertEquals(jsonFromMapper, jsonFromBuilder);
  }
}
