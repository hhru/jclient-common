package ru.hh.jclient.common.model;

import java.util.Objects;

public class JsonTest {

  public Long valueLong;
  public String valueString;

  public JsonTest() {
  }

  /**
   * Use such constructor in case you want to use Json serialization for keys of Map<Object, Object> type,
   * because by default java objects in keys of a Map is serialized to String using toString() method.
   * See {@link com.fasterxml.jackson.databind.ser.std.StdKeySerializer}
   * You can also override StdKeySerializer for more complex logic if you need to.
   */
  public JsonTest(String stringRepresentation) {
    String[] split = stringRepresentation.split("@");
    this.valueLong = Long.parseLong(split[0]);
    this.valueString = split[1];
  }

  public JsonTest(Long valueLong, String valueString) {
    this.valueLong = valueLong;
    this.valueString = valueString;
  }

  @Override
  public String toString() {
    return valueLong + "@" + valueString;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JsonTest that = (JsonTest) o;
    return Objects.equals(valueLong, that.valueLong) &&
      Objects.equals(valueString, that.valueString);
  }

  @Override
  public int hashCode() {
    return Objects.hash(valueLong, valueString);
  }
}
