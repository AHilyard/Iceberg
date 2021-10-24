package com.anthonyhilyard.iceberg.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.Fluid;


public final class EntityFluidEvents
{
	public EntityFluidEvents() { }

	/**
	 * Called when an Entity enters a new fluid.
	 */
	public static final Event<EntityFluidEvents.Entered> ENTERED = EventFactory.createArrayBacked(EntityFluidEvents.Entered.class, callbacks -> (entity, fluid) -> {
		for (EntityFluidEvents.Entered callback : callbacks)
		{
			callback.onEntered(entity, fluid);
		}
	});

	/**
	 * Called when an Entity exits a fluid.
	 */
	public static final Event<EntityFluidEvents.Exited> EXITED = EventFactory.createArrayBacked(EntityFluidEvents.Exited.class, callbacks -> (entity, fluid) -> {
		for (EntityFluidEvents.Exited callback : callbacks)
		{
			callback.onExited(entity, fluid);
		}
	});

	@FunctionalInterface
	public interface Entered
	{
		void onEntered(Entity entity, Fluid fluid);
	}

	@FunctionalInterface
	public interface Exited
	{
		void onExited(Entity entity, Fluid fluid);
	}
}