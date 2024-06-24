package ru.leonidm.enumextender;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.leonidm.enumextender.utils.ClassLoaderUtils;
import ru.leonidm.enumextender.utils.ReflectionUtils;
import ru.leonidm.enumextender.utils.UnsafeUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
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
     *
     * @param enumClass     enumeration class that must be extended
     * @param classLoader   class loader whose switch/case synthetic classes must be extended
     * @param mapper        mapper from synthetic field to switch/case mappings in this field
     * @param extendParents if true also parents of the class loader will be extended
     */
    public static <E extends Enum<E>> void extend(@NotNull Class<E> enumClass, @NotNull ClassLoader classLoader,
                                                  @Nullable Mapper<E> mapper, boolean extendParents) {
        Field valuesField = ReflectionUtils.findValuesField(enumClass);
        E[] values = UnsafeUtils.getStaticFieldSafely(valuesField);

        try {
            scan(enumClass, classLoader, extendParents, (originalClass, field) -> {
                int[] array = UnsafeUtils.getStaticFieldSafely(field);
                if (array.length == values.length) {
                    return;
                }

                int[] newArray = new int[values.length];
                System.arraycopy(array, 0, newArray, 0, array.length);

                if (mapper != null) {
                    Map<E, E> mappings = mapper.prepareMappings(originalClass);
                    System.out.println("[EnumSwitchCaseExtender:51] mappings: " + mappings);
                    for (var entry : mappings.entrySet()) {
                        E key = entry.getKey();
                        E value = entry.getValue();

                        newArray[key.ordinal()] = value != null ? value.ordinal() + 1 : 0;

                        System.out.println("[EnumSwitchCaseExtender:54] key.ordinal(): " + key.ordinal());
                        System.out.println("[EnumSwitchCaseExtender:54] value.ordinal(): " + (value != null ? value.ordinal() : 0));
                    }
                }

                System.out.println("[EnumSwitchCaseExtender:55] Arrays.toString(newArray): " + Arrays.toString(newArray));

                UnsafeUtils.setStaticFieldSafely(field, newArray);
            });
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static <E extends Enum<E>> void scan(@NotNull Class<E> enumClass, @NotNull ClassLoader classLoader,
                                                 boolean extendParents, @NotNull BiConsumer<Class<?>, Field> consumer) {
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
                    Class<?> originalClass = ReflectionUtils.getOriginalClass(clazz);
                    consumer.accept(originalClass != null ? originalClass : clazz, field);
                }
            }
        };

        if (extendParents) {
            ClassLoaderUtils.getClassesWithParentsSynchronized(classLoader, classesConsumer);
        } else {
            ClassLoaderUtils.getClassesSynchronized(classLoader, classesConsumer);
        }
    }

    @FunctionalInterface
    public interface Mapper<E extends Enum<E>> {

        /**
         * @param originalClass in mostly cases it must be classes where
         *                      synthetic switch/case class was created,
         *                      but, probably, they could not have been found
         *                      each time, so in this case synthetic class
         *                      is returned
         * @return mappings from enum to its value in new array
         */
        @NotNull
        Map<@NotNull E, @Nullable E> prepareMappings(@NotNull Class<?> originalClass);

    }
}
