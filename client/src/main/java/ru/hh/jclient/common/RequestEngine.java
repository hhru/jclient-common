package ru.hh.jclient.common;

import java.util.concurrent.CompletableFuture;

public interface RequestEngine {
  CompletableFuture<? extends Response> execute();
}
