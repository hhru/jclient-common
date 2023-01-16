package ru.hh.jclient.errors.impl;

import java.util.function.BiFunction;

public abstract class ExceptionBuilder<E extends RuntimeException, T extends ExceptionBuilder<E, T>> {
  protected StringBuilder message;
  protected Integer status;
  protected Integer originalStatus;
  protected BiFunction<String, Integer, Object> entityCreator;
  protected Throwable cause;

  public T appendToMessage(String addition) {
    if (message == null) {
      message = new StringBuilder();
      message.append(addition);
    } else {
      message.append(" ");
      message.append(addition);
    }
    return getSelf();
  }

  public String getMessage() {
    return message.toString();
  }

  public T setStatus(Integer status) {
    this.status = status;
    return getSelf();
  }

  public T setOriginalStatus(Integer originalStatus) {
    this.originalStatus = originalStatus;
    return getSelf();
  }

  /**
   * Set function that is able to produce entity for WAE's response. Function accepts message and status code and produces any object that can be
   * serialized to framework's format of choice.
   */
  public T setEntityCreator(BiFunction<String, Integer, Object> entityCreator) {
    this.entityCreator = entityCreator;
    return getSelf();
  }

  public T setCause(Throwable cause) {
    this.cause = cause;
    return getSelf();
  }

  public abstract E toException();

  @SuppressWarnings("unchecked")
  protected T getSelf() {
    return (T) this;
  }
}
