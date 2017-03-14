package ca.dal.csci4145.team2.resources;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Path.Node;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hibernate.validator.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

import ca.dal.csci4145.team2.Main;
import ca.dal.csci4145.team2.callers.AuthCaller;
import ca.dal.csci4145.team2.callers.AuthCaller.User;
import ca.dal.csci4145.team2.callers.InsCaller;
import ca.dal.csci4145.team2.callers.InsCaller.InsReq;
import ca.dal.csci4145.team2.callers.LogCaller;
import ca.dal.csci4145.team2.callers.MunCaller;
import ca.dal.csci4145.team2.callers.MunCaller.MunReq;

@Path("re")
@Produces(MediaType.APPLICATION_JSON)
public class ReResource
{
	private static final Random rand = new Random();
	private static final Logger log = LoggerFactory.getLogger(ReResource.class);
	private final LogCaller logCaller;
	private final AuthCaller authCaller;
	private final InsCaller insCaller;
	private final MunCaller munCaller;

	@Inject
	public ReResource(LogCaller logCaller, AuthCaller authCaller, InsCaller insCaller,
		MunCaller munCaller)
	{
		this.logCaller = logCaller;
		this.authCaller = authCaller;
		this.insCaller = insCaller;
		this.munCaller = munCaller;
	}

	@Inject
	ContainerRequestContext request;

	public static class AppraisalReq
	{
		@NotBlank
		public String houseId;

		@NotNull
		public Integer mortId;

		@NotBlank
		public String name;
	}

	@POST
	@Path("send")
	public Response requestAppraisal(AppraisalReq req)
	{
		logCaller.logStart("Recieved appraisal request", req);

		User user = getUserOrThrow();

		throwIfInvalid(req);

		// We know this is non-empty since we already got the user
		String token = getToken(request).get();

		String sql = ""
			+ "INSERT INTO appraisals "
			+ "(user_id, house_id, mort_id, appraised_value) "
			+ "values "
			+ "(:uid, :hid, :mid, :val) "
			+ "RETURNING id";

		double val = rand.nextGaussian() * 50_000 + 200_000;
		val = Math.round(val * 100.0) / 100.0; // Only keep two decimals

		try (Connection con = Main.sql2o.open())
		{
			int id = con.createQuery(sql)
				.addParameter("uid", user.id)
				.addParameter("hid", req.houseId)
				.addParameter("mid", req.mortId)
				.addParameter("val", val)
				.executeScalar(Integer.class);

			InsReq insReq = new InsReq();
			insReq.houseId = req.houseId;
			insReq.mortId = req.mortId;
			insReq.appraisedValue = val;
			insReq.token = token;

			boolean insSuccess = insCaller.sendIns(insReq);

			MunReq munReq = new MunReq();
			munReq.houseId = req.houseId;
			munReq.mortId = req.mortId;
			munReq.token = token;

			boolean munSuccess = munCaller.sendMun(munReq);

			String updateSql = ""
				+ "UPDATE appraisals SET "
				+ "ins_sent = :isuc, "
				+ "mun_sent = :msuc "
				+ "WHERE id = :id";

			con.createQuery(updateSql)
				.addParameter("isuc", insSuccess)
				.addParameter("msuc", munSuccess)
				.addParameter("id", id)
				.executeUpdate();
		}

		logCaller.logEnd("Appraisal request submitted successfully");

		return Response.ok().build();
	}

	public <T> void throwIfInvalid(T obj)
	{
		if (obj == null) throw new BadRequestException();

		String err = Main.validator.validate(obj).stream()
			.map(v -> msgForViol(v))
			.collect(Collectors.joining(" "));

		if (err.isEmpty()) return;
		Response resp = Response.status(400).entity(err).build();
		logCaller.logEnd("Parameter validation failed");
		throw new BadRequestException(resp);
	}

	private String msgForViol(ConstraintViolation<?> viol)
	{
		Node last = null;
		for (Node obj : viol.getPropertyPath())
		{
			last = obj;
		}

		return String.format("Property '%s' %s. ", last.getName(), viol.getMessage());
	}

	private User getUserOrThrow()
	{
		return getUser(request)
			.orElseThrow(() -> new NotAuthorizedException(Response.status(401).build()));
	}

	public Optional<String> getToken(ContainerRequestContext ctx)
	{
		String header = ctx.getHeaderString("Authorization");
		if (header == null)
		{
			log.debug("Auth header absent");
			return Optional.empty();
		}
		String[] parts = header.split("^[Bb]earer ?");
		if (parts.length != 2)
		{
			log.debug("Header does not have bearer prefix");
			return Optional.empty();
		}
		String token = parts[1];
		return Optional.of(token);
	}

	public Optional<User> getUser(ContainerRequestContext ctx)
	{
		return getToken(ctx).flatMap(t -> authCaller.verify(t));
	}

	public static class Appraisal
	{
		public int id;
		public int userId;
		public String houseId;
		public int mortId;
		public double appraisedValue;
		public boolean insSent;
		public boolean munSent;
	}

	@GET
	@Path("appraisals")
	public List<Appraisal> getAppraisals()
	{
		logCaller.logStart("Getting appraisals", null);

		User user = getUserOrThrow();

		try (Connection con = Main.sql2o.open())
		{
			String sql = ""
				+ "SELECT * FROM appraisals "
				+ "WHERE user_id = :uid";

			List<Appraisal> apps = con.createQuery(sql)
				.addParameter("uid", user.id)
				.setAutoDeriveColumnNames(true)
				.executeAndFetch(Appraisal.class);

			log.debug("Found {} applications", apps.size());
			logCaller.logEnd("Retrieved appraisals");
			return apps;
		}
	}
}
