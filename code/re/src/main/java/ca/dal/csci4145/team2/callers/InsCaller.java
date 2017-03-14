package ca.dal.csci4145.team2.callers;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

public class InsCaller
{
	private final Client client = ClientBuilder.newClient();
	private final WebTarget insTarget = client
		.target("");
	
	public static class InsReq
	{
		public int mortId;
		public String houseId;
		public double appraisedValue;
		public String token;
	}
	
	public boolean sendIns(InsReq req)
	{
		return true;
		//		Response resp = insTarget.request().post(Entity.entity(req, MediaType.APPLICATION_JSON));
		// log.debug("Response from ins: {}", resp);
		//		return resp.getStatusInfo().getFamily().equals(Family.SUCCESSFUL);
	}
}
