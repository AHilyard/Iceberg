package com.anthonyhilyard.iceberg.events;

import java.lang.reflect.Array;
import java.util.function.Function;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public class ToggleableEvent<T>
{
	private T dummyInvoker;
	private boolean disabled = false;
	private Event<T> event;

	@SuppressWarnings("unchecked")
	private ToggleableEvent(Class<? super T> type, Function<T[], T> invokerFactory)
	{
		event = EventFactory.createArrayBacked(type, invokerFactory);
		this.dummyInvoker = invokerFactory.apply((T[]) Array.newInstance(type, 0));
	}

	public static <T> ToggleableEvent<T> create(Class<? super T> type, Function<T[], T> invokerFactory)
	{
		return new ToggleableEvent<>(type, invokerFactory);
	}

	public void register(T listener)
	{
		event.register(listener);
	}

	public T invoker()
	{
		if (!disabled)
		{
			return event.invoker();
		}
		else
		{
			return dummyInvoker;
		}
	}

	public boolean disable()
	{
		if (disabled)
		{
			return false;
		}
		else
		{
			disabled = true;
			return true;
		}
	}

	public boolean enable()
	{
		if (!disabled)
		{
			return false;
		}
		else
		{
			disabled = false;
			return true;
		}
	}
}
