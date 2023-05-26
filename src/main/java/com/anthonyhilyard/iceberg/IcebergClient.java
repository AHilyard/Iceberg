package com.anthonyhilyard.iceberg;

import com.anthonyhilyard.iceberg.util.Tooltips.TitleBreakComponent;
import com.mojang.datafixers.util.Either;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent.GatherComponents;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = Loader.MODID, bus = Bus.FORGE, value = Dist.CLIENT)
public class IcebergClient
{
	public IcebergClient()
	{
	}

	public void onClientSetup(FMLClientSetupEvent event)
	{
	}

	// https://github.com/Creators-of-Create/Create/blob/mc1.18/dev/src/main/resources/assets/create/lang/default/interface.json
	private static final List<String> CREATE_TOOLTIPS = List.of(
			"create.gui.goggles",
			"create.gui.gauge.info_header"
	);

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onGatherComponentsEventEnd(GatherComponents event)
	{
		if (event.getTooltipElements().size() > 1)
		{
			List<Either<FormattedText, TooltipComponent>> tooltipElements = event.getTooltipElements();
			Either<FormattedText, TooltipComponent> element = tooltipElements.get(0);

			if (element.left().isPresent())
			{
				String text = element.left().get().getString();

				Map<String, String> languageData = Language.getInstance().getLanguageData();
				Set<String> keys = languageData.keySet().stream().filter(key -> {
					for (String tooltip : CREATE_TOOLTIPS) {
						if (key.startsWith(tooltip))
						{
							return true;
						}
					}

					return key.startsWith("create.tooltip") && key.endsWith(".header");
				}).collect(Collectors.toSet());

				for (String toCheck : keys) {
					if (text.contains(languageData.get(toCheck)))
					{
						return;
					}
				}
			}

			// Insert a title break component after the first formattedText component.
			for (int i = 0; i < event.getTooltipElements().size(); i++)
			{
				if (event.getTooltipElements().get(i).left().isPresent())
				{
					event.getTooltipElements().add(i + 1, Either.<FormattedText, TooltipComponent>right(new TitleBreakComponent()));
					break;
				}
			}
		}
	}

	// @SubscribeEvent
	// public static void onTooltipPre(RenderTooltipEvent.Pre event)
	// {
	// 	Loader.LOGGER.info("tooltip pre");
	// }

	// @SubscribeEvent
	// public static void onTooltipColor(RenderTooltipEvent.Color event)
	// {
	// 	Loader.LOGGER.info("tooltip color");
	// }

	// @SubscribeEvent
	// public static void onTooltipPost(RenderTooltipExtEvent.Post event)
	// {
	// 	Loader.LOGGER.info("tooltip post");
	// }
}
