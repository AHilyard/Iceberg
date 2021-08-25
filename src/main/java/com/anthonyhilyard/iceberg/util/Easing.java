package com.anthonyhilyard.iceberg.util;

/**
 * Helper functions for smooth easing/interpolation.  If you need linear, use net.minecraft.util.math.MathHelper.lerp instead.
 */
public final class Easing
{
	public static enum EasingType
	{
		None, // Produces step-wise interpolation.
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

	public static float Ease(float a, float b, float t, EasingType type, EasingDirection direction)
	{
		switch (type)
		{
			case None:
			default:
				return None(a, b, t);
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