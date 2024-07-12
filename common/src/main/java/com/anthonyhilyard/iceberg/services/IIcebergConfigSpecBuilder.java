package com.anthonyhilyard.iceberg.services;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;

import com.anthonyhilyard.iceberg.config.IIcebergConfigSpec;



public interface IIcebergConfigSpecBuilder
{
	<T> Pair<T, IIcebergConfigSpec> finish(Function<IIcebergConfigSpecBuilder, T> consumer);
	void reset();

	public IIcebergConfigSpecBuilder comment(String comment);
	public IIcebergConfigSpecBuilder comment(String... comment);
	public IIcebergConfigSpecBuilder translation(String translationKey);
	public IIcebergConfigSpecBuilder push(String path);
	public IIcebergConfigSpecBuilder push(List<String> path);
	public IIcebergConfigSpecBuilder pop();

	public <T> Supplier<T> add(String path, T defaultValue);
	public <T> Supplier<T> add(String path, T defaultValue, Predicate<Object> validator);
	public <V extends Comparable<? super V>> Supplier<V> addInRange(String path, V defaultValue, V min, V max, Class<V> clazz);
	public <T> Supplier<T> addInList(String path, T defaultValue, Collection<? extends T> acceptableValues);
	public <T> Supplier<List<? extends T>> addList(String path, List<? extends T> defaultValue, Predicate<Object> elementValidator);
	public <T> Supplier<List<? extends T>> addListAllowEmpty(String path, List<? extends T> defaultValue, Predicate<Object> elementValidator);
	public <V extends Enum<V>> Supplier<V> addEnum(String path, V defaultValue);
	public <V extends Enum<V>> Supplier<V> addEnum(String path, V defaultValue, Predicate<Object> validator);
	public Supplier<Boolean> add(String path, boolean defaultValue);
	public Supplier<Double> addInRange(String path, double defaultValue, double min, double max);
	public Supplier<Integer> addInRange(String path, int defaultValue, int min, int max);
	public Supplier<Long> addInRange(String path, long defaultValue, long min, long max);
	public Supplier<Map<String, Object>> addSubconfig(String path, Map<String, Object> defaultValue, Predicate<Object> keyValidator, Predicate<Object> valueValidator);
}
