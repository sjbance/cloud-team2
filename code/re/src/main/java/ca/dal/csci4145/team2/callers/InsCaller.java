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

public class InsCaller
{
	private static final Logger log = LoggerFactory.getLogger(InsCaller.class);
	private final Client client = ClientBuilder.newBuilder().sslContext(Main.noCheckSslContext)
		.hostnameVerifier((s1, s2) -> true).build();
	private final WebTarget insTarget = client
		.target(
			"https://ec2-54-202-139-197.us-west-2.compute.amazonaws.com:3001/ins/insurance_quote");

	public static class InsReq
	{
		public int mortId;
		public String houseId;
		public double appraisedValue;
		public String token;
	}

	public boolean sendIns(InsReq req)
	{
		Response resp = insTarget.request().post(Entity.entity(req, MediaType.APPLICATION_JSON));
		log.debug("Response from ins: {}", resp);
		return resp.getStatusInfo().getFamily().equals(Family.SUCCESSFUL);
	}
}
