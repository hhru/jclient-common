package ru.hh.jclient.common.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.io.IOException;
import java.io.ObjectStreamClass;
import java.util.concurrent.ExecutionException;

public class ClassesCache {
  private static final LoadingCache<String, Class<?>> CLASS_CACHE = CacheBuilder
          .newBuilder()
          .concurrencyLevel(Runtime.getRuntime().availableProcessors())
          .build(new CacheLoader<String, Class<?>>() {
            @Override
            public Class<?> load(String input) throws RuntimeException {
              try {
                return Class.forName(input);
              } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
              }
            }
          });

  public static Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
    try {
      return CLASS_CACHE.get(desc.getName());
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }
}
