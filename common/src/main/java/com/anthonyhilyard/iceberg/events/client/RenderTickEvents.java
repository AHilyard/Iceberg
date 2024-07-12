package com.anthonyhilyard.iceberg.events.client;

import com.anthonyhilyard.iceberg.events.Event;
import com.anthonyhilyard.iceberg.events.EventFactory;

import net.minecraft.client.DeltaTracker;

public class RenderTickEvents
{
	public RenderTickEvents() { }

	public static final Event<RenderTickEvents.Start> START = EventFactory.create(RenderTickEvents.Start.class,
		callbacks -> (timer) -> {
		for (RenderTickEvents.Start callback : callbacks)
		{
			callback.onStart(timer);
		}
	});

	@FunctionalInterface
	public interface Start
	{
		void onStart(DeltaTracker timer);
	}
}
