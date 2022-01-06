package com.anthonyhilyard.iceberg.mixin;

import java.util.List;
import java.util.Optional;

import com.anthonyhilyard.iceberg.events.RenderTooltipEvents;
import com.anthonyhilyard.iceberg.events.RenderTooltipEvents.ColorExtResult;
import com.anthonyhilyard.iceberg.events.RenderTooltipEvents.ColorResult;
import com.anthonyhilyard.iceberg.events.RenderTooltipEvents.PreExtResult;
import com.anthonyhilyard.iceberg.util.Tooltips;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Matrix4f;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.AbstractContainerMenu;
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
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderTooltipInternal(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/util/List;II)V"),
			locals = LocalCapture.CAPTURE_FAILEXCEPTION)
	public void renderTooltip(PoseStack poseStack, List<Component> textComponents, Optional<TooltipComponent> itemComponent, int x, int y, CallbackInfo info, List<ClientTooltipComponent> components)
	{
		Screen self = (Screen)(Object)this;

		List<ClientTooltipComponent> newComponents = Tooltips.gatherTooltipComponents(tooltipStack, textComponents, itemComponent, x, self.width, self.height, null, font, -1);
		if (newComponents != null && !newComponents.isEmpty())
		{
			components.clear();
			components.addAll(newComponents);
		}
	}

	@SuppressWarnings({"unchecked", "deprecation"})
	@Inject(method = "renderTooltipInternal", at = @At(value = "HEAD"), cancellable = true)
	private void preRenderTooltipInternal(PoseStack poseStack, List<ClientTooltipComponent> components, int x, int y, CallbackInfo info)
	{
		PreExtResult eventResult = null;
		Screen self = (Screen)(Object)this;
		if (self instanceof AbstractContainerScreen)
		{
			if (!components.isEmpty())
			{
				Slot hoveredSlot = ((AbstractContainerScreen<AbstractContainerMenu>)self).hoveredSlot;
				if (hoveredSlot != null)
				{
					ItemStack tooltipStack = hoveredSlot.getItem();
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
		}
	}

	@SuppressWarnings("unchecked")
	@Redirect(method = "renderTooltipInternal", at = @At(value = "INVOKE",
	target = "Lnet/minecraft/client/gui/screens/Screen;fillGradient(Lcom/mojang/math/Matrix4f;Lcom/mojang/blaze3d/vertex/BufferBuilder;IIIIIII)V"))
	private void fillGradientProxy(Matrix4f matrix4f, BufferBuilder bufferBuilder, int left, int top, int right, int bottom, int zIndex, int colorStart, int colorEnd)
	{
		Screen self = (Screen)(Object)this;
		ItemStack tooltipStack = ItemStack.EMPTY;
		if (self instanceof AbstractContainerScreen)
		{
			Slot hoveredSlot = ((AbstractContainerScreen<AbstractContainerMenu>)self).hoveredSlot;
			
			if (hoveredSlot != null)
			{
				tooltipStack = hoveredSlot.getItem();
			}
		}
		if (tooltipStack == ItemStack.EMPTY)
		{
			// Do standard functionality if this isn't a container screen.
			Screen.fillGradient(matrix4f, bufferBuilder, left, top, right, bottom, zIndex, colorStart, colorEnd);
		}
		else
		{
			// Otherwise do nothing to disable the default calls.
		}
	}

	@SuppressWarnings({"unchecked", "deprecation"})
	@Inject(method = "renderTooltipInternal", at = @At(value = "INVOKE",
	target = "Lnet/minecraft/client/gui/screens/Screen;fillGradient(Lcom/mojang/math/Matrix4f;Lcom/mojang/blaze3d/vertex/BufferBuilder;IIIIIII)V", ordinal = 0, shift = Shift.BEFORE),
	locals = LocalCapture.CAPTURE_FAILEXCEPTION)
	private void preFillGradient(PoseStack poseStack, List<ClientTooltipComponent> components, int x, int y, CallbackInfo info,
		int __, int ___, int left, int top, int width, int height, int background, int borderStart, int borderEnd,
		int zIndex, float blitOffset, Tesselator tesselator, BufferBuilder bufferBuilder, Matrix4f matrix4f)
	{
		Screen self = (Screen)(Object)this;
		ItemStack tooltipStack = ItemStack.EMPTY;
		if (self instanceof AbstractContainerScreen)
		{
			Slot hoveredSlot = ((AbstractContainerScreen<AbstractContainerMenu>)self).hoveredSlot;
			
			if (hoveredSlot != null)
			{
				tooltipStack = hoveredSlot.getItem();
			}
		}

		if (tooltipStack != ItemStack.EMPTY)
		{
			int backgroundEnd = background;

			// Do colors now, sure why not.
			ColorExtResult result = RenderTooltipEvents.COLOREXT.invoker().onColor(tooltipStack, components, poseStack, x, y, font, background, backgroundEnd, borderStart, borderEnd, false, 0);
			if (result != null)
			{
				background = result.backgroundStart();
				backgroundEnd = result.backgroundEnd();
				borderStart = result.borderStart();
				borderEnd = result.borderEnd();
			}

			// Fire a colors event as well for compatibility.
			ColorResult colorResult = RenderTooltipEvents.COLOR.invoker().onColor(tooltipStack, components, poseStack, x, y, font, background, borderStart, borderEnd, false);
			if (colorResult != null)
			{
				background = colorResult.background();
				borderStart = colorResult.borderStart();
				borderEnd = colorResult.borderEnd();
			}

			Screen.fillGradient(matrix4f, bufferBuilder, left - 3, top - 4, left + width + 3, top - 3, zIndex, background, background);
			Screen.fillGradient(matrix4f, bufferBuilder, left - 3, top + height + 3, left + width + 3, top + height + 4, zIndex, backgroundEnd, backgroundEnd);
			Screen.fillGradient(matrix4f, bufferBuilder, left - 3, top - 3, left + width + 3, top + height + 3, zIndex, background, backgroundEnd);
			Screen.fillGradient(matrix4f, bufferBuilder, left - 4, top - 3, left - 3, top + height + 3, zIndex, background, backgroundEnd);
			Screen.fillGradient(matrix4f, bufferBuilder, left + width + 3, top - 3, left + width + 4, top + height + 3, zIndex, background, backgroundEnd);
			Screen.fillGradient(matrix4f, bufferBuilder, left - 3, top - 3 + 1, left - 3 + 1, top + height + 3 - 1, zIndex, borderStart, borderEnd);
			Screen.fillGradient(matrix4f, bufferBuilder, left + width + 2, top - 3 + 1, left + width + 3, top + height + 3 - 1, zIndex, borderStart, borderEnd);
			Screen.fillGradient(matrix4f, bufferBuilder, left - 3, top - 3, left + width + 3, top - 3 + 1, zIndex, borderStart, borderStart);
			Screen.fillGradient(matrix4f, bufferBuilder, left - 3, top + height + 2, left + width + 3, top + height + 3, zIndex, borderEnd, borderEnd);
		}
	}

	@SuppressWarnings({"unchecked", "deprecation"})
	@Inject(method = "renderTooltipInternal", at = @At(value = "TAIL"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
	private void renderTooltipInternal(PoseStack poseStack, List<ClientTooltipComponent> components, int x, int y, CallbackInfo info, int tooltipWidth, int tooltipHeight, int postX, int postY)
	{
		if ((Screen)(Object)this instanceof AbstractContainerScreen)
		{
			if (!components.isEmpty())
			{
				Slot hoveredSlot = ((AbstractContainerScreen<AbstractContainerMenu>)(Object)this).hoveredSlot;
				if (hoveredSlot != null)
				{
					ItemStack tooltipStack = hoveredSlot.getItem();
					RenderTooltipEvents.POSTEXT.invoker().onPost(tooltipStack, components, poseStack, postX, postY, font, tooltipWidth, tooltipHeight, false, 0);
					RenderTooltipEvents.POST.invoker().onPost(tooltipStack, components, poseStack, postX, postY, font, tooltipWidth, tooltipHeight, false);
				}
			}
		}
	}

	@Override
	public List<? extends GuiEventListener> children()
	{
		return children;
	}
}
