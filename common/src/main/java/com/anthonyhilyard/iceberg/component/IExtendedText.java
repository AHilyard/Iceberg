package com.anthonyhilyard.iceberg.component;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public interface IExtendedText
{
	public enum TextAlignment
	{
		LEFT,
		CENTER,
		RIGHT
	}

	void setAlignment(TextAlignment alignment);
	TextAlignment getAlignment();

	default void setPadding(int padding)
	{
		setPadding(padding, padding, padding, padding);
	}

	default void setPadding(int left, int right)
	{
		setPadding(left, right, 0, 0);
	}

	void setPadding(int left, int right, int top, int bottom);
	int getLeftPadding();
	int getRightPadding();
	int getTopPadding();
	int getBottomPadding();

	public static class ExtendedTextDataStore
	{
		private static final Map<Object, TextAlignment> alignmentMap = Collections.synchronizedMap(new WeakHashMap<Object, TextAlignment>());
		private static final Map<Object, Integer> leftPaddingMap = Collections.synchronizedMap(new WeakHashMap<Object, Integer>());
		private static final Map<Object, Integer> rightPaddingMap = Collections.synchronizedMap(new WeakHashMap<Object, Integer>());

		public static void setAlignment(Object key, TextAlignment alignment)
		{
			alignmentMap.put(key, alignment);
		}

		public static TextAlignment get(Object key)
		{
			return alignmentMap.getOrDefault(key, TextAlignment.LEFT);
		}

		public static void setPadding(Object key, int left, int right)
		{
			leftPaddingMap.put(key, left);
			rightPaddingMap.put(key, right);
		}

		public static int getLeftPadding(Object key)
		{
			return leftPaddingMap.getOrDefault(key, 0);
		}

		public static int getRightPadding(Object key)
		{
			return rightPaddingMap.getOrDefault(key, 0);
		}
	}
}
