package com.anthonyhilyard.iceberg.compat;

import java.util.List;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.render.EmiTooltipMetadata;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.screen.EmiScreenManager;
import dev.emi.emi.screen.EmiScreenManager.SidebarPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;

public class EMIHandler
{
	public static ItemStack getTooltipStack(List<ClientTooltipComponent> components)
	{
		ItemStack result = ItemStack.EMPTY;

		Minecraft minecraft = Minecraft.getInstance();
		Screen currentScreen = minecraft.screen;
		SidebarPanel panel = EmiScreenManager.getHoveredPanel(EmiScreenManager.lastMouseX, EmiScreenManager.lastMouseY);

		if ((currentScreen != null && currentScreen.getClass().getName().startsWith("dev.emi.emi")) || panel != null)
		{
			EmiIngredient ingredient = EmiApi.getHoveredStack(true).getStack();

			if (ingredient instanceof EmiStack stack)
			{
				result = stack.getItemStack();
			}

			if (result.isEmpty())
			{
				EmiTooltipMetadata metadata = EmiTooltipMetadata.of(components);
				if (metadata.getStack() instanceof EmiStack stack)
				{
					result = stack.getItemStack();
				}
			}
		}

		return result;
		
	}
}
