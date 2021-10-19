package com.anthonyhilyard.iceberg.util;

import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSink;

public class StringRecomposer implements FormattedCharSink
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
