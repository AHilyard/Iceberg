package com.anthonyhilyard.iceberg.events;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

public class Event<T>
{
	/**
	 * The invoker field. This should be updated by the implementation to
	 * always refer to an instance containing all code that should be
	 * executed upon event emission.
	 */
	protected volatile T invoker;

	/**
	 * Returns the invoker instance.
	 *
	 * <p>An "invoker" is an object which hides multiple registered
	 * listeners of type T under one instance of type T, executing
	 * them and leaving early as necessary.
	 *
	 * @return The invoker instance.
	 */
	public final T invoker() { return invoker; }

	private final Function<T[], T> invokerFactory;
	protected final Object lock = new Object();
	private T[] listeners;

	private void addListener(T listener)
	{
		int oldLength = listeners.length;
		listeners = Arrays.copyOf(listeners, oldLength + 1);
		listeners[oldLength] = listener;
	}

	@SuppressWarnings("unchecked")
	public Event(Class<? super T> type, Function<T[], T> invokerFactory)
	{
		this.invokerFactory = invokerFactory;
		this.listeners = (T[]) Array.newInstance(type, 0);
		update();
	}

	void update()
	{
		this.invoker = invokerFactory.apply(listeners);
	}

	/**
	 * Register a listener to the event.
	 *
	 * @param listener The desired listener.
	 */
	public void register(T listener)
	{
		Objects.requireNonNull(listener, "Tried to register a null listener!");

		synchronized (lock)
		{
			addListener(listener);
			update();
		}
	}

	public int listenerCount()
	{
		return listeners.length;
	}
}
