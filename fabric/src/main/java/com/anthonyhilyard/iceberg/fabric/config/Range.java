package com.anthonyhilyard.iceberg.fabric.config;

import java.util.function.Predicate;

import com.anthonyhilyard.iceberg.Iceberg;

class Range<V> implements Predicate<Object>
{
	private final Class<? extends V> clazz;
	private final V min;
	private final V max;

	Range(Class<V> clazz, V min, V max)
	{
		this.clazz = clazz;
		this.min = min;
		this.max = max;
	}

	public Class<? extends V> getClazz() { return clazz; }
	public V getMin() { return min; }
	public V getMax() { return max; }

	private boolean isNumber(Object other) { return Number.class.isAssignableFrom(clazz) && other instanceof Number; }

	@Override
	@SuppressWarnings("unchecked")
	public boolean test(Object t)
	{
		if (isNumber(t))
		{
			Number n = (Number) t;
			boolean result = ((Number)min).doubleValue() <= n.doubleValue() && n.doubleValue() <= ((Number)max).doubleValue();
			if (!result)
			{
				Iceberg.LOGGER.debug("Range value {} is not within its bounds {}-{}", n.doubleValue(), ((Number)min).doubleValue(), ((Number)max).doubleValue());
			}
			return result;
		}

		if (!clazz.isInstance(t))
		{
			return false;
		}

		if (t instanceof Comparable c)
		{
			boolean result = c.compareTo(min) >= 0 && c.compareTo(max) <= 0;
			if (!result)
			{
				Iceberg.LOGGER.debug("Range value {} is not within its bounds {}-{}", c, min, max);
			}
			return result;
		}

		return false;
	}

	@SuppressWarnings("unchecked")
	public V correct(V value, V defaultValue)
	{
		if (isNumber(value))
		{
			Number n = (Number) value;
			return n.doubleValue() < ((Number)min).doubleValue() ? min : n.doubleValue() > ((Number)max).doubleValue() ? max : value;
		}

		if (!clazz.isInstance(value))
		{
			return defaultValue;
		}

		if (value instanceof Comparable c)
		{
			return c.compareTo(min) < 0 ? min : c.compareTo(max) > 0 ? max : value;
		}
		
		return defaultValue;
	}

	@Override
	public String toString() {
		if (clazz == Integer.class) {
			if (max.equals(Integer.MAX_VALUE)) {
				return "> " + min;
			} else if (min.equals(Integer.MIN_VALUE)) {
				return "< " + max;
			}
		} // TODO add more special cases?
		return min + " ~ " + max;
	}
}