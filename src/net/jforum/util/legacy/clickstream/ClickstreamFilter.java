
package net.jforum.util.legacy.clickstream;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import net.jforum.util.preferences.ConfigKeys;

import org.apache.log4j.Logger;

public class ClickstreamFilter implements Filter {

	private final static Logger log = Logger.getLogger(ClickstreamFilter.class);

	private final static String FILTER_APPLIED = "_clickstream_filter_applied";

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		// Ensure that filter is only applied once per request.
		if (request.getAttribute(FILTER_APPLIED) == null) {
			request.setAttribute(FILTER_APPLIED, Boolean.TRUE);
			String bot = BotChecker.isBot((HttpServletRequest)request);
			if (bot != null && log.isDebugEnabled()) {
				log.debug("Found a bot: " + bot);
			}
			request.setAttribute(ConfigKeys.IS_BOT, Boolean.valueOf(bot != null));
		}
		// Pass the request on
		chain.doFilter(request, response);
	}

	public void init(FilterConfig filterConfig) throws ServletException {
		
	}

	public void destroy() {
		
	}
}