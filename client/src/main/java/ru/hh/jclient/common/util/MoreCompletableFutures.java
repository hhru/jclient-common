package ru.hh.jclient.common.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class MoreCompletableFutures {
  private MoreCompletableFutures() {
  }

  /**
   * Returns a new CompletableFuture that is completed when one of two things happen (whichever happens first):
   * <ul>
   * <li>
   * all of the given CompletableFutures complete normally -
   * then the returned CompletableFuture is also completed normally
   * </li>
   * <li>
   * any of the given CompletableFutures completes exceptionally -
   * then the returned CompletableFuture is also completed exceptionally
   * with a CompletionException holding this exception as its cause
   * (other CompletableFutures are not automatically cancelled)
   * </li>
   * </ul>
   * <br/>
   * If no CompletableFutures are provided, returns a CompletableFuture completed with the value {@code null}.
   */
  public static CompletableFuture<Void> allOrAnyExceptionallyOf(CompletableFuture<?>... futures) {
    CompletableFuture<Void> all = CompletableFuture.allOf(futures);

    for (var future : futures) {
      future.exceptionally(ex -> {
        if (!(ex instanceof CompletionException)) {
          ex = new CompletionException(ex);
        }

        all.completeExceptionally(ex);
        return null;
      });
    }

    return all;
  }
}
