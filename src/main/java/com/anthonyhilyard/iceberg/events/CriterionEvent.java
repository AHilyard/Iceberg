package com.anthonyhilyard.iceberg.events;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraft.advancements.Advancement;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.MinecraftForge;

/**
 * This event is fired when a player is granted an advancement criterion.
 * <br>
 * This event is not {@link net.minecraftforge.eventbus.api.Cancelable}.<br>
 * <br>
 * This event does not have a result. {@link HasResult}<br>
 * <br>
 * This event is fired on the {@link MinecraftForge#EVENT_BUS}.
 */
public class CriterionEvent extends PlayerEvent
{
	private final Advancement advancement;
	private final String criterionKey;

	public CriterionEvent(PlayerEntity player, Advancement advancement, String criterionKey)
	{
		super(player);
		this.advancement = advancement;
		this.criterionKey = criterionKey;
	}

	public Advancement getAdvancement()
	{
		return advancement;
	}

	public String getCriterionKey()
	{
		return criterionKey;
	}
}
