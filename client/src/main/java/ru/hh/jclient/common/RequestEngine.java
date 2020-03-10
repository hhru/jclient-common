package ru.hh.jclient.common;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface RequestEngine {
  CompletableFuture<Response> execute();
}
