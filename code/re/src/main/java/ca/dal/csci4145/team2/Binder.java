package ca.dal.csci4145.team2;

import javax.inject.Singleton;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

import ca.dal.csci4145.team2.callers.AuthCaller;
import ca.dal.csci4145.team2.callers.InsCaller;
import ca.dal.csci4145.team2.callers.LogCaller;
import ca.dal.csci4145.team2.callers.MunCaller;

public class Binder extends AbstractBinder
{

	@Override
	protected void configure()
	{
		bindSingleton(LogCaller.class, AuthCaller.class, InsCaller.class, MunCaller.class);
	}

	private void bindSingleton(Class<?>... classes)
	{
		for (Class<?> clazz : classes)
		{
			bind(clazz).to(clazz).in(Singleton.class);
		}
	}

}
