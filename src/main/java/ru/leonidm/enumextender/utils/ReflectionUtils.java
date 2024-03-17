package ru.leonidm.enumextender.utils;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

/**
 * @author LeonidM
 */
public final class ReflectionUtils {

    private ReflectionUtils() {

    }

    /**
     * Finds values field in the provided enumeration class, allowing for cases where the field name
     * has already been taken.
     * @param enumClass enumeration class where values field must be found
     */
    @NotNull
    public static <E extends Enum<E>> Field findValuesField(@NotNull Class<E> enumClass) {
        Field field = null;
        String currentName = "$VALUES";
        while (true) {
            try {
                field = enumClass.getDeclaredField(currentName);
                currentName += "$";
            } catch (NoSuchFieldException e) {
                if (field == null) {
                    throw new IllegalStateException("There is no field \"$VALUES\" in %s".formatted(enumClass));
                }

                return field;
            }
        }
    }
}
