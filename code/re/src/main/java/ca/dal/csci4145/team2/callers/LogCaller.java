package ca.dal.csci4145.team2.callers;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogCaller
{
	private static final Logger log = LoggerFactory.getLogger(LogCaller.class);

	private final Client client = ClientBuilder.newClient();
	private final WebTarget startTarget = client
		.target("https://mbr.danielstout.ca/api/logs/start");
	private final WebTarget endTarget = client.target("https://mbr.danielstout.ca/api/logs/end");

	public static class LogStart
	{
		public String message;
		public Object params;
		public String source;
	}

	public void logStart(String message, Object params)
	{
		LogStart start = new LogStart();
		start.message = message;
		start.params = params;
		start.source = "RE";

		startTarget.request().async().post(Entity.entity(start, MediaType.APPLICATION_JSON),
			new InvocationCallback<Response>()
			{

				@Override
				public void completed(Response response)
				{
					log.debug("Log end message sent. Response code: {}", response.getStatus());
				}

				@Override
				public void failed(Throwable throwable)
				{
					log.warn("Log end message failed.", throwable);
				}
			});
	}

	public static class LogEnd
	{
		public String message;
		public String source;
	}

	public void logEnd(String message)
	{
		LogEnd end = new LogEnd();
		end.message = message;
		end.source = "RE";

		endTarget.request().async().post(Entity.entity(end, MediaType.APPLICATION_JSON),
			new InvocationCallback<Response>()
			{
				@Override
				public void completed(Response response)
				{
					log.debug("Log end message sent. Response code: {}", response.getStatus());
				}

				@Override
				public void failed(Throwable throwable)
				{
					log.warn("Log end message failed.", throwable);
				}
			});
	}
}
