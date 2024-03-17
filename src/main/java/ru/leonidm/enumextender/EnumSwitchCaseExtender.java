package ru.leonidm.enumextender;

import org.jetbrains.annotations.NotNull;
import ru.leonidm.enumextender.utils.ClassLoaderUtils;
import ru.leonidm.enumextender.utils.ReflectionUtils;
import ru.leonidm.enumextender.utils.UnsafeUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author LeonidM
 */
public final class EnumSwitchCaseExtender {

    private EnumSwitchCaseExtender() {

    }

    /**
     * Scans all loaded classes in provided class loader (also, it can scan its parents),
     * finds all switch/case synthetic classes created for provided enumeration class
     * and extends arrays inside them if needed (right now only default values are supported).
     * @param enumClass enumeration class that must be extended
     * @param classLoader class loader whose switch/case synthetic classes must be extended
     * @param extendParents if true also parents of provided class loader will be extended
     */
    public static <E extends Enum<E>> void extend(@NotNull Class<E> enumClass, @NotNull ClassLoader classLoader,
                                                  boolean extendParents) {
        Field valuesField = ReflectionUtils.findValuesField(enumClass);
        E[] values = UnsafeUtils.getStaticFieldSafely(valuesField);

        try {
            scan(enumClass, classLoader, extendParents, (field) -> {
                int[] array = UnsafeUtils.getStaticFieldSafely(field);
                if (array.length == values.length) {
                    return;
                }

                int[] newArray = new int[values.length];
                System.arraycopy(array, 0, newArray, 0, array.length);

                UnsafeUtils.setStaticFieldSafely(field, newArray);
            });
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static <E extends Enum<E>> void scan(@NotNull Class<E> enumClass, @NotNull ClassLoader classLoader,
                                                 boolean extendParents, @NotNull Consumer<Field> consumer) {
        String name = "$SwitchMap$" + enumClass.getName().replace('.', '$');

        Consumer<List<Class<?>>> classesConsumer = classes -> {
            for (Class<?> clazz : classes) {
                if (!clazz.isSynthetic()) {
                    continue;
                }

                Field[] declaredFields = clazz.getDeclaredFields();
                if (declaredFields.length != 1) {
                    continue;
                }

                Field field = declaredFields[0];
                int modifiers = field.getModifiers();
                if (!Modifier.isStatic(modifiers) || !Modifier.isFinal(modifiers) || !field.isSynthetic()) {
                    continue;
                }

                Class<?> fieldType = field.getType();
                if (!fieldType.isArray() || fieldType.componentType() != int.class) {
                    return;
                }

                if (field.getName().equals(name)) {
                    consumer.accept(field);
                }
            }
        };

        if (extendParents) {
            ClassLoaderUtils.getClassesWithParentsSynchronized(classLoader, classesConsumer);
        } else {
            ClassLoaderUtils.getClassesSynchronized(classLoader, classesConsumer);
        }
    }
}
