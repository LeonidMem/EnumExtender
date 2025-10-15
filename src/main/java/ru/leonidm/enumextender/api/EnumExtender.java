package ru.leonidm.enumextender.api;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import ru.leonidm.enumextender.util.ReflectionUtils;
import ru.leonidm.enumextender.util.UnsafeUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @author LeonidM
 */
public final class EnumExtender<E extends Enum<E>> {

    private static final Map<Class<? extends Enum<?>>, EnumExtender<?>> EXTENDERS = new HashMap<>();

    private final Class<E> enumClass;
    private final EnumSwitchCaseExtender<E> enumSwitchCaseExtender;

    private EnumExtender(@NotNull Class<E> enumClass) {
        this.enumClass = enumClass;
        enumSwitchCaseExtender = new EnumSwitchCaseExtender<>(this);
    }

    /**
     * Returns {@link EnumExtender} instance
     *
     * @param enumClass enumeration class that must be extended
     * @param <E>       enum type
     * @return instance of {@link EnumExtender<E>}
     */
    @NotNull
    public static <E extends Enum<E>> EnumExtender<E> of(@NotNull Class<E> enumClass) {
        return (EnumExtender<E>) EXTENDERS.computeIfAbsent(enumClass, k -> new EnumExtender<>(enumClass));
    }

    /**
     * Returns new instance of {@link EnumBuilder}
     *
     * @param enumName name of new enumeration instance
     * @return instance of {@link EnumBuilder<E>}
     */
    @NotNull
    public EnumBuilder<E> enumBuilder(@NotNull String enumName) {
        return new EnumBuilder<>(enumName, this::extendEnum);
    }

    /**
     * Creates new instance of enumeration, pushes it into the values array,
     * clears cache for enum constants and updates enum constant directory.
     *
     * @param enumName    name of new enumeration instance
     * @param fieldValues map that contains names of the fields declared in the enumeration class and
     *                    corresponding values. If some field is not specified in the map,
     *                    it will contain default value (0 for primitives, null for objects)
     * @return created instance of enumeration
     * @throws EnumExtendException if enum with such name exists or if JVM is somehow broken
     */
    @NotNull
    public EnumResult<E> addEnum(@NotNull String enumName, @NotNull Map<String, Object> fieldValues) {
        return extendEnum(enumName, fieldValues, -1);
    }

    /**
     * Creates new instance of enumeration, pushes it into the values array
     * before provided {@code E}, shift all enums from provided {@code E} by 1,
     * clears cache for enum constants and updates enum constant directory.
     *
     * @param enumName     name of new enumeration instance
     * @param fieldValues  map that contains names of the fields declared in the enumeration class and
     *                     corresponding values. If some field is not specified in the map,
     *                     it will contain default value (0 for primitives, null for objects)
     * @param insertBefore enumeration instance that must go after new one in context of all values and
     *                     ordinals
     * @return created instance of enumeration
     * @throws EnumExtendException if enum with such name exists or if JVM is somehow broken
     */
    @NotNull
    public EnumResult<E> insertEnum(@NotNull String enumName, @NotNull Map<String, Object> fieldValues,
                                    @NotNull E insertBefore) {
        return extendEnum(enumName, fieldValues, insertBefore.ordinal());
    }

    @NotNull
    private EnumResult<E> extendEnum(@NotNull String enumName, @NotNull Map<String, Object> fieldValues, int ordinal) {
        E e = null;
        try {
            e = Enum.valueOf(enumClass, enumName);
        } catch (IllegalArgumentException ignored) {

        }

        if (e != null) {
            return new EnumResult.Error<>(
                    new EnumExtendException("Enum with name '%s' is already defined in %s".formatted(enumName, enumClass))
            );
        }

        try {
            e = UnsafeUtils.allocateInstance(enumClass);

            Field valuesField = ReflectionUtils.findValuesField(enumClass);
            E[] values = UnsafeUtils.getStaticFieldSafely(valuesField);

            UnsafeUtils.setFieldSafely(e, Enum.class.getDeclaredField("name"), enumName);
            Field ordinalField = Enum.class.getDeclaredField("ordinal");
            UnsafeUtils.setFieldSafely(e, ordinalField, ordinal < 0 ? values.length : ordinal);

            for (Field field : enumClass.getDeclaredFields()) {
                Object value = fieldValues.get(field.getName());
                if (value != null) {
                    UnsafeUtils.setFieldSafely(e, field, value);
                }
            }

            E[] newValues = (E[]) Array.newInstance(enumClass, values.length + 1);

            if (ordinal < 0) {
                System.arraycopy(values, 0, newValues, 0, values.length);

                ordinal = values.length;
            } else {
                System.arraycopy(values, 0, newValues, 0, ordinal);
                System.arraycopy(values, ordinal, newValues, ordinal + 1, values.length - ordinal);

                enumSwitchCaseExtender.addPatcher(SwitchCasePatcher.insert(ordinal, values.length));

                for (int i = ordinal; i < values.length; i++) {
                    UnsafeUtils.setFieldSafely(values[i], ordinalField, i + 1);
                }
            }

            newValues[ordinal] = e;

            UnsafeUtils.setStaticFieldSafely(valuesField, newValues);

            Field enumConstantsField = Class.class.getDeclaredField("enumConstants");
            UnsafeUtils.setFieldSafely(enumClass, enumConstantsField, null);

            Field enumConstantDirectoryField = Class.class.getDeclaredField("enumConstantDirectory");
            Map<String, E> map = UnsafeUtils.getFieldSafely(enumClass, enumConstantDirectoryField);
            if (map != null) {
                map.put(enumName, e);
            }
        } catch (Exception ex) {
            return new EnumResult.Error<>(
                    new EnumExtendException(ex)
            );
        }

        return new EnumResult.Success<>(e);
    }

    @NotNull
    @Contract("-> new")
    public EnumSwitchCaseExtender<E> switchCase() {
        return enumSwitchCaseExtender;
    }


    @NotNull
    public Class<E> getEnumClass() {
        return enumClass;
    }
}
