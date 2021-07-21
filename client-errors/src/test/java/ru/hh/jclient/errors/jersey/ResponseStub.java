package ru.hh.jclient.errors.jersey;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

final class ResponseStub extends Response {

  private final Object entity;
  private final StatusType status;

  ResponseStub(int statusCode, Object entity) {
    this.entity = entity;

    status = new StatusType() {
      @Override
      public int getStatusCode() {
        return statusCode;
      }

      @Override
      public Status.Family getFamily() {
        return null;
      }

      @Override
      public String getReasonPhrase() {
        return null;
      }
    };
  }

  @Override
  public int getStatus() {
    return status.getStatusCode();
  }

  @Override
  public StatusType getStatusInfo() {
    return status;
  }

  @Override
  public Object getEntity() {
    return entity;
  }

  @Override
  public <T> T readEntity(Class<T> entityType) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T readEntity(GenericType<T> entityType) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T readEntity(Class<T> entityType, Annotation[] annotations) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasEntity() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean bufferEntity() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() {
  }

  @Override
  public MediaType getMediaType() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Locale getLanguage() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getLength() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<String> getAllowedMethods() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, NewCookie> getCookies() {
    throw new UnsupportedOperationException();
  }

  @Override
  public EntityTag getEntityTag() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Date getDate() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Date getLastModified() {
    throw new UnsupportedOperationException();
  }

  @Override
  public URI getLocation() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<Link> getLinks() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasLink(String relation) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Link getLink(String relation) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Link.Builder getLinkBuilder(String relation) {
    throw new UnsupportedOperationException();
  }

  @Override
  public MultivaluedMap<String, Object> getMetadata() {
    throw new UnsupportedOperationException();
  }

  @Override
  public MultivaluedMap<String, String> getStringHeaders() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getHeaderString(String name) {
    throw new UnsupportedOperationException();
  }
}
