package ru.hh.jclient.errors.jersey;

import jakarta.ws.rs.SeBootstrap;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.Variant.VariantListBuilder;
import jakarta.ws.rs.ext.RuntimeDelegate;
import java.util.concurrent.CompletionStage;

@SuppressWarnings("unused")
public final class RuntimeDelegateStub extends RuntimeDelegate {

  @Override
  public UriBuilder createUriBuilder() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder createResponseBuilder() {
    return new ResponseBuilderStub();
  }

  @Override
  public VariantListBuilder createVariantListBuilder() {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T createEndpoint(Application application, Class<T> endpointType) throws IllegalArgumentException, UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> HeaderDelegate<T> createHeaderDelegate(Class<T> type) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Link.Builder createLinkBuilder() {
    throw new UnsupportedOperationException();
  }

  @Override
  public SeBootstrap.Configuration.Builder createConfigurationBuilder() {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompletionStage<SeBootstrap.Instance> bootstrap(Application application, SeBootstrap.Configuration configuration) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompletionStage<SeBootstrap.Instance> bootstrap(Class<? extends Application> aClass, SeBootstrap.Configuration configuration) {
    throw new UnsupportedOperationException();
  }

  @Override
  public EntityPart.Builder createEntityPartBuilder(String s) throws IllegalArgumentException {
    throw new UnsupportedOperationException();
  }
}
