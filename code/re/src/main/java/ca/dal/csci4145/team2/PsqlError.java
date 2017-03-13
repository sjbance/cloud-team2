package ca.dal.csci4145.team2;

import org.postgresql.util.PSQLException;
import org.sql2o.Sql2oException;

public enum PsqlError
{
	UNIQUE_VIOLATION(23505);

	private int code;

	PsqlError(int code)
	{
		this.code = code;
	}

	public int getCode()
	{
		return code;
	}

	public boolean matches(Sql2oException ex)
	{
		Throwable cause = ex.getCause();
		if (!PSQLException.class.isInstance(cause)) return false;
		PSQLException psqlCause = (PSQLException) cause;
		return Integer.parseInt(psqlCause.getSQLState()) == code;
	}
}
