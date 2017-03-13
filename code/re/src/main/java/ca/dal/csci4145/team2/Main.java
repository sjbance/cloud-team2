package ca.dal.csci4145.team2;

import java.io.IOException;
import java.net.URI;

import javax.sql.DataSource;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.ws.rs.core.UriBuilder;

import org.flywaydb.core.Flyway;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Sql2o;
import org.sql2o.quirks.PostgresQuirks;

import ca.dal.csci4145.team2.resources.ReResource;

public class Main
{
	private static final DataSource datasource = getDataSource();
	public static final Sql2o sql2o = new Sql2o(datasource, new PostgresQuirks());

	public static Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	private static final DataSource getDataSource()
	{
		PGSimpleDataSource src = new PGSimpleDataSource();
		src.setUser("csci4145");
		src.setPassword("supersecret");
		src.setPortNumber(5432);
		src.setDatabaseName("re");
		return src;
	}

	private static final Logger log = LoggerFactory.getLogger(Main.class);
	private static final String RESOURCE_PKG = ReResource.class.getPackage().getName();
	public static final URI BASE_URI = UriBuilder.fromUri("http://0.0.0.0").port(8080).path("api")
		.build();

	public static HttpServer startServer()
	{
		ResourceConfig rc = new ResourceConfig()
			.packages(RESOURCE_PKG)
			.register(ExceptionLogger.class)
			.register(JacksonFeature.class)
			.register(MultiPartFeature.class)
			.register(LoggingFilter.class)
			.register(new Binder());

		return GrizzlyHttpServerFactory.createHttpServer(BASE_URI, rc);
	}

	public static void main(String[] args) throws IOException
	{
		Flyway fly = new Flyway();
		fly.setDataSource(datasource);
		fly.migrate();

		HttpServer server = startServer();
		configurePublicContent(server.getServerConfiguration());
		log.debug("Server app started at {}", BASE_URI);
	}

	private static void configurePublicContent(ServerConfiguration conf)
	{
		StaticHttpHandler handler = new StaticHttpHandler("public");
		handler.setFileCacheEnabled(false);
		conf.addHttpHandler(handler, "/");
	}
}
