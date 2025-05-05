package ru.hh.jclient.common;

public class ExternalRequestBuilder extends RequestBuilder {
  private Integer maxTries;

  public ExternalRequestBuilder() {
    super();
  }

  public ExternalRequestBuilder(String method) {
    super(method);
    setExternalRequest(true);
  }

  public ExternalRequestBuilder(String method, boolean disableUrlEncoding) {
    super(method, disableUrlEncoding);
    setExternalRequest(true);
  }

  public ExternalRequestBuilder(String method, boolean disableUrlEncoding, boolean validateHeaders) {
    super(method, disableUrlEncoding, validateHeaders);
    setExternalRequest(true);
  }

  public ExternalRequestBuilder(Request prototype) {
    super(prototype, false);
    setExternalRequest(true);
  }

  public ExternalRequestBuilder(Request prototype, boolean disableUrlEncoding) {
    super(prototype, disableUrlEncoding);
    setExternalRequest(true);
  }

  public ExternalRequestBuilder(Request prototype, boolean disableUrlEncoding, boolean validateHeaders) {
    super(prototype, disableUrlEncoding, validateHeaders);
    setExternalRequest(true);
  }

  public ExternalRequestBuilder setMaxTries(int maxTries) {
    this.maxTries = maxTries;
    return this;
  }
}
