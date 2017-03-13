package ca.dal.csci4145.team2.callers;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.dal.csci4145.team2.Main;

public class MunCaller
{
	private static final Logger log = LoggerFactory.getLogger(MunCaller.class);
	private final Client client = ClientBuilder.newBuilder().sslContext(Main.noCheckSslContext)
		.hostnameVerifier((s1, s2) -> true).build();
	private final WebTarget munTarget = client
		.target("https://mun.danielstout.ca:3001/mun/insurance");

	public static class MunReq
	{
		public int mortId;
		public String houseId;
		public String token;
	}

	public boolean sendMun(MunReq req)
	{
		Response resp = munTarget.request().post(Entity.entity(req, MediaType.APPLICATION_JSON));
		log.debug("Response from mun: {}", resp);
		return resp.getStatusInfo().getFamily().equals(Family.SUCCESSFUL);
	}
}
