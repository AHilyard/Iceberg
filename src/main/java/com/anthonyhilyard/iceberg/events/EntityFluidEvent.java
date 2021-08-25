package com.anthonyhilyard.iceberg.events;

import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraftforge.event.entity.EntityEvent;

public class EntityFluidEvent extends EntityEvent
{
	private final Fluid fluid;
	
	private EntityFluidEvent(Entity entity, Fluid fluid)
	{
		super(entity);
		this.fluid = fluid;
	}

	public Fluid getFluid()
	{
		return fluid;
	}

	/**
	 * This event is fired when an entity enters a fluid to at least eye-level.
	 * If this is a player, they will see the "submerged in fluid" effect at this point.
	 * <br>
	 * This event is not {@link net.minecraftforge.eventbus.api.Cancelable}.<br>
	 * <br>
	 * This event does not have a result. {@link HasResult}<br>
	 * <br>
	 * This event is fired on the {@link MinecraftForge#EVENT_BUS}.
	 */
	public static class Entered extends EntityFluidEvent
	{
		public Entered(Entity entity, Fluid fluid)
		{
			super(entity, fluid);
		}
	}

	/**
	 * This event is fired when an entity was previously submerged in a fluid to at least eye-level and no longer are.
	 * If this is a player, they will no longer see the "submerged in fluid" effect at this point.
	 * <br>
	 * This event is not {@link net.minecraftforge.eventbus.api.Cancelable}.<br>
	 * <br>
	 * This event does not have a result. {@link HasResult}<br>
	 * <br>
	 * This event is fired on the {@link MinecraftForge#EVENT_BUS}.
	 */
	public static class Exited extends EntityFluidEvent
	{
		public Exited(Entity entity, Fluid fluid)
		{
			super(entity, fluid);
		}
	}
}
