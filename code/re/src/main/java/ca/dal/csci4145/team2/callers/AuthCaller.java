package ca.dal.csci4145.team2.callers;

import java.util.Optional;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class AuthCaller
{
	public static class User
	{
		public String username;
		public int id;
	}

	public static class VerifyReq
	{
		public String token;
	}

	private final Client client = ClientBuilder.newClient();
	private final WebTarget verifyTarget = client
		.target("https://mbr.danielstout.ca/api/auth/verify");

	public Optional<User> verify(String token)
	{
		VerifyReq req = new VerifyReq();
		req.token = token;

		Response resp = verifyTarget.request(MediaType.APPLICATION_JSON)
			.post(Entity.entity(req, MediaType.APPLICATION_JSON));
		if (resp.getStatus() == 200)
		{
			User u = resp.readEntity(User.class);
			return Optional.of(u);
		}
		return Optional.empty();
	}
}
