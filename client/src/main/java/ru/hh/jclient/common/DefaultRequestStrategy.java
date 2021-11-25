package ru.hh.jclient.common;

import java.util.function.UnaryOperator;
import static java.util.function.UnaryOperator.identity;

public class DefaultRequestStrategy implements RequestStrategy<DefaultEngineBuilder> {
  private final UnaryOperator<DefaultEngineBuilder> configAction;

  private DefaultRequestStrategy(UnaryOperator<DefaultEngineBuilder> configAction) {
    this.configAction = configAction;
  }

  public DefaultRequestStrategy() {
    this(identity());
  }

  @Override
  public DefaultEngineBuilder createRequestEngineBuilder(HttpClient client) {
    return configAction.apply(new DefaultEngineBuilder(client));
  }

  @Override
  public RequestStrategy<DefaultEngineBuilder> createCustomizedCopy(UnaryOperator<DefaultEngineBuilder> configAction) {
    return new DefaultRequestStrategy(this.configAction.andThen(configAction)::apply);
  }
}
