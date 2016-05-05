package ru.trylogic.spring.boot.thrift.filters;

import org.springframework.beans.factory.annotation.Autowired;
import ru.trylogic.spring.boot.thrift.beans.RequestIdLogger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class RequestIdFilter implements Filter {

	@Autowired
	private RequestIdLogger requestIdLogger;

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
		requestIdLogger.set((HttpServletRequest) request);
		chain.doFilter(request, response);
		requestIdLogger.unset();
	}

}