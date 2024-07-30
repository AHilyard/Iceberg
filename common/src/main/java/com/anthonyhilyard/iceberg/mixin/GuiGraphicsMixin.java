package com.anthonyhilyard.iceberg.mixin;

import java.lang.reflect.Field;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.services.Services;
import com.anthonyhilyard.iceberg.util.ITooltipAccess;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin implements ITooltipAccess
{
	@Unique
	private static Field tooltipStackField = null;

	@Override
	public void setIcebergTooltipStack(ItemStack stack)
	{
		if (tooltipStackField == null)
		{
			try
			{
				switch (Services.getPlatformHelper().getPlatformName())
				{
					case "Fabric":
						tooltipStackField = GuiGraphics.class.getDeclaredField("icebergTooltipStack");
					default:
						tooltipStackField = GuiGraphics.class.getDeclaredField("tooltipStack");
						break;
				}
				
				tooltipStackField.setAccessible(true);
			}
			catch (Exception e)
			{
				Iceberg.LOGGER.debug(ExceptionUtils.getStackTrace(e));
			}
		}
		
		try
		{
			tooltipStackField.set(this, stack);
		}
		catch (Exception e) {}
	}

	@Override
	public ItemStack getIcebergTooltipStack()
	{
		if (tooltipStackField == null)
		{
			try
			{
				switch (Services.getPlatformHelper().getPlatformName())
				{
					case "Fabric":
						tooltipStackField = GuiGraphics.class.getDeclaredField("icebergTooltipStack");
					default:
						tooltipStackField = GuiGraphics.class.getDeclaredField("tooltipStack");
						break;
				}
				
				tooltipStackField.setAccessible(true);
			}
			catch (Exception e)
			{
				Iceberg.LOGGER.debug(ExceptionUtils.getStackTrace(e));
			}
		}

		try
		{
			return (ItemStack)tooltipStackField.get(this);
		}
		catch (Exception e) {}

		return ItemStack.EMPTY;
	}
}
