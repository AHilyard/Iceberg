package com.anthonyhilyard.iceberg.util;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSink;

public class StringRecomposer
{
	public static List<FormattedText> recompose(List<ClientTooltipComponent> components)
	{
		List<FormattedText> recomposedLines = new ArrayList<>();
		for (ClientTooltipComponent component : components)
		{
			if (component instanceof ClientTextTooltip)
			{
				RecomposerSink recomposer = new RecomposerSink();
				((ClientTextTooltip)component).text.accept(recomposer);
				recomposedLines.add(recomposer.getFormattedText());
			}
		}
		return recomposedLines;
	}
	
	private static class RecomposerSink implements FormattedCharSink
	{
		private StringBuilder builder = new StringBuilder();
		private MutableComponent text = new TextComponent("").withStyle(Style.EMPTY);

		@Override
		public boolean accept(int index, Style style, int charCode)
		{
			builder.append(Character.toChars(charCode));

			if (!style.equals(text.getStyle()))
			{
				text.append(new TextComponent(builder.toString()).withStyle(style));
				builder.setLength(0);
			}
			return true;
		}

		public FormattedText getFormattedText()
		{
			text.append(new TextComponent(builder.toString()).withStyle(text.getStyle()));
			return text;
		}
	}
}
