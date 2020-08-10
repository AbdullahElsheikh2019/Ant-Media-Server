package io.antmedia.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import io.antmedia.statistic.HlsViewerStats;
import io.antmedia.statistic.IStreamStats;

public class HlsStatisticsFilter extends AbstractFilter {

	protected static Logger logger = LoggerFactory.getLogger(HlsStatisticsFilter.class);
	private IStreamStats streamStats;



	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {


		HttpServletRequest httpRequest =(HttpServletRequest)request;

		String method = httpRequest.getMethod();
		if ("GET".equals(method)) {
			//only accept GET methods
			String sessionId = httpRequest.getSession().getId();

		
			chain.doFilter(request, response);

			int status = ((HttpServletResponse) response).getStatus();
			
			if (HttpServletResponse.SC_OK <= status && status <= HttpServletResponse.SC_BAD_REQUEST) 
			{
				String streamId = TokenFilterManager.getStreamId(httpRequest.getRequestURI());
				
				if (streamId != null) {
					logger.debug("req ip {} session id {} stream id {} status {}", request.getRemoteHost(), sessionId, streamId, status);
					IStreamStats stats = getStreamStats();
					if (stats != null) {
						stats.registerNewViewer(streamId, sessionId);
					}
				}
			}
		}
		else {
			chain.doFilter(httpRequest, response);
		}

	}

	public IStreamStats getStreamStats() {
		if (streamStats == null) {
			ApplicationContext context = getAppContext();
			if (context != null) 
			{
				streamStats = (IStreamStats)context.getBean(HlsViewerStats.BEAN_NAME);
			}
		}
		return streamStats;
	}

}
