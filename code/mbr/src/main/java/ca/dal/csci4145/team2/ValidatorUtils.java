package ca.dal.csci4145.team2;

import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.Path.Node;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

import ca.dal.csci4145.team2.resources.LogResource;

public class ValidatorUtils
{
	public static <T> void throwIfInvalid(T obj)
	{
		if (obj == null) throw new BadRequestException();
		
		String err = Main.validator.validate(obj).stream()
			.map(v -> msgForViol(v))
			.collect(Collectors.joining(" "));
		
		if (err.isEmpty()) return;
		Response resp = Response.status(400).entity(err).build();
		LogResource.logEndInternal("MBR", "Parameter validation failed");
		throw new BadRequestException(resp);
	}
	
	private static String msgForViol(ConstraintViolation<?> viol)
	{
		Node last = null;
		for (Node obj : viol.getPropertyPath())
		{
			last = obj;
		}
		
		return String.format("Property '%s' %s. ", last.getName(), viol.getMessage());
	}
}
