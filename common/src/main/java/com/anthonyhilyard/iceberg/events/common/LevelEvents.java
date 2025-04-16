package com.anthonyhilyard.iceberg.events.common;

import com.anthonyhilyard.iceberg.events.Event;
import com.anthonyhilyard.iceberg.events.EventFactory;

import net.minecraft.world.level.LevelAccessor;

public final class LevelEvents
{
	protected LevelEvents() { }

	public static final Event<LevelEvents.Load> LOAD = EventFactory.create(LevelEvents.Load.class,
		callbacks -> (level) -> {
			for (LevelEvents.Load callback : callbacks)
			{
				callback.onLoad(level);
			}
		});

	public static final Event<LevelEvents.Unload> UNLOAD = EventFactory.create(LevelEvents.Unload.class,
		callbacks -> (level) -> {
			for (LevelEvents.Unload callback : callbacks)
			{
				callback.onUnload(level);
			}
		});

	@FunctionalInterface public interface Load { void onLoad(LevelAccessor level); }

	@FunctionalInterface public interface Unload { void onUnload(LevelAccessor level); }
}