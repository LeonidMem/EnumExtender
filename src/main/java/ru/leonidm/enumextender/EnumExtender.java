package ru.leonidm.enumextender;

import org.jetbrains.annotations.NotNull;
import ru.leonidm.enumextender.utils.ReflectionUtils;
import ru.leonidm.enumextender.utils.UnsafeUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * @author LeonidM
 */
public final class EnumExtender {

    private EnumExtender() {

    }

    /**
     * Creates new instance of enumeration, pushes it into the values array,
     * clears cache for enum constants and updates enum constant directory.
     *
     * @param enumClass   enumeration class that must be extended
     * @param enumName    new of new enumeration instance
     * @param fieldValues map that contains names of the fields declared in the enumeration class and
     *                    corresponding values. If some field is not specified in the map,
     *                    it will contain default value (0 for primitives, null for objects)
     * @return created instance of enumeration
     * @throws IllegalArgumentException if enum with such name exists
     * @throws IllegalStateException    if JVM is somehow broken
     */
    @NotNull
    public static <E extends Enum<E>> E extend(@NotNull Class<E> enumClass, @NotNull String enumName,
                                               @NotNull Map<String, Object> fieldValues) throws IllegalArgumentException {
        E e = null;
        try {
            e = Enum.valueOf(enumClass, enumName);
        } catch (IllegalArgumentException ignored) {

        }

        if (e != null) {
            throw new IllegalArgumentException("Enum with name '%s' is already defined in %s".formatted(enumName, enumClass));
        }

        e = UnsafeUtils.allocateInstance(enumClass);

        Field valuesField = ReflectionUtils.findValuesField(enumClass);
        E[] values = UnsafeUtils.getStaticFieldSafely(valuesField);

        try {
            UnsafeUtils.setFieldSafely(e, getEnumField("name"), enumName);
            UnsafeUtils.setFieldSafely(e, getEnumField("ordinal"), values.length);

            for (Field field : enumClass.getDeclaredFields()) {
                Object value = fieldValues.get(field.getName());
                if (value != null) {
                    UnsafeUtils.setFieldSafely(e, field, value);
                }
            }

            E[] newValues = (E[]) Array.newInstance(enumClass, values.length + 1);

            System.arraycopy(values, 0, newValues, 0, values.length);
            newValues[values.length] = e;

            UnsafeUtils.setStaticFieldSafely(valuesField, newValues);

            Field enumConstantsField = Class.class.getDeclaredField("enumConstants");
            UnsafeUtils.setFieldSafely(enumClass, enumConstantsField, null);

            Field enumConstantDirectoryField = Class.class.getDeclaredField("enumConstantDirectory");
            Map<String, E> map = UnsafeUtils.getFieldSafely(enumClass, enumConstantDirectoryField);
            if (map != null) {
                map.put(enumName, e);
            }
        } catch (NoSuchFieldException ex) {
            throw new IllegalStateException(ex);
        }

        return e;
    }

    @NotNull
    private static Field getEnumField(@NotNull String name) throws NoSuchFieldException {
        return Enum.class.getDeclaredField(name);
    }
}
