package ru.hh.jclient.errors.jersey;

import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Variant;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@SuppressWarnings("unused")
final class ResponseBuilderStub extends ResponseBuilder {

  private int status;
  private Object entity;

  @Override
  public Response build() {
    return new ResponseStub(status, entity);
  }

  @Override
  public ResponseBuilder clone() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder status(int status) {
    this.status = status;
    return this;
  }

  @Override
  public ResponseBuilder status(int status, String reasonPhrase) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder entity(Object entity) {
    this.entity = entity;
    return this;
  }

  @Override
  public ResponseBuilder entity(Object entity, Annotation[] annotations) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder allow(String... methods) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder allow(Set<String> methods) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder type(MediaType type) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder type(String type) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder variant(Variant variant) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder variants(List<Variant> variants) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder links(Link... links) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder link(URI uri, String rel) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder link(String uri, String rel) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder language(String language) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder language(Locale language) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder location(URI location) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder contentLocation(URI location) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder tag(EntityTag tag) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder tag(String tag) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder variants(Variant... variants) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder lastModified(Date lastModified) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder cacheControl(CacheControl cacheControl) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder encoding(String encoding) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder expires(Date expires) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder header(String name, Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder replaceAll(MultivaluedMap<String, Object> headers) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder cookie(NewCookie... cookies) {
    throw new UnsupportedOperationException();
  }
}
