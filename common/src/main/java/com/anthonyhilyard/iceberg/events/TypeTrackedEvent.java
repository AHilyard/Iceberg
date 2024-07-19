package com.anthonyhilyard.iceberg.events;

import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.Maps;

public class TypeTrackedEvent<S, T> extends Event<T>
{
	private Map<Class<? extends S>, T> listenerTypes = Maps.newHashMap();

	public TypeTrackedEvent(Class<? super T> type, Function<T[], T> invokerFactory)
	{
		super(type, invokerFactory);
	}
	
	@Override
	public void register(T listener)
	{
		throw new UnsupportedOperationException("Register(listener) unsupported.  Use Register(type, listener) instead!");
	}

	public void register(Class<? extends S> type, T listener)
	{
		super.register(listener);
		listenerTypes.put(type, listener);
	}

	public Map<Class<? extends S>, T> getListenerTypes()
	{
		return listenerTypes;
	}
}
