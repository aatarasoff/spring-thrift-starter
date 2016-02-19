package ru.trylogic.spring.boot.thrift.beans;

import com.google.common.base.Strings;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;

import static org.slf4j.MDC.put;
import static org.slf4j.MDC.remove;

/**
 * Created by aleksandr on 18.02.16.
 */
public class RequestIdLogger {

  private static final String DEFAULT_MDC_KEY = "request_id";
  private static final String X_REQUEST_ID = "x-request-id";

  public String getMDCKey() {
    return DEFAULT_MDC_KEY;
  }

  public String getRequestIdHeader() {
    return X_REQUEST_ID;
  }

  public void set(HttpServletRequest request) {
    put(getMDCKey(), getXRequestId(request));
  }

  public void unset() {
    remove(getMDCKey());
  }

  protected String generateXRequestId() {
    return String.valueOf(Instant.now().toEpochMilli());
  }

  protected String getXRequestId(HttpServletRequest request) {
    String requestId = request.getHeader(getRequestIdHeader());
    if (Strings.isNullOrEmpty(requestId)) {
      return generateXRequestId();
    }
    return requestId;
  }
}
