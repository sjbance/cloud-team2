package ca.dal.csci4145.team2.cors;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;

public class CorsResponseFilter implements ContainerResponseFilter
{
	@Override
	public void filter(ContainerRequestContext requestContext,
		ContainerResponseContext responseContext) throws IOException
	{
		MultivaluedMap<String, Object> map = responseContext.getHeaders();
		map.add("Access-Control-Allow-Origin", "*");
		map.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD");
		map.add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
		map.add("Access-Control-Allow-Credentials", "true");
	}
}
