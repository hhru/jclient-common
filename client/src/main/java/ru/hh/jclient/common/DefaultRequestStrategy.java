package ru.hh.jclient.common;

import static java.util.Optional.ofNullable;
import java.util.function.UnaryOperator;

public class DefaultRequestStrategy implements RequestStrategy<DefaultEngineBuilder> {
  private final UnaryOperator<DefaultEngineBuilder> configAction;

  private DefaultRequestStrategy(UnaryOperator<DefaultEngineBuilder> configAction) {
    this.configAction = configAction;
  }

  public DefaultRequestStrategy() {
    this(null);
  }

  @Override
  public DefaultEngineBuilder createRequestEngineBuilder(HttpClient client) {
    return ofNullable(configAction).map(action -> action.apply(new DefaultEngineBuilder(client))).orElseGet(() -> new DefaultEngineBuilder(client));
  }

  @Override
  public RequestStrategy<DefaultEngineBuilder> createCustomizedCopy(UnaryOperator<DefaultEngineBuilder> configAction) {
    return new DefaultRequestStrategy(configAction);
  }
}
