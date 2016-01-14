package ru.trylogic.spring.boot.thrift.filters;

import com.google.common.base.Strings;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Instant;

import static org.slf4j.MDC.put;
import static org.slf4j.MDC.remove;

public class RequestIdFilter implements Filter {

	private static final String MDC_REQUEST_ID = "request_id";
	private static final String X_REQUEST_ID = "x-request-id";

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {

	}

	private String getXRequestId(HttpServletRequest request) {
		String requestId = request.getHeader(X_REQUEST_ID);
		if (Strings.isNullOrEmpty(requestId)) {
			return String.valueOf(Instant.now().toEpochMilli());
		}

		return requestId;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException,
			ServletException {
		put(MDC_REQUEST_ID, getXRequestId((HttpServletRequest) request));
		chain.doFilter(request, response);
		remove(MDC_REQUEST_ID);
	}

	@Override
	public void destroy() {

	}
}