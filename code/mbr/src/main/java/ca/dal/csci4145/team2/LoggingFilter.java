package ca.dal.csci4145.team2;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingFilter implements ContainerRequestFilter
{
	private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

	@Override
	public void filter(ContainerRequestContext ctx) throws IOException
	{
		String agent = ctx.getHeaderString("User-Agent");
		String url = ctx.getUriInfo().getRequestUri().toString();
		String method = ctx.getMethod();
		log.debug("User '{}' made a {} request for {}", agent, method, url);
	}

}
