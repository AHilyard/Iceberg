package com.anthonyhilyard.iceberg.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;

@Mixin(GuiGraphics.class)
public interface GuiGraphicsInvoker
{
	@Invoker("renderTooltipInternal")
	void invokeRenderTooltipInternal(Font font, List<ClientTooltipComponent> list, int x, int y, ClientTooltipPositioner clientTooltipPositioner);
}
