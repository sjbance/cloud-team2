package ca.dal.csci4145.team2.resources;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
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
import ca.dal.csci4145.team2.ValidatorUtils;
import ca.dal.csci4145.team2.resources.AuthResource.User;

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

		@NotNull
		@Min(1)
		public Double mortgageValue;

		@NotBlank
		public String houseId;

		@Override
		public String toString()
		{
			return StringUtils.makeToString(this);
		}
	}

	public static class ApplyResp
	{
		public int mortId;

		@Override
		public String toString()
		{
			return StringUtils.makeToString(this);
		};
	}

	@POST
	@Path("apply")
	public Response makeApplication(MortgageReq req)
	{
		LogResource.logStartInternal("MBR", "Received Mortgage Application Request", req);

		User user = getUserOrThrow();

		ValidatorUtils.throwIfInvalid(req);

		String sql = ""
			+ "INSERT INTO applications(user_id, name, mortgage_value, house_id) "
			+ "VALUES (:userid, :name, :mortval, :houseid) "
			+ "RETURNING id";

		try (Connection con = Main.sql2o.open())
		{
			int id = con.createQuery(sql)
				.addParameter("userid", user.id)
				.addParameter("name", req.name)
				.addParameter("mortval", req.mortgageValue)
				.addParameter("houseid", req.houseId)
				.executeScalar(Integer.class);

			ApplyResp resp = new ApplyResp();
			resp.mortId = id;
			LogResource.logEndInternal("MBR", "Application created");
			return Response.ok().entity(resp).build();
		}
	}

	public static class MbrApplication
	{
		public int id;
		public int userId;
		public String name;
		public Double mortgageValue;
		public String houseId;
		public Double salary;
		public String startOfEmployment;
		public Double insuredValue;
		public Double deductible;

		@Override
		public String toString()
		{
			return StringUtils.makeToString(this);
		}
	}

	private User getUserOrThrow()
	{
		return getUser(request)
			.orElseThrow(() -> new NotAuthorizedException(Response.status(401).build()));
	}

	public static Optional<User> getUser(ContainerRequestContext ctx)
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
		return AuthResource.getUser(token);
	}

	@GET
	@Path("applications")
	public List<MbrApplication> getApplications()
	{
		LogResource.logStartInternal("MBR", "Getting Applications", null);

		User user = getUserOrThrow();

		try (Connection con = Main.sql2o.open())
		{
			String sql = ""
				+ "SELECT * FROM applications "
				+ "WHERE user_id = :uid";

			List<MbrApplication> apps = con.createQuery(sql)
				.addParameter("uid", user.id)
				.setAutoDeriveColumnNames(true)
				.executeAndFetch(MbrApplication.class);

			log.debug("Found {} applications", apps.size());
			LogResource.logEndInternal("MBR", "Retrieved applications");
			return apps;
		}
	}

	public static class InsurerInfoReq
	{
		public int mortId;
		public Double insuredValue;
		public Double deductible;
		public String name;

		@Override
		public String toString()
		{
			return StringUtils.makeToString(this);
		}
	}

	@POST
	@Path("insurer")
	public Response receiveInsuranceInfo(InsurerInfoReq req)
	{
		LogResource.logStartInternal("MBR", "Received insurance info", req);

		log.debug("Received insurance info: {}", StringUtils.makeToString(req));

		try (Connection con = Main.sql2o.open())
		{
			String sql = ""
				+ "UPDATE applications SET "
				+ "insured_value = :insur, "
				+ "deductible = :deduct "
				+ "WHERE id = :id";

			int updated = con.createQuery(sql)
				.addParameter("insur", req.insuredValue)
				.addParameter("deduct", req.deductible)
				.addParameter("id", req.mortId)
				.executeUpdate()
				.getResult();

			log.debug("Updated {} rows", updated);
			boolean ok = updated == 1;

			LogResource.logEndInternal("MBR", "Insurance info successfully stored in DB: " + ok);

			return ok ? Response.ok().build() : Response.status(400).build();
		}
	}

	public static class EmployerInfoReq
	{
		public int mortId;
		public String name;
		public Double salary;
		public String startOfEmployment;

		@Override
		public String toString()
		{
			return StringUtils.makeToString(this);
		}
	}

	@POST
	@Path("employer")
	public Response receiveEmployerInfo(EmployerInfoReq req)
	{
		LogResource.logStartInternal("MBR", "Received Employer info", req);
		log.debug("Received employer info: {}", StringUtils.makeToString(req));

		try (Connection con = Main.sql2o.open())
		{
			String sql = ""
				+ "UPDATE applications SET "
				+ "salary = :salary, "
				+ "start_of_employment = :stofempl "
				+ "WHERE id = :mortid";

			int updated = con.createQuery(sql)
				.addParameter("salary", req.salary)
				.addParameter("stofempl", req.startOfEmployment)
				.addParameter("mortid", req.mortId)
				.executeUpdate()
				.getResult();

			log.debug("Updated {} rows", updated);
			boolean ok = updated == 1;
			LogResource.logEndInternal("MBR", "Employer info stored in DB successfully: " + ok);
			return ok ? Response.ok().build() : Response.status(400).build();
		}
	}
}
