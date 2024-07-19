package com.anthonyhilyard.iceberg.fabric.mixin;

import java.lang.reflect.Field;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.anthonyhilyard.iceberg.util.Tooltips;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;

@Mixin(TooltipRenderUtil.class)
public class TooltipRenderUtilMixin
{
	@Unique
	private static Field horizontalLineColorField = null;

	@Inject(method = "renderTooltipBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;renderHorizontalLine(Lnet/minecraft/client/gui/GuiGraphics;IIIII)V", shift = At.Shift.BEFORE, ordinal = 0))
	private static void icebergRenderTooltipBackgroundOne(GuiGraphics graphics, int x, int y, int width, int height, int z, CallbackInfo info)
	{
		if (horizontalLineColorField == null)
		{
			try
			{
				horizontalLineColorField = TooltipRenderUtil.class.getDeclaredField("horizontalLineColor");
				horizontalLineColorField.setAccessible(true);
			}
			catch (Exception e) {}
		}
		
		try
		{
			horizontalLineColorField.set(null, Tooltips.currentColors.backgroundColorStart());
		}
		catch (Exception e) {}
	}

	@Inject(method = "renderTooltipBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;renderHorizontalLine(Lnet/minecraft/client/gui/GuiGraphics;IIIII)V", shift = At.Shift.BEFORE, ordinal = 1))
	private static void icebergRenderTooltipBackgroundTwo(GuiGraphics graphics, int x, int y, int width, int height, int z, CallbackInfo info)
	{
		try
		{
			horizontalLineColorField.set(null, Tooltips.currentColors.backgroundColorEnd());
		}
		catch (Exception e) {}
	}
}