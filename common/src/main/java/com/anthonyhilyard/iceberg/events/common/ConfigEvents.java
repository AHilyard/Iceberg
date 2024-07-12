package com.anthonyhilyard.iceberg.events.common;

import com.anthonyhilyard.iceberg.config.IIcebergConfigSpec;
import com.anthonyhilyard.iceberg.config.IcebergConfig;
import com.anthonyhilyard.iceberg.events.Event;
import com.anthonyhilyard.iceberg.events.EventFactory;

public final class ConfigEvents
{
	protected ConfigEvents() { }

	public static final Event<ConfigEvents.Register> REGISTER = EventFactory.create(ConfigEvents.Register.class,
		callbacks -> (clazz, spec, modId) -> {
			for (ConfigEvents.Register callback : callbacks)
			{
				callback.onRegister(clazz, spec, modId);
			}
		});

	public static final Event<ConfigEvents.Load> LOAD = EventFactory.create(ConfigEvents.Load.class,
		callbacks -> (modId) -> {
			for (ConfigEvents.Load callback : callbacks)
			{
				callback.onLoad(modId);
			}
		});

	public static final Event<ConfigEvents.Reload> RELOAD = EventFactory.create(ConfigEvents.Reload.class,
		callbacks -> (modId) -> {
			for (ConfigEvents.Reload callback : callbacks)
			{
				callback.onReload(modId);
			}
		});

	@FunctionalInterface public interface Register { void onRegister(Class<? extends IcebergConfig<?>> clazz, IIcebergConfigSpec spec, String modId); }

	@FunctionalInterface public interface Load { void onLoad(String modId); }

	@FunctionalInterface public interface Reload { void onReload(String modId); }
}
