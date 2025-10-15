package ru.leonidm.enumextender.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
     *
     * @param enumClass enumeration class where values field must be found
     */
    @NotNull
    public static Field findValuesField(@NotNull Class<? extends Enum<?>> enumClass) {
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

    /**
     * Tries to find original class where from provided synthetic class
     * was created
     *
     * @param syntheticClass synthetic class
     * @return original class if found, otherwise null
     */
    @Nullable
    public static Class<?> getOriginalClass(@NotNull Class<?> syntheticClass) {
        if (!syntheticClass.isSynthetic()) {
            return null;
        }

        String name = syntheticClass.getName();
        if (!name.contains("$")) {
            return null;
        }

        try {
            String mainName = name.substring(0, name.lastIndexOf('$'));
            return Class.forName(mainName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
