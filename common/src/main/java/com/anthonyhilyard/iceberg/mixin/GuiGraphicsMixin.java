package com.anthonyhilyard.iceberg.mixin;

import java.lang.reflect.Field;
import java.util.List;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents;
import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents.ColorExtResult;
import com.anthonyhilyard.iceberg.services.Services;
import com.anthonyhilyard.iceberg.util.ITooltipAccess;
import com.anthonyhilyard.iceberg.util.Tooltips;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin// implements ITooltipAccess
{
	/**
	 * //TODO tooltips
	 * 1.21.11 renders tooltips as full sprites, disabled for now.
	 * Recommended fix is to write an entire new tooltip rendering engine.
	 */
	/*
	@Shadow
	@Final
	private Minecraft minecraft;

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

	@Inject(method = "renderTooltipInternal",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;renderTooltipBackground(Lnet/minecraft/client/gui/GuiGraphics;IIIIILnet/minecraft/resources/Identifier;)V",
			ordinal = 0, shift = Shift.BEFORE))
	private void preFillGradient(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, Identifier resource, CallbackInfo info)
	{
		GuiGraphics self = (GuiGraphics)(Object)this;
		Screen currentScreen = minecraft.screen;
		ItemStack containerStack = ItemStack.EMPTY;
		if (currentScreen != null && currentScreen instanceof AbstractContainerScreen<?> containerScreen)
		{
			Slot hoveredSlot = containerScreen.hoveredSlot;
			if (hoveredSlot != null)
			{
				containerStack = hoveredSlot.getItem();
			}
		}

		if (containerStack.isEmpty())
		{
			containerStack = getIcebergTooltipStack();
		}

		if (!containerStack.isEmpty())
		{
			int backgroundStart = Tooltips.DEFAULT_COLORS.backgroundColorStart().getValue();
			int backgroundEnd = Tooltips.DEFAULT_COLORS.backgroundColorEnd().getValue();
			int borderStart = Tooltips.DEFAULT_COLORS.borderColorStart().getValue();
			int borderEnd = Tooltips.DEFAULT_COLORS.borderColorEnd().getValue();
			boolean gradientBackground = false;
			boolean gradientBorder = false;

			// Do colors now, sure why not.
			ColorExtResult result = RenderTooltipEvents.COLOREXT.invoker().onColor(containerStack, self, x, y, font, backgroundStart, backgroundEnd, borderStart, borderEnd, components, false, 0, resource, gradientBackground, gradientBorder);
			if (result != null)
			{
				backgroundStart = result.backgroundStart();
				backgroundEnd = result.backgroundEnd();
				borderStart = result.borderStart();
				borderEnd = result.borderEnd();
				gradientBackground = result.gradientBackground();
				gradientBorder = result.gradientBorder();
			}

			Tooltips.currentColors = new Tooltips.TooltipColors(TextColor.fromRgb(backgroundStart), TextColor.fromRgb(backgroundEnd), TextColor.fromRgb(borderStart), TextColor.fromRgb(borderEnd));
			Tooltips.gradientBackground = gradientBackground;
			Tooltips.gradientBorder = gradientBorder;
		}
		else
		{
			Tooltips.currentColors = Tooltips.DEFAULT_COLORS;
			Tooltips.gradientBackground = false;
			Tooltips.gradientBorder = false;
		}
	}
	 */
}
