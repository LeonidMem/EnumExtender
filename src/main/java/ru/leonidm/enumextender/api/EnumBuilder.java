package ru.leonidm.enumextender.api;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder for new enumeration instances
 *
 * @param <E> enum type
 * @author LeonidM
 */
public final class EnumBuilder<E extends Enum<E>> {

    private final Map<String, Object> fieldValues = new HashMap<>();
    private final String enumName;
    private final EnumFactory<E> enumFactory;
    private int ordinal = -1;
    private EnumResult<E> enumResult;

    public EnumBuilder(@NotNull String enumName, @NotNull EnumFactory<E> enumFactory) {
        this.enumName = enumName;
        this.enumFactory = enumFactory;
    }

    /**
     * When new enumeration will be created, {@link EnumExtender} will set field.
     * provided name and provided value
     *
     * @param fieldName  name of field
     * @param fieldValue value of this field
     * @return this
     */
    @NotNull
    @Contract("_, _ -> this")
    public EnumBuilder<E> setField(@NotNull String fieldName, @Nullable Object fieldValue) {
        fieldValues.put(fieldName, fieldValue);
        return this;
    }

    @NotNull
    @Contract("_ -> this")
    public EnumBuilder<E> setFields(@NotNull Map<String, Object> fieldValues) {
        this.fieldValues.putAll(fieldValues);
        return this;
    }

    @NotNull
    public EnumBuilder<E> insertBefore(@NotNull E insertBefore) {
        this.ordinal = insertBefore.ordinal();
        return this;
    }

    @NotNull
    public EnumResult<E> create() {
        if (enumResult == null) {
            enumResult = enumFactory.apply(enumName, fieldValues, ordinal);
        }

        return enumResult;
    }

    @FunctionalInterface
    public interface EnumFactory<E extends Enum<E>> {

        @NotNull
        EnumResult<E> apply(@NotNull String enumName, @NotNull Map<String, Object> fieldValues, int ordinal);

    }
}
