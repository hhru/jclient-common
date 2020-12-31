package ru.hh.jclient.common.util;

import java.util.Objects;

public class SimpleRange {

  private int from;
  private int to;

  public SimpleRange(int from, int to) {
    this.from = from;
    this.to = to;
  }

  public int getLowerEndpoint() {
    return from;
  }

  public int getUpperEndpoint() {
    return to;
  }

  public boolean contains(int value) {
    return from <= value && value <= to;
  }

  public boolean isConnected(SimpleRange simpleRange) {
    return from <= simpleRange.to && simpleRange.from <= to;
  }

  public static SimpleRange greaterThan(int value) {
    return new SimpleRange(value + 1, Integer.MAX_VALUE);
  }

  public static SimpleRange singleton(int value) {
    return new SimpleRange(value, value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SimpleRange that = (SimpleRange) o;
    return from == that.from && to == that.to;
  }

  @Override
  public int hashCode() {
    return Objects.hash(from, to);
  }
}
