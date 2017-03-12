package ca.dal.csci4145.team2.resources;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.data.Row;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import ca.dal.csci4145.team2.Main;
import ca.dal.csci4145.team2.StringUtils;

@Path("auth")
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource
{
	public static class LoginReq
	{
		public String user;
		public String pass;
	}

	public static class TokenHolder
	{
		public String token;
	}

	private static final String TOKEN_SECRET = "hzdPrRolqW22FYWImlXG0ZZXwd1V4I3CTuCpxAzodvX61zPXU69yrWiC2jhnhwA";

	public static final Algorithm TOKEN_ALGO = getAlgorithm();

	private static Algorithm getAlgorithm()
	{
		try
		{
			return Algorithm.HMAC256(TOKEN_SECRET);
		}
		catch (IllegalArgumentException | UnsupportedEncodingException e)
		{
			throw new RuntimeException(e);
		}
	}

	private static final Logger log = LoggerFactory.getLogger(AuthResource.class);
	private static final String ISSUER = "mbr";

	private static final JWTVerifier verifier = JWT
		.require(TOKEN_ALGO)
		.withIssuer(ISSUER)
		.build();

	/**
	 * Returns either a 200 status code with a {@Code LoginResp}, or a 400 status code with an empty
	 * body depending upon whether the request is for a valid user
	 * @param req
	 * @return
	 */
	@POST
	@Path("login")
	public Response login(LoginReq req)
	{
		log.debug("Received req: {}", StringUtils.makeToString(req));

		try (Connection con = Main.sql2o.open())
		{
			String sql = "SELECT hashed_password, id FROM users WHERE username = :user";

			List<Row> rows = con
				.createQuery(sql)
				.addParameter("user", req.user)
				.executeAndFetchTable().rows();

			if (!rows.isEmpty())
			{
				Row row = rows.get(0);
				String stored = row.getString(0);
				int id = row.getInteger(1);

				if (BCrypt.checkpw(req.pass, stored))
				{
					log.debug("Valid password!");

					String token = JWT.create()
						.withIssuer(ISSUER)
						.withClaim("id", id)
						.withSubject(req.user)
						.sign(TOKEN_ALGO);

					TokenHolder resp = new TokenHolder();
					resp.token = token;
					return Response.ok().entity(resp).build();
				}

				log.debug("Invalid password");

			}
			log.debug("Unknown user");
			return Response.status(Status.UNAUTHORIZED).build();
		}
	}

	public static class User
	{
		public String username;
		public int id;
	}
	
	@POST
	@Path("verify")
	public Response verify(TokenHolder resp)
	{
		Optional<User> u = getUser(resp.token);
		if (!u.isPresent())
		{
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		return Response.ok().entity(u.get()).build();
	}

	public static Optional<User> getUser(String token)
	{
		try
		{
			DecodedJWT tok = verifier.verify(token);
			User user = new User();
			user.username = tok.getSubject();
			user.id = tok.getClaim("id").asDouble().intValue();
			return Optional.of(user);
		}
		catch (JWTVerificationException ex)
		{
			log.debug("Token present but invalid");
			return Optional.empty();
		}
	}
}
