package ru.hh.jclient.errors.impl;

import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;

public class PredicateWithStatus<T> {

  private Predicate<T> predicate;
  @Nullable
  private Integer status;

  public PredicateWithStatus(Predicate<T> predicate) {
    this(predicate, null);
  }

  public PredicateWithStatus(Predicate<T> predicate, Integer status) {
    this.predicate = predicate;
    this.status = status;
  }

  public Predicate<T> getPredicate() {
    return predicate;
  }

  public Optional<Integer> getStatus() {
    return Optional.ofNullable(status);
  }
}
