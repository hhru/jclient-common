package ru.hh.jclient.common.util;

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
}
