package ru.hh.jclient.common.util;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.MDC;

public class MDCCopy {
  public static final MDCCopy EMPTY = new MDCCopy(Collections.emptyMap());

  protected Map<String, String> contextCopy;

  MDCCopy(Map<String, String> contextCopy) {
    this.contextCopy = contextCopy;
  }

  public static MDCCopy capture() {
    Map<String, String> copyOfContextMap = MDC.getCopyOfContextMap();
    return copyOfContextMap == null || copyOfContextMap.isEmpty() ? EMPTY : new MDCCopy(copyOfContextMap);
  }

  public void restore() {
    MDC.setContextMap(this.contextCopy);
  }

  public MDCCopy replace() {
    MDCCopy contextBefore = capture();
    restore();
    return contextBefore;
  }

  public void doInContext(Runnable r) {
    MDCCopy contextBefore = replace();
    try {
      r.run();
    } finally {
      contextBefore.restore();
    }
  }

  public <T> T doInContext(Supplier<T> s) {
    MDCCopy contextBefore = replace();
    try {
      return s.get();
    } finally {
      contextBefore.restore();
    }
  }

  public static void doWithoutContext(Runnable r) {
    EMPTY.doInContext(r);
  }

  public static <T> T doWithoutContext(Supplier<T> s) {
    return EMPTY.doInContext(s);
  }
}
