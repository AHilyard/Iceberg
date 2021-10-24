package com.anthonyhilyard.iceberg.mixin;

import java.util.List;

import com.anthonyhilyard.iceberg.events.RenderTooltipEvents;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

@Mixin(Screen.class)
public class ScreenMixin extends AbstractContainerEventHandler
{
	@Shadow
	protected Font font = null;

	@Final
	@Shadow
	private final List<GuiEventListener> children = Lists.newArrayList();

	@SuppressWarnings("unchecked")
	@Inject(method = "renderTooltipInternal",
			at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;blitOffset:F", ordinal = 2, shift = Shift.AFTER),
			locals = LocalCapture.CAPTURE_FAILEXCEPTION)
	private void renderTooltipInternal(PoseStack poseStack, List<ClientTooltipComponent> components, int x, int y, CallbackInfo info, int tooltipWidth, int tooltipHeight, int postX, int postY)
	{
		if (!components.isEmpty())
		{
			if ((Screen)(Object)this instanceof AbstractContainerScreen)
			{
				ItemStack tooltipStack = ((AbstractContainerScreen<AbstractContainerMenu>)(Object)this).hoveredSlot.getItem();
				RenderTooltipEvents.POST.invoker().onPost(tooltipStack, components, poseStack, x, y, font, tooltipWidth, tooltipHeight, false);
			}
		}
	}

	@Override
	public List<? extends GuiEventListener> children()
	{
		return children;
	}
}
