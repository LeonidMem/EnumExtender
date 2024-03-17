package ru.leonidm.enumextender.utils;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * @author LeonidM
 */
public final class UnsafeUtils {

    public static final Unsafe UNSAFE;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Gets value from the provided static field using {@link Unsafe} class as safely as possible.
     * It is very useful when working with highly protected fields.
     */
    @UnknownNullability
    public static <T> T getStaticFieldSafely(@NotNull Field field) {
        return getFieldSafely(UNSAFE.staticFieldBase(field), UNSAFE.staticFieldOffset(field), field);
    }

    /**
     * Gets value from the provided field of the provided object using {@link Unsafe} class as safely as possible.
     * It is very useful when working with highly protected fields.
     */
    @UnknownNullability
    public static <T> T getFieldSafely(@NotNull Object object, @NotNull Field field) {
        return getFieldSafely(object, UNSAFE.objectFieldOffset(field), field);
    }

    @UnknownNullability
    private static <T> T getFieldSafely(@NotNull Object object, long fieldOffset, @NotNull Field field) {
        Object result;
        if (Modifier.isVolatile(field.getModifiers())) {
            if (field.getType() == boolean.class) {
                result = UNSAFE.getBooleanVolatile(object, fieldOffset);
            } else if (field.getType() == byte.class) {
                result = UNSAFE.getByteVolatile(object, fieldOffset);
            } else if (field.getType() == short.class) {
                result = UNSAFE.getShortVolatile(object, fieldOffset);
            } else if (field.getType() == int.class) {
                result = UNSAFE.getIntVolatile(object, fieldOffset);
            } else if (field.getType() == long.class) {
                result = UNSAFE.getLongVolatile(object, fieldOffset);
            } else if (field.getType() == float.class) {
                result = UNSAFE.getFloatVolatile(object, fieldOffset);
            } else if (field.getType() == double.class) {
                result = UNSAFE.getDoubleVolatile(object, fieldOffset);
            } else if (field.getType() == char.class) {
                result = UNSAFE.getCharVolatile(object, fieldOffset);
            } else {
                result = UNSAFE.getObjectVolatile(object, fieldOffset);
            }
        } else {
            if (field.getType() == boolean.class) {
                result = UNSAFE.getBoolean(object, fieldOffset);
            } else if (field.getType() == byte.class) {
                result = UNSAFE.getByte(object, fieldOffset);
            } else if (field.getType() == short.class) {
                result = UNSAFE.getShort(object, fieldOffset);
            } else if (field.getType() == int.class) {
                result = UNSAFE.getInt(object, fieldOffset);
            } else if (field.getType() == long.class) {
                result = UNSAFE.getLong(object, fieldOffset);
            } else if (field.getType() == float.class) {
                result = UNSAFE.getFloat(object, fieldOffset);
            } else if (field.getType() == double.class) {
                result = UNSAFE.getDouble(object, fieldOffset);
            } else if (field.getType() == char.class) {
                result = UNSAFE.getChar(object, fieldOffset);
            } else {
                result = UNSAFE.getObject(object, fieldOffset);
            }
        }

        return (T) result;
    }

    /**
     * Sets value to the provided static field with the provided value using {@link Unsafe} class
     * as safely as possible.
     * It is very useful when working with highly protected fields.
     */
    public static void setStaticFieldSafely(@NotNull Field field, @Nullable Object value) {
        setFieldSafely(UNSAFE.staticFieldBase(field), UNSAFE.staticFieldOffset(field), field, value);
    }

    /**
     * Sets value to the provided field of the provided object with the provided value using {@link Unsafe} class
     * as safely as possible.
     * It is very useful when working with highly protected fields.
     */
    public static void setFieldSafely(@NotNull Object object, @NotNull Field field, @Nullable Object value) {
        setFieldSafely(object, UNSAFE.objectFieldOffset(field), field, value);
    }

    private static void setFieldSafely(@NotNull Object object, long fieldOffset, @NotNull Field field,
                                       @Nullable Object value) {
        if (Modifier.isVolatile(field.getModifiers())) {
            if (field.getType() == boolean.class) {
                UNSAFE.putBooleanVolatile(object, fieldOffset, cast(value, field));
            } else if (field.getType() == byte.class) {
                UNSAFE.putByteVolatile(object, fieldOffset, cast(value, field));
            } else if (field.getType() == short.class) {
                UNSAFE.putShortVolatile(object, fieldOffset, cast(value, field));
            } else if (field.getType() == int.class) {
                UNSAFE.putIntVolatile(object, fieldOffset, cast(value, field));
            } else if (field.getType() == long.class) {
                UNSAFE.putLongVolatile(object, fieldOffset, cast(value, field));
            } else if (field.getType() == float.class) {
                UNSAFE.putFloatVolatile(object, fieldOffset, cast(value, field));
            } else if (field.getType() == double.class) {
                UNSAFE.putDoubleVolatile(object, fieldOffset, cast(value, field));
            } else if (field.getType() == char.class) {
                UNSAFE.putCharVolatile(object, fieldOffset, cast(value, field));
            } else {
                UNSAFE.putObjectVolatile(object, fieldOffset, value);
            }
        } else {
            if (field.getType() == boolean.class) {
                UNSAFE.putBoolean(object, fieldOffset, cast(value, field));
            } else if (field.getType() == byte.class) {
                UNSAFE.putByte(object, fieldOffset, cast(value, field));
            } else if (field.getType() == short.class) {
                UNSAFE.putShort(object, fieldOffset, cast(value, field));
            } else if (field.getType() == int.class) {
                UNSAFE.putInt(object, fieldOffset, cast(value, field));
            } else if (field.getType() == long.class) {
                UNSAFE.putLong(object, fieldOffset, cast(value, field));
            } else if (field.getType() == float.class) {
                UNSAFE.putFloat(object, fieldOffset, cast(value, field));
            } else if (field.getType() == double.class) {
                UNSAFE.putDouble(object, fieldOffset, cast(value, field));
            } else if (field.getType() == char.class) {
                UNSAFE.putChar(object, fieldOffset, cast(value, field));
            } else {
                UNSAFE.putObject(object, fieldOffset, value);
            }
        }
    }

    @NotNull
    private static <T> T cast(@Nullable Object value, @NotNull Field field) {
        if (value == null) {
            throw new IllegalArgumentException("Field %s requires %s, got null"
                    .formatted(field, field.getType()));
        }

        try {
            return (T) value;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Field %s requires %s, got %s (class = %s)"
                    .formatted(field, field.getType(), value, value.getClass()));
        }
    }

    /**
     * Allocates an instance but does not run any constructor. Initializes the class if it has not yet been.
     */
    @NotNull
    @SneakyThrows
    public static <T> T allocateInstance(@NotNull Class<T> tClass) {
        return (T) UNSAFE.allocateInstance(tClass);
    }
}
