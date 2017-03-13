package ca.dal.csci4145.team2;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionLogger implements ApplicationEventListener, RequestEventListener
{
	private static final Logger log = LoggerFactory.getLogger(ExceptionLogger.class);

	@Override
	public void onEvent(ApplicationEvent event)
	{
		ApplicationEvent.Type type = event.getType();
		log.trace("Application event: {}", type);
	}

	@Override
	public RequestEventListener onRequest(RequestEvent requestEvent)
	{
		return this;
	}

	@Override
	public void onEvent(RequestEvent event)
	{
		if (event.getType() == RequestEvent.Type.ON_EXCEPTION)
		{
			Throwable ex = event.getException();

			if (ex instanceof NotFoundException)
			{
				log.debug("404 Not Found: {}", event.getContainerRequest().getRequestUri());
				return;
			}

			if (ex != null && ex.getCause() != null) ex = ex.getCause();

			String msg = "Request caused an exception";
			if (ex instanceof WebApplicationException)
			{
				log.info(msg, ex);
			}
			else
			{
				log.warn(msg, ex);
			}
		}
	}

}
