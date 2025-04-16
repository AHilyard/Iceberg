package com.anthonyhilyard.iceberg.neoforge.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mojang.datafixers.util.Either;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderTooltipEvent.GatherComponents;

@Mixin(value = ClientHooks.class, remap = false)
public class ClientHooksMixin
{
	@Redirect(method = "gatherTooltipComponentsFromElements", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/client/event/RenderTooltipEvent$GatherComponents;getMaxWidth()I", ordinal = 2))
	private static int adjustWrapping(GatherComponents instance, ItemStack stack, List<Either<FormattedText, TooltipComponent>> elements, int mouseX, int screenWidth, int screenHeight, Font fallbackFont)
	{
		Font font = ClientHooks.getTooltipFont(stack, fallbackFont);

		// If the first component has a negative width, apply that to the wrap width.
		if (instance.getTooltipElements().size() > 0 &&
			instance.getTooltipElements().get(0).right().orElse(null) instanceof ClientTooltipComponent clientComponent &&
			clientComponent.getWidth(font) < 0)
		{
			return instance.getMaxWidth() + clientComponent.getWidth(font);
		}

		return instance.getMaxWidth();
	}
}
