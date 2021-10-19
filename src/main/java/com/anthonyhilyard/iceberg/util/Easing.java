package com.anthonyhilyard.iceberg.util;

import net.minecraft.network.chat.TextColor;

/**
 * Helper functions for smooth easing/interpolation.
 */
public final class Easing
{
	public static enum EasingType
	{
		None, // Produces step-wise interpolation.
		Linear,
		Quad,
		Cubic
	}

	public static enum EasingDirection
	{
		In,
		Out,
		InOut
	}

	public static float Ease(float a, float b, float t)
	{
		return Ease(a, b, t, EasingType.Quad);
	}

	public static float Ease(float a, float b, float t, EasingType type)
	{
		return Ease(a, b, t, type, EasingDirection.InOut);
	}

	public static TextColor Ease(TextColor a, TextColor b, float t, EasingType type)
	{
		// Helper function to ease between TextColors.
		// Ease each component individually.
		int aV = a.getValue(), bV = b.getValue();
		int aA = (aV >> 24) & 0xFF, aR = (aV >> 16) & 0xFF, aG = (aV >> 8) & 0xFF, aB = (aV >> 0) & 0xFF,
			bA = (bV >> 24) & 0xFF, bR = (bV >> 16) & 0xFF, bG = (bV >> 8) & 0xFF, bB = (bV >> 0) & 0xFF;
		return TextColor.fromRgb((int)Ease(aA, bA, t, type) << 24 |
							 (int)Ease(aR, bR, t, type) << 16 |
							 (int)Ease(aG, bG, t, type) << 8  |
							 (int)Ease(aB, bB, t, type) << 0);
	}

	public static float Ease(float a, float b, float t, EasingType type, EasingDirection direction)
	{
		switch (type)
		{
			case None:
			default:
				return None(a, b, t);
			case Linear:
				return Linear(a, b, t);
			case Quad:
				return Quad(a, b, t, direction);
			case Cubic:
				return Cubic(a, b, t, direction);
		}
	}

	private static float None(float a, float b, float t)
	{
		if (t < 0.5f)
		{
			return a;
		}
		else
		{
			return b;
		}
	}

	private static float Linear(float a, float b, float t)
	{
		return a + (b - a) * t;
	}

	private static float Quad(float a, float b, float t, EasingDirection direction)
	{
		switch (direction)
		{
			case In:
				return a + (b - a) * t * t;
			case Out:
				return a + (b - a) * (1.0f - (1.0f - t) * (1.0f - t));
			case InOut:
			default:
			{
				t *= 2.0f;
				if (t < 1.0f)
				{
					return a + (b - a) * 0.5f * t * t;
				}
				else
				{
					t -= 2.0f;
					return a + (a - b) * 0.5f * (t * t - 2.0f);
				}
			}
		}
	}

	private static float Cubic(float a, float b, float t, EasingDirection direction)
	{
		switch (direction)
		{
			case In:
				return a + (b - a) * t * t * t;
			case Out:
				return a + (b - a) * (1.0f - (1.0f - t) * (1.0f - t) * (1.0f - t));
			case InOut:
			default:
			{
				t *= 2.0f;
				if (t < 1.0f)
				{
					return a + (b - a) * 0.5f * t * t * t;
				}
				else
				{
					t -= 2.0f;
					return a + (b - a) * 0.5f * (t * t * t + 2.0f);
				}
			}
		}
	}
}