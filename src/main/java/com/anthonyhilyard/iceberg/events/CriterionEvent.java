package com.anthonyhilyard.iceberg.events;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.world.entity.player.Player;
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
	private final AdvancementHolder advancementHolder;
	private final String criterionKey;

	public CriterionEvent(Player player, AdvancementHolder advancementHolder, String criterionKey)
	{
		super(player);
		this.advancementHolder = advancementHolder;
		this.criterionKey = criterionKey;
	}

	@Deprecated
	public Advancement getAdvancement()
	{
		return advancementHolder.value();
	}

	public AdvancementHolder getAdvancementHolder()
	{
		return advancementHolder;
	}

	public String getCriterionKey()
	{
		return criterionKey;
	}
}
