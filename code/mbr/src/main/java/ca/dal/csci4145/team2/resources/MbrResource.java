package ca.dal.csci4145.team2.resources;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Path.Node;
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
import ca.dal.csci4145.team2.StringUtils;

@Path("mbr")
@Produces(MediaType.APPLICATION_JSON)
public class MbrResource
{
	private static final Logger log = LoggerFactory.getLogger(MbrResource.class);

	@Inject
	ContainerRequestContext request;

	public static class MortgageReq
	{
		@NotBlank
		public String name;

		@NotBlank
		public String address;

		@NotBlank
		public String phone;

		@NotBlank
		public String employer;

		@NotBlank
		public String lifeInsurance;
	}

	public static class ApplyResp
	{
		public int applicationId;
	}

	private <T> void validateObject(T obj)
	{
		if (obj == null) throw new BadRequestException();

		String err = Main.validator.validate(obj).stream()
			.map(v -> msgForViol(v))
			.collect(Collectors.joining(" "));

		if (err.isEmpty()) return;
		Response resp = Response.status(400).entity(err).build();
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

	public static class MbrApplication
	{
		public int id;
		public int userId;
		public String name;
		public String address;
		public String phoneNumber;
		public String employer;
		public String lifeInsurance;
		public String jobTitle;
		public Double jobSalary;
		public Integer jobYears;
		public String lifePolicyId;
		public Double lifePolicyValue;
	}
	
	private String getUserOrThrow()
	{
		return getUsername(request)
			.orElseThrow(() -> new NotAuthorizedException(Response.status(401).build()));
	}

	public static Optional<String> getUsername(ContainerRequestContext ctx)
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
		return AuthResource.getUser(token).map(u -> u.username);
	}

	@GET
	@Path("applications")
	public List<MbrApplication> getApplications()
	{
		String user = getUserOrThrow();

		try (Connection con = Main.sql2o.open())
		{
			String sql = ""
				+ "SELECT * FROM mbr_applications "
				+ "WHERE user_id = (SELECT id FROM mbr_users WHERE username = :user)";

			List<MbrApplication> apps = con.createQuery(sql)
				.addParameter("user", user)
				.setAutoDeriveColumnNames(true)
				.executeAndFetch(MbrApplication.class);

			log.debug("Found {} applications", apps.size());
			return apps;
		}
	}

	public static class InsurerInfoReq
	{
		public int mortgageApplicationId;
		public String policyId;
		public Double policyValue;
	}

	@POST
	@Path("insurer")
	public Response receiveInsuranceInfo(InsurerInfoReq req)
	{
		log.debug("Received insurance info: {}", StringUtils.makeToString(req));

		try (Connection con = Main.sql2o.open())
		{
			String sql = ""
				+ "UPDATE mbr_applications SET "
				+ "life_policy_id = :polId, "
				+ "life_policy_value = :polVal "
				+ "WHERE id = :id";

			int updated = con.createQuery(sql)
				.addParameter("polId", req.policyId)
				.addParameter("polVal", req.policyValue)
				.addParameter("id", req.mortgageApplicationId)
				.executeUpdate()
				.getResult();

			log.debug("Updated {} rows", updated);
			return updated == 1 ? Response.ok().build() : Response.status(400).build();
		}
	}

	public static class EmployerInfoReq
	{
		public int mortgageApplicationId;
		public String jobTitle;
		public Double jobSalary;
		public Integer jobYears;
	}

	@POST
	@Path("employer")
	public Response receiveEmployerInfo(EmployerInfoReq req)
	{
		log.debug("Received employer info: {}", StringUtils.makeToString(req));

		try (Connection con = Main.sql2o.open())
		{
			String sql = ""
				+ "UPDATE mbr_applications SET "
				+ "job_title = :title, "
				+ "job_salary = :salary, "
				+ "job_years = :years "
				+ "WHERE id = :id";

			int updated = con.createQuery(sql)
				.addParameter("title", req.jobTitle)
				.addParameter("salary", req.jobSalary)
				.addParameter("years", req.jobYears)
				.addParameter("id", req.mortgageApplicationId)
				.executeUpdate()
				.getResult();

			log.debug("Updated {} rows", updated);
			return updated == 1 ? Response.ok().build() : Response.status(400).build();
		}
	}

	@POST
	@Path("apply")
	public Response process(MortgageReq req)
	{
		String user = getUserOrThrow();

		validateObject(req);

		String sql = ""
			+ "INSERT INTO mbr_applications(user_id, name, address, phone_number, employer, life_insurance) "
			+ "VALUES ((SELECT id FROM mbr_users WHERE username = :user), :name, :addr, :phone, :empl, :life) "
			+ "RETURNING id";

		try (Connection con = Main.sql2o.open())
		{
			int id = con.createQuery(sql)
				.addParameter("user", user)
				.addParameter("name", req.name)
				.addParameter("addr", req.address)
				.addParameter("phone", req.phone)
				.addParameter("empl", req.employer)
				.addParameter("life", req.lifeInsurance)
				.executeScalar(Integer.class);

			ApplyResp resp = new ApplyResp();
			resp.applicationId = id;
			return Response.ok().entity(resp).build();
		}
	}
}
