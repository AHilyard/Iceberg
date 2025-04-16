package com.anthonyhilyard.iceberg.mixin;

import java.lang.reflect.Field;
import java.util.List;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents;
import com.anthonyhilyard.iceberg.services.Services;
import com.anthonyhilyard.iceberg.util.ITooltipAccess;
import com.anthonyhilyard.iceberg.util.Tooltips;
import com.anthonyhilyard.iceberg.util.Tooltips.TooltipRenderContext;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.world.item.ItemStack;

@Mixin(value = GuiGraphics.class, priority = 1001)
public class GuiGraphicsMixin implements ITooltipAccess
{
	@Unique
	private static Field tooltipStackField = null;

	
	@Unique
	private int storedTooltipWidth;
	
	@Unique
	private int storedTooltipHeight;

	@Unique
	private Vector2ic storedPostPos;

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

	@ModifyArg(method = "renderTooltipInternal",
			   at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;positionTooltip(IIIIII)Lorg/joml/Vector2ic;"), index = 4)
	private int storeTooltipWidth(int width)
	{
		TooltipRenderContext context = Tooltips.getCurrentRenderContext();
		if (context.maxWidth() < width && context.maxWidth() > 0)
		{
			width = context.maxWidth();
		}

		storedTooltipWidth = width;
		return width;
	}

	@ModifyArg(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;positionTooltip(IIIIII)Lorg/joml/Vector2ic;"), index = 5)
	private int storeTooltipHeight(int height)
	{
		TooltipRenderContext context = Tooltips.getCurrentRenderContext();
		if (context.maxHeight() < height && context.maxHeight() > 0)
		{
			height = context.maxHeight();
		}

		storedTooltipHeight = height;
		return height;
	}

	@ModifyVariable(method = "renderTooltipInternal", at = @At(value = "STORE", ordinal = 0))
	private Vector2ic storeTooltipPosition(Vector2ic pos)
	{
		storedPostPos = pos;
		return pos;
	}

	@Inject(method = "renderTooltipInternal",
			at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;positionTooltip(IIIIII)Lorg/joml/Vector2ic;"), cancellable = true)
	private void storeCalculatedRect(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo info)
	{
		Tooltips.setCurrentRect(storedPostPos.x(), storedPostPos.y(), storedTooltipWidth, storedTooltipHeight);
		if (Tooltips.getCurrentRenderContext() == Tooltips.CALCULATE_RECT_CONTEXT)
		{
			info.cancel();
		}
	}

	@Inject(method = "renderTooltipInternal", at = @At(value = "HEAD"))
	private void preRenderTooltipInternal(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo info)
	{
		if (Tooltips.getCurrentRenderContext() != Tooltips.CALCULATE_RECT_CONTEXT)
		{
			Tooltips.setAnyTooltipsVisible(true);
		}

		if (Services.getPlatformHelper().isModLoaded("emi") && Tooltips.getCurrentRenderContext() == Tooltips.EMPTY_CONTEXT)
		{
			try
			{
				ItemStack tooltipStack = (ItemStack)Class.forName("com.anthonyhilyard.iceberg.compat.EMIHandler").getMethod("getTooltipStack", List.class).invoke(null, components);
				if (!tooltipStack.isEmpty())
				{
					setIcebergTooltipStack(tooltipStack);
				}
			}
			catch (Exception e)
			{
				Iceberg.LOGGER.debug(ExceptionUtils.getStackTrace(e));
			}
		}
	}

	@Inject(method = "renderTooltipInternal", at = @At(value = "TAIL"))
	private void renderTooltipInternalTail(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo info)
	{
		GuiGraphics self = (GuiGraphics)(Object)this;

		ItemStack tooltipStack = getIcebergTooltipStack();

		if (!components.isEmpty())
		{
			TooltipRenderContext context = Tooltips.getCurrentRenderContext();
			RenderTooltipEvents.POSTEXT.invoker().onPost(tooltipStack, self, storedPostPos.x(), storedPostPos.y(), font, storedTooltipWidth, storedTooltipHeight, components, context.comparison(), context.index());
		}

		setIcebergTooltipStack(ItemStack.EMPTY);
		Tooltips.setCurrentRect(0, 0, 0, 0);
	}
}
