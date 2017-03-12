package ca.dal.csci4145.team2.resources;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("logs")
public class LogResource
{
	private static final Logger logInvoc = LoggerFactory.getLogger("invocation");

	public static class LogStartData
	{
		public LogStartData()
		{

		}

		public LogStartData(String message, Object params, String source)
		{
			this.message = message;
			this.params = params;
			this.source = source;
		}

		public String message;
		public Object params;
		public String source;
	}

	@POST
	@Path("start")
	public void logStart(LogStartData data)
	{
		logStartInternal(data.source, data.message, data.params);
	}

	public static void logStartInternal(String source, String message, Object params)
	{
		Object rep = params == null ? "{}" : params;
		logInvoc.debug("START {} {} {}", source, message, rep);
	}

	public static class LogEndData
	{
		public LogEndData()
		{

		}

		public LogEndData(String message, String source)
		{
			super();
			this.message = message;
			this.source = source;
		}

		public String message;
		public String source;
	}

	@POST
	@Path("end")
	public void logEnd(LogEndData data)
	{
		logEndInternal(data.source, data.message);
	}

	public static void logEndInternal(String source, String message)
	{
		logInvoc.debug("END {} {}", source, message);
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String getLogs() throws IOException
	{
		return new String(Files.readAllBytes(Paths.get("logs/invocations.log")));
	}
}
