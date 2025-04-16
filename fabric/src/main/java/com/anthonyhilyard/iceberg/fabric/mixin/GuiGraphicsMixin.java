package com.anthonyhilyard.iceberg.fabric.mixin;

import java.util.List;
import java.util.Optional;

import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents;
import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents.ColorExtResult;
import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents.PreExtResult;
import com.anthonyhilyard.iceberg.services.Services;
import com.anthonyhilyard.iceberg.util.Tooltips;
import com.anthonyhilyard.iceberg.util.Tooltips.TooltipRenderContext;
import com.mojang.blaze3d.systems.RenderSystem;

import org.joml.Matrix4fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin
{
	@Shadow
	@Final
	private Minecraft minecraft;

	@Unique
	private static ItemStack icebergTooltipStack = ItemStack.EMPTY;

	@Unique
	private int xChange = 0;
	
	@Unique
	private int yChange = 0;

	@Shadow
	private void renderTooltipInternal(Font font, List<ClientTooltipComponent> list, int i, int j, ClientTooltipPositioner clientTooltipPositioner) {}

	@Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V", at = @At(value = "HEAD"))
	protected void renderTooltipHead(Font font, ItemStack itemStack, int x, int y, CallbackInfo info)
	{
		icebergTooltipStack = itemStack;
	}

	@Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V", at = @At(value = "TAIL"))
	protected void renderTooltipTail(Font font, ItemStack itemStack, int x, int y, CallbackInfo info)
	{
		icebergTooltipStack = ItemStack.EMPTY;
	}

	@Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;II)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderTooltipInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;)V",
			shift = Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
	public void renderTooltip(Font font, List<Component> textComponents, Optional<TooltipComponent> itemComponent, int x, int y, CallbackInfo info, List<ClientTooltipComponent> components)
	{
		Screen currentScreen = minecraft.screen;

		if (currentScreen != null && currentScreen instanceof AbstractContainerScreen<?> containerScreen)
		{
			Slot hoveredSlot = containerScreen.hoveredSlot;

			// If the tooltip stack is empty, try to get the stack from the slot under the mouse.
			// This is needed for the creative inventory screen, which doesn't set the tooltip stack.
			if (icebergTooltipStack.isEmpty() && hoveredSlot != null)
			{
				icebergTooltipStack = hoveredSlot.getItem();
			}
		}

		int screenWidth = currentScreen != null ? currentScreen.width : minecraft.getWindow().getGuiScaledWidth();
		int screenHeight = currentScreen != null ? currentScreen.height : minecraft.getWindow().getGuiScaledHeight();

		List<ClientTooltipComponent> newComponents = Tooltips.gatherTooltipComponents(icebergTooltipStack, textComponents, itemComponent, x, screenWidth, screenHeight, null, font, -1);
		if (newComponents != null && !newComponents.isEmpty())
		{
			components.clear();
			components.addAll(newComponents);
		}
	}


	@Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;II)V", at = @At(value = "HEAD"))
	public void renderNonItemTooltip(Font font, Component components, int x, int y, CallbackInfo info)
	{
		// Not an item tooltip, so clear the stack.
		icebergTooltipStack = ItemStack.EMPTY;
	}

	@Inject(method = "renderTooltipInternal", at = @At(value = "HEAD"), cancellable = true)
	private void preRenderTooltipInternal(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo info)
	{
		GuiGraphics self = (GuiGraphics)(Object)this;
		Screen currentScreen = minecraft.screen;

		int width = minecraft.getWindow().getGuiScaledWidth();
		int height = minecraft.getWindow().getGuiScaledHeight();
		
		if (currentScreen != null)
		{
			width = currentScreen.width;
			height = currentScreen.height;
		}

		if (!components.isEmpty())
		{
			PreExtResult eventResult = null;
			InteractionResult result = InteractionResult.PASS;

			TooltipRenderContext context = Tooltips.getCurrentRenderContext();

			eventResult = RenderTooltipEvents.PREEXT.invoker().onPre(icebergTooltipStack, self, x, y, width, height, font, components, positioner, context.comparison(), context.index());
			result = eventResult.result();

			if (result != InteractionResult.PASS)
			{
				info.cancel();
			}
		}
	}

	@Inject(method = "renderTooltipInternal",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawManaged(Ljava/lang/Runnable;)V",
			ordinal = 0, shift = Shift.BEFORE))
	private void preFillGradient(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo info)
	{
		GuiGraphics self = (GuiGraphics)(Object)this;
		Screen currentScreen = minecraft.screen;
		ItemStack containerStack = ItemStack.EMPTY;
		// if (currentScreen != null && currentScreen instanceof AbstractContainerScreen<?> containerScreen)
		// {
		// 	Slot hoveredSlot = containerScreen.hoveredSlot;
		// 	if (hoveredSlot != null)
		// 	{
		// 		containerStack = hoveredSlot.getItem();
		// 	}
		// }

		if (containerStack.isEmpty())
		{
			containerStack = icebergTooltipStack;
		}

		if (!containerStack.isEmpty())
		{
			int backgroundStart = TooltipRenderUtil.BACKGROUND_COLOR;
			int backgroundEnd = backgroundStart;
			int borderStart = TooltipRenderUtil.BORDER_COLOR_TOP;
			int borderEnd = TooltipRenderUtil.BORDER_COLOR_BOTTOM;

			TooltipRenderContext context = Tooltips.getCurrentRenderContext();

			// Do colors now, sure why not.
			ColorExtResult result = RenderTooltipEvents.COLOREXT.invoker().onColor(containerStack, self, x, y, font, backgroundStart, backgroundEnd, borderStart, borderEnd, components, context.comparison(), context.index());
			if (result != null)
			{
				backgroundStart = result.backgroundStart();
				backgroundEnd = result.backgroundEnd();
				borderStart = result.borderStart();
				borderEnd = result.borderEnd();
			}

			Tooltips.currentColors = new Tooltips.TooltipColors(backgroundStart, backgroundEnd, borderStart, borderEnd);
		}
		else
		{
			Tooltips.currentColors = Tooltips.DEFAULT_COLORS;
		}
	}

	@Inject(method = "renderTooltipInternal", at = @At(value = "TAIL"))
	private void renderTooltipInternalTail(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo info)
	{
		if (Services.getPlatformHelper().isModLoaded("andromeda") && minecraft.player != null && icebergTooltipStack == minecraft.player.getMainHandItem())
		{
			Matrix4fStack poseStack = RenderSystem.getModelViewStack();
			poseStack.translate(-xChange, -yChange, 0);
			RenderSystem.applyModelViewMatrix();
		}

		xChange = 0;
		yChange = 0;
	}
}
