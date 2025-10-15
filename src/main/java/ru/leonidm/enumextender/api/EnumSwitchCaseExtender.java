package ru.leonidm.enumextender.api;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import ru.leonidm.enumextender.util.ClassLoaderUtils;
import ru.leonidm.enumextender.util.ReflectionUtils;
import ru.leonidm.enumextender.util.UnsafeUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @param <E> enum type
 * @author LeonidM
 */
public final class EnumSwitchCaseExtender<E extends Enum<E>> {

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final List<SwitchCasePatcher<E>> patchers = new ArrayList<>();
    private final Map<Class<?>, Integer> patchVersions = new HashMap<>();
    private final EnumExtender<E> enumExtender;

    public EnumSwitchCaseExtender(@NotNull EnumExtender<E> enumExtender) {
        this.enumExtender = enumExtender;
    }

    /**
     * Adds patcher for switch-cases
     *
     * @param patcher switch case patcher
     * @return this
     */
    @NotNull
    @Contract("_ -> this")
    public EnumSwitchCaseExtender<E> addPatcher(@NotNull SwitchCasePatcher<E> patcher) {
        patchers.add(patcher);
        return this;
    }

    /**
     * Scans all loaded classes in provided class loader (also, it can scan its parents),
     * finds all switch/case synthetic classes created for provided enumeration class
     * and extends arrays inside them if needed (right now only default values are supported).
     *
     * @param classLoader   class loader whose switch/case synthetic classes must be extended
     * @param extendParents if true also parents of the class loader will be extended
     */
    @NotNull
    public EnumSwitchCaseExtender<E> patch(@NotNull ClassLoader classLoader, boolean extendParents) {
        // TODO: use Java Instrumentation API to patch new loaded classes with call of mappers in <clinit>
        try {
            Class<E> enumClass = enumExtender.getEnumClass();

            scan(enumClass, classLoader, extendParents, (originalClass, field) -> {
                int patchVersion = patchVersions.getOrDefault(originalClass, 0);
                int currentPatchVersion = patchers.size();
                if (patchVersion >= currentPatchVersion) {
                    return;
                }

                patchVersions.put(originalClass, currentPatchVersion);

                int[] array = UnsafeUtils.getStaticFieldSafely(field);

                for (int i = patchVersion; i < currentPatchVersion; i++) {
                    SwitchCasePatcher<E> patcher = patchers.get(i);
                    array = patcher.patch(originalClass, array);
                }

                UnsafeUtils.setStaticFieldSafely(field, array);
            });
        } catch (Exception e) {
            throw new EnumExtendException(e);
        }

        return this;
    }

    private void scan(@NotNull Class<E> enumClass, @NotNull ClassLoader classLoader,
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
                    continue;
                }

                if (field.getName().equals(name)) {
                    Class<?> originalClass = ReflectionUtils.getOriginalClass(clazz);
                    try {
                        consumer.accept(originalClass != null ? originalClass : clazz, field);
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, e, () -> "Could not patch switch-case class '%s'".formatted(clazz));
                    }
                }
            }
        };

        if (extendParents) {
            ClassLoaderUtils.getClassesWithParentsSynchronized(classLoader, classesConsumer);
        } else {
            ClassLoaderUtils.getClassesSynchronized(classLoader, classesConsumer);
        }
    }


    @NotNull
    @UnmodifiableView
    public List<SwitchCasePatcher<E>> getPatchers() {
        return Collections.unmodifiableList(patchers);
    }
}
