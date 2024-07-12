package com.anthonyhilyard.iceberg.fabric.config;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
class ValueSpec<T>
{
	private final String comment;
	private final String langKey;
	private final Range<T> range;
	private final Class<?> clazz;
	private final Supplier<T> supplier;
	private final Predicate<Object> validator;

	ValueSpec(Supplier<T> supplier, Predicate<Object> validator, FabricIcebergConfigSpec.BuilderContext context, List<String> path)
	{
		Objects.requireNonNull(supplier, "Default supplier can not be null!");
		Objects.requireNonNull(validator, "Validator can not be null!");

		this.comment = context.hasComment() ? context.buildComment(path) : null;
		this.langKey = context.getTranslationKey();
		this.range = (Range<T>)context.getRange();
		this.clazz = context.getClazz();
		this.supplier = supplier;
		this.validator = validator;
	}

	public String getComment() { return comment; }
	public String getTranslationKey() { return langKey; }
	public Range<T> getRange() { return this.range; }
	public Class<?> getClazz(){ return this.clazz; }
	public boolean test(T value) { return validator.test(value); }
	public T correct(T value) { return range == null ? getDefault() : range.correct(value, getDefault()); }

	public T getDefault() { return supplier.get(); }
}