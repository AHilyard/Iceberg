package com.anthonyhilyard.iceberg.mixin;

import java.util.List;

import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents;
import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents.PreExtResult;

import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

	@Shadow @Final private Minecraft minecraft;
	@Shadow public abstract int guiWidth();
	@Shadow public abstract int guiHeight();

	@Unique private static ItemStack icebergTooltipStack = ItemStack.EMPTY;
	@Unique private int xChange = 0;
	@Unique private int yChange = 0;

	@Inject(method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V", at = @At("HEAD"))
	protected void captureTooltipStack(Font font, ItemStack itemStack, int x, int y, CallbackInfo info) {
		icebergTooltipStack = itemStack;
	}

	@Inject(method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;II)V", at = @At("HEAD"))
	public void clearTooltipStack(Font font, Component component, int x, int y, CallbackInfo info) {
		icebergTooltipStack = ItemStack.EMPTY;
	}

	@Inject(method = "setTooltipForNextFrameInternal", at = @At("HEAD"))
	public void modifyGatheredComponents(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, Identifier resource, boolean bl, CallbackInfo info) {
		Screen currentScreen = minecraft.screen;
		if (currentScreen instanceof AbstractContainerScreen<?> containerScreen && ((AbstractContainerScreenAccessor)containerScreen).getHoveredSlot() != null) {
			if (icebergTooltipStack.isEmpty()) icebergTooltipStack = ((AbstractContainerScreenAccessor)containerScreen).getHoveredSlot().getItem();
		}
	}

	@Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;)V", at = @At("HEAD"), cancellable = true)
	private void preRenderTooltip(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, Identifier resource, CallbackInfo info) {
		GuiGraphics self = (GuiGraphics)(Object)this;
		int width = minecraft.getWindow().getGuiScaledWidth();
		int height = minecraft.getWindow().getGuiScaledHeight();

		if (minecraft.screen != null) {
			width = minecraft.screen.width;
			height = minecraft.screen.height;
		}

		if (!components.isEmpty() && !icebergTooltipStack.isEmpty()) {
			PreExtResult eventResult = RenderTooltipEvents.PREEXT.invoker().onPre(icebergTooltipStack, self, x, y, width, height, font, components, positioner, false, 0);
			if (eventResult.result() != InteractionResult.PASS) {
				info.cancel();
			}
		}
	}

	@Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;)V", at = @At("TAIL"))
	private void postRenderTooltip(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, Identifier resource, CallbackInfo info) {
		GuiGraphics self = (GuiGraphics)(Object)this;
		ItemStack containerStack = ItemStack.EMPTY;

		if (minecraft.screen instanceof AbstractContainerScreen<?> containerScreen && ((AbstractContainerScreenAccessor)containerScreen).getHoveredSlot() != null) {
			containerStack = ((AbstractContainerScreenAccessor)containerScreen).getHoveredSlot().getItem();
		}
		if (containerStack.isEmpty()) containerStack = icebergTooltipStack;

		if (!containerStack.isEmpty() && !components.isEmpty()) {
			int tooltipWidth = 0;
			int tooltipHeight = components.size() == 1 ? -2 : 0;
			for (ClientTooltipComponent c : components) {
				int w = c.getWidth(font);
				if (w > tooltipWidth) tooltipWidth = w;
				tooltipHeight += c.getHeight(font);
			}

			Vector2ic pos = positioner.positionTooltip(this.guiWidth(), this.guiHeight(), x, y, tooltipWidth, tooltipHeight);

			RenderTooltipEvents.POSTEXT.invoker().onPost(containerStack, self, pos.x(), pos.y(), font, tooltipWidth, tooltipHeight, components, false, 0);
		}

		icebergTooltipStack = ItemStack.EMPTY;
		xChange = 0;
		yChange = 0;
	}
}