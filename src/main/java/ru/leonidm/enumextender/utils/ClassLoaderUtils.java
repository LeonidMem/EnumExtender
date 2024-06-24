package ru.leonidm.enumextender.utils;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author LeonidM
 */
public final class ClassLoaderUtils {

    private static Field classesField;

    private ClassLoaderUtils() {

    }

    @NotNull
    private static Field getClassesField() {
        if (classesField == null) {
            try {
                Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
                getDeclaredFields0.setAccessible(true);

                Field[] declaredFields = (Field[]) getDeclaredFields0.invoke(ClassLoader.class, false);

                for (Field field : declaredFields) {
                    if (field.getName().equals("classes")) {
                        classesField = field;
                    }
                }

                if (classesField == null) {
                    throw new NoSuchFieldException("classes");
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        return classesField;
    }

    /**
     * Gets loaded classes by the provided class loader and sends them in the provided consumer.
     * This method provides multi-thread safety, because the JVM adds each loaded class in
     * the list synchronizing with it.
     *
     * @param consumer consumer of the loaded classes
     * @throws IllegalStateException if JVM is somehow broken
     */
    public static void getClassesSynchronized(@NotNull ClassLoader classLoader, @NotNull Consumer<List<Class<?>>> consumer) {
        List<Class<?>> classes = UnsafeUtils.getFieldSafely(classLoader, getClassesField());
        synchronized (classes) {
            consumer.accept(Collections.unmodifiableList(classes));
        }
    }

    /**
     * Gets loaded classes by the provided class loader and its parents and sends them in the provided consumer
     * separately for each class loader.
     * This method provides multi-thread safety, because the JVM adds each loaded class in
     * the list synchronizing with it.
     *
     * @param consumer consumer of the loaded classes
     * @throws IllegalStateException if JVM is somehow broken
     */
    public static void getClassesWithParentsSynchronized(@NotNull ClassLoader classLoader,
                                                         @NotNull Consumer<List<Class<?>>> consumer) {
        while (classLoader != null) {
            getClassesSynchronized(classLoader, consumer);
            classLoader = classLoader.getParent();
        }
    }
}
