package com.anthonyhilyard.iceberg.mixin;

import java.util.List;
import java.util.Optional;

import com.anthonyhilyard.iceberg.events.RenderTooltipEvents;
import com.anthonyhilyard.iceberg.events.RenderTooltipEvents.ColorExtResult;
import com.anthonyhilyard.iceberg.events.RenderTooltipEvents.ColorResult;
import com.anthonyhilyard.iceberg.events.RenderTooltipEvents.PreExtResult;
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
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

@Mixin(Screen.class)
public class ScreenMixin extends AbstractContainerEventHandler
{
	@Shadow
	protected Font font = null;

	@Shadow
	private List<GuiEventListener> children = Lists.newArrayList();

	private ItemStack tooltipStack = ItemStack.EMPTY;

	@Inject(method = "renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemStack;II)V", at = @At(value = "HEAD"))
	protected void renderTooltipHead(PoseStack poseStack, ItemStack itemStack, int x, int y, CallbackInfo info)
	{
		tooltipStack = itemStack;
	}

	@Inject(method = "renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemStack;II)V", at = @At(value = "TAIL"))
	protected void renderTooltipTail(PoseStack poseStack, ItemStack itemStack, int x, int y, CallbackInfo info)
	{
		tooltipStack = ItemStack.EMPTY;
	}

	@Inject(method = "renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/util/List;Ljava/util/Optional;II)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderTooltipInternal(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;)V",
			shift = Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
	public void renderTooltip(PoseStack poseStack, List<Component> textComponents, Optional<TooltipComponent> itemComponent, int x, int y, CallbackInfo info, List<ClientTooltipComponent> components)
	{
		Screen self = (Screen)(Object)this;

		if (self instanceof AbstractContainerScreen<?> containerScreen)
		{
			Slot hoveredSlot = containerScreen.hoveredSlot;

			// If the tooltip stack is empty, try to get the stack from the slot under the mouse.
			// This is needed for the creative inventory screen, which doesn't set the tooltip stack.
			if (tooltipStack.isEmpty() && hoveredSlot != null)
			{
				tooltipStack = hoveredSlot.getItem();
			}
		}

		List<ClientTooltipComponent> newComponents = Tooltips.gatherTooltipComponents(tooltipStack, textComponents, itemComponent, x, self.width, self.height, null, font, -1);
		if (newComponents != null && !newComponents.isEmpty())
		{
			components.clear();
			components.addAll(newComponents);
		}
	}

	@SuppressWarnings("deprecation")
	@Inject(method = "renderTooltipInternal", at = @At(value = "HEAD"), cancellable = true)
	private void preRenderTooltipInternal(PoseStack poseStack, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo info)
	{
		Screen self = (Screen)(Object)this;

		if (!components.isEmpty())
		{
			PreExtResult eventResult = null;
			InteractionResult result = InteractionResult.PASS;

			eventResult = RenderTooltipEvents.PREEXT.invoker().onPre(tooltipStack, components, poseStack, x, y, self.width, self.height, font, false, 0);
			result = eventResult.result();

			if (result != InteractionResult.PASS)
			{
				info.cancel();
			}

			// Fire a pre event as well for compatibility.
			result = RenderTooltipEvents.PRE.invoker().onPre(tooltipStack, components, poseStack, x, y, self.width, self.height, -1, font, false);
			if (result != InteractionResult.PASS)
			{
				info.cancel();
			}
		}
	}

	@SuppressWarnings("deprecation")
	@Inject(method = "renderTooltipInternal",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;renderTooltipBackground(Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil$BlitPainter;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/BufferBuilder;IIIII)V",
			ordinal = 0, shift = Shift.BEFORE))
	private void preFillGradient(PoseStack poseStack, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo info)
	{
		Screen self = (Screen)(Object)this;
		ItemStack containerStack = ItemStack.EMPTY;
		if (self instanceof AbstractContainerScreen<?> containerScreen)
		{
			Slot hoveredSlot = containerScreen.hoveredSlot;
			if (hoveredSlot != null)
			{
				containerStack = hoveredSlot.getItem();
			}
		}

		if (containerStack.isEmpty())
		{
			containerStack = tooltipStack;
		}

		if (!containerStack.isEmpty())
		{
			int background = TooltipRenderUtil.BACKGROUND_COLOR;
			int backgroundEnd = background;
			int borderStart = TooltipRenderUtil.BORDER_COLOR_TOP;
			int borderEnd = TooltipRenderUtil.BORDER_COLOR_BOTTOM;

			// Do colors now, sure why not.
			ColorExtResult result = RenderTooltipEvents.COLOREXT.invoker().onColor(containerStack, components, poseStack, x, y, font, background, backgroundEnd, borderStart, borderEnd, false, 0);
			if (result != null)
			{
				background = result.backgroundStart();
				backgroundEnd = result.backgroundEnd();
				borderStart = result.borderStart();
				borderEnd = result.borderEnd();
			}

			// Fire a colors event as well for compatibility.
			ColorResult colorResult = RenderTooltipEvents.COLOR.invoker().onColor(containerStack, components, poseStack, x, y, font, background, borderStart, borderEnd, false);
			if (colorResult != null)
			{
				background = colorResult.background();
				borderStart = colorResult.borderStart();
				borderEnd = colorResult.borderEnd();
			}

			Tooltips.currentColors = new Tooltips.TooltipColors(TextColor.fromRgb(background), TextColor.fromRgb(backgroundEnd), TextColor.fromRgb(borderStart), TextColor.fromRgb(borderEnd));
		}
	}

	@SuppressWarnings("deprecation")
	@Inject(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", shift = Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILEXCEPTION, cancellable = true)
	private void renderTooltipInternal(PoseStack poseStack, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo info, int tooltipWidth, int tooltipHeight, int tooltipWidth2, int tooltipHeight2, Vector2ic postPos)
	{
		Screen self = (Screen)(Object)this;
		ItemStack containerStack = ItemStack.EMPTY;
		if (self instanceof AbstractContainerScreen<?> containerScreen)
		{
			Slot hoveredSlot = containerScreen.hoveredSlot;
			if (hoveredSlot != null)
			{
				containerStack = hoveredSlot.getItem();
			}
		}

		if (containerStack.isEmpty())
		{
			containerStack = tooltipStack;
		}

		poseStack.popPose();

		if (!containerStack.isEmpty() && !components.isEmpty())
		{
			RenderTooltipEvents.POSTEXT.invoker().onPost(containerStack, components, poseStack, postPos.x(), postPos.y(), font, tooltipWidth2, tooltipHeight2, false, 0);
			RenderTooltipEvents.POST.invoker().onPost(containerStack, components, poseStack, postPos.x(), postPos.y(), font, tooltipWidth2, tooltipHeight2, false);
		}

		tooltipStack = ItemStack.EMPTY;
		info.cancel();
	}

	@Override
	public List<? extends GuiEventListener> children()
	{
		return children;
	}
}
