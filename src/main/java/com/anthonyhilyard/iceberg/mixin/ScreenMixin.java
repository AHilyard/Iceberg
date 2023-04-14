package com.anthonyhilyard.iceberg.mixin;

import java.util.List;

import com.anthonyhilyard.iceberg.events.RenderTooltipExtEvent;
import com.anthonyhilyard.iceberg.util.Tooltips;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;

import org.joml.Vector2ic;
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
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;

@Mixin(Screen.class)
public class ScreenMixin extends AbstractContainerEventHandler
{
	@Shadow
	protected Font font = null;

	@Shadow(remap = false)
	private Font tooltipFont = null;

	@Shadow(remap = false)
	private ItemStack tooltipStack = ItemStack.EMPTY;

	@Shadow
	private final List<GuiEventListener> children = Lists.newArrayList();

	@Inject(method = "renderTooltipInternal", at = @At(value = "HEAD"))
	private void icebergRenderTooltipInternalHead(PoseStack poseStack, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo info)
	{
		// If the tooltip stack is empty, try to get the stack from the slot under the mouse.
		// This is needed for the creative inventory screen, which doesn't set the tooltip stack.
		Screen self = (Screen) (Object) this;
		if (tooltipStack.isEmpty() && self instanceof AbstractContainerScreen<?> containerScreen && containerScreen.getSlotUnderMouse() != null)
		{
			tooltipStack = containerScreen.getSlotUnderMouse().getItem();
		}
	}

	@Inject(method = "renderTooltipInternal", at = @At(value = "TAIL"))
	private void icebergRenderTooltipInternalTail(PoseStack poseStack, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo info)
	{
		tooltipStack = ItemStack.EMPTY;
	}

	@Inject(method = "renderTooltipInternal",
			at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", shift = Shift.AFTER), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
	private void icebergRenderTooltipInternalPost(PoseStack poseStack, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo info, RenderTooltipEvent.Pre preEvent, int tooltipWidth, int tooltipHeight, Vector2ic postPos)
	{
		if (!components.isEmpty())
		{
			MinecraftForge.EVENT_BUS.post(new RenderTooltipExtEvent.Post(preEvent.getItemStack(), poseStack, postPos.x(), postPos.y(), preEvent.getFont(), tooltipWidth, tooltipHeight, components, false));
		}
	}

	@Inject(method = "renderTooltipInternal",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;renderTooltipBackground(Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil$BlitPainter;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/BufferBuilder;IIIII)V",
			ordinal = 0, shift = Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
	private void renderTooltipInternalColor(PoseStack poseStack, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo info, RenderTooltipEvent.Pre preEvent)
	{
		RenderTooltipExtEvent.Color colorEvent = new RenderTooltipExtEvent.Color(preEvent.getItemStack(), poseStack, x, y, preEvent.getFont(), TooltipRenderUtil.BACKGROUND_COLOR, TooltipRenderUtil.BACKGROUND_COLOR, TooltipRenderUtil.BORDER_COLOR_TOP, TooltipRenderUtil.BORDER_COLOR_BOTTOM, components, false, 0);
		MinecraftForge.EVENT_BUS.post(colorEvent);

		Tooltips.currentColors = new Tooltips.TooltipColors(TextColor.fromRgb(colorEvent.getBackgroundStart()), TextColor.fromRgb(colorEvent.getBackgroundEnd()), TextColor.fromRgb(colorEvent.getBorderStart()), TextColor.fromRgb(colorEvent.getBorderEnd()));
	}

	@Override
	public List<? extends GuiEventListener> children()
	{
		return children;
	}
}
