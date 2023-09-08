package com.anthonyhilyard.iceberg.mixin;

import java.util.List;

import com.anthonyhilyard.iceberg.events.RenderTooltipExtEvent;

import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Surrogate;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin
{
	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow(remap = false)
	private ItemStack tooltipStack = ItemStack.EMPTY;

	@Inject(method = "renderTooltipInternal", at = @At(value = "HEAD"))
	private void icebergRenderTooltipInternalHead(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo info)
	{
		// If the tooltip stack is empty, try to get the stack from the slot under the mouse.
		// This is needed for the creative inventory screen, which doesn't set the tooltip stack.
		if (tooltipStack.isEmpty() && minecraft.screen != null && minecraft.screen instanceof AbstractContainerScreen<?> containerScreen && containerScreen.getSlotUnderMouse() != null)
		{
			tooltipStack = containerScreen.getSlotUnderMouse().getItem();
		}
	}

	@Inject(method = "renderTooltipInternal", at = @At(value = "TAIL"))
	private void icebergRenderTooltipInternalTail(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo info)
	{
		tooltipStack = ItemStack.EMPTY;
	}

	@Inject(method = "renderTooltipInternal",
			at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", shift = Shift.AFTER), locals = LocalCapture.CAPTURE_FAILSOFT, require = 0)
	private void icebergRenderTooltipInternalPost(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo info, RenderTooltipEvent.Pre preEvent, int tooltipWidth, int tooltipHeight, int tooltipWidth2, int tooltipHeight2, Vector2ic postPos)
	{
		if (!components.isEmpty())
		{
			GuiGraphics self = (GuiGraphics)(Object)this;
			MinecraftForge.EVENT_BUS.post(new RenderTooltipExtEvent.Post(preEvent.getItemStack(), self, postPos.x(), postPos.y(), preEvent.getFont(), tooltipWidth2, tooltipHeight2, components, false));
		}
	}

	@Surrogate
	private void icebergRenderTooltipInternalPost(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo info, Object preEventObj, int tooltipWidth, int tooltipHeight, int tooltipWidth2, int tooltipHeight2, Vector2ic postPos)
	{
		RenderTooltipEvent.Pre preEvent = (RenderTooltipEvent.Pre)preEventObj;
		icebergRenderTooltipInternalPost(font, components, x, y, positioner, info, preEvent, tooltipWidth, tooltipHeight, tooltipWidth2, tooltipHeight2, postPos);
	}
}
