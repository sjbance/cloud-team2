package ca.dal.csci4145.team2;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringUtils
{
	private static final Logger log = LoggerFactory.getLogger(StringUtils.class);
	
	public static String makeToString(Object obj)
	{
		Class<?> clazz = obj.getClass();
		String name = clazz.getSimpleName();
		StringBuilder builder = new StringBuilder(name + ": {");
		Field[] fields = clazz.getDeclaredFields();
		for (int i = 0; i < fields.length; i++)
		{
			Field field = fields[i];
			if (Modifier.isStatic(field.getModifiers())) continue;
			try
			{
				String fieldName = field.getName();
				field.setAccessible(true);
				Object value = field.get(obj);
				String valueRep = null;
				if (value == null)
				{
					valueRep = "null";
				}
				else if (field.getType().isAssignableFrom(String.class))
				{
					valueRep = "\"" + value + "\"";
				}
				else
				{
					valueRep = value.toString();
				}
				builder.append(fieldName + ":" + valueRep);
				if (i < fields.length - 1) builder.append(", ");
			}
			catch (IllegalArgumentException | IllegalAccessException e)
			{
				log.error("Error generating toString", e);
			}
		}
		
		return builder.append("}").toString();
	}
}
