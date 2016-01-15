package ru.trylogic.spring.boot.thrift.filters;

import com.google.common.base.Strings;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Instant;

import static org.slf4j.MDC.put;
import static org.slf4j.MDC.remove;

public class RequestIdFilter implements Filter {

	private static final String DEFAULT_MDC_KEY = "request_id";
	private static final String X_REQUEST_ID = "x-request-id";

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException,
			ServletException {
		String key = getMDCKey();
		put(key, getXRequestId((HttpServletRequest) request));
		chain.doFilter(request, response);
		remove(key);
	}

	protected String getMDCKey() {
		return DEFAULT_MDC_KEY;
	}

	protected String getXRequestId(HttpServletRequest request) {
		String requestId = request.getHeader(X_REQUEST_ID);
		if (Strings.isNullOrEmpty(requestId)) {
			return generateXRequestId();
		}
		return requestId;
	}

	protected String generateXRequestId() {
		return String.valueOf(Instant.now().toEpochMilli());
	}

}