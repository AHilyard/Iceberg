package com.anthonyhilyard.iceberg.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.DeltaTracker;

public class RenderTickEvents
{
	public RenderTickEvents() { }

	public static final Event<RenderTickEvents.Start> START = EventFactory.createArrayBacked(RenderTickEvents.Start.class,
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
