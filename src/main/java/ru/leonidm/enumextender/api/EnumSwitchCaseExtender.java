package ru.leonidm.enumextender.api;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import ru.leonidm.enumextender.instrument.EnumInstrumentation;
import ru.leonidm.enumextender.util.ClassLoaderUtils;
import ru.leonidm.enumextender.util.ReflectionUtils;
import ru.leonidm.enumextender.util.UnsafeUtils;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
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
    private final Map<Class<?>, Integer> patchVersions = new WeakHashMap<>();
    private final EnumExtender<E> enumExtender;

    public EnumSwitchCaseExtender(@NotNull EnumExtender<E> enumExtender) {
        this.enumExtender = enumExtender;
    }

    /**
     * Adds patcher for switch-cases
     * <p>
     * Patcher <b>must not initialize other classes</b>, since it can be called on static initializers
     * of switch/case synthetic classes. Otherwise, it can deadlock with the thread that is initializing them.
     *
     * @param patcher switch case patcher
     * @return this
     */
    @NotNull
    @Contract("_ -> this")
    public synchronized EnumSwitchCaseExtender<E> addPatcher(@NotNull SwitchCasePatcher<E> patcher) {
        patchers.add(patcher);
        return this;
    }

    /**
     * Scans all loaded classes in provided class loader (also, it can scan its parents),
     * finds all switch/case synthetic classes created for provided enumeration class
     * and applies all patchers that were not applied to them yet.
     * <p>
     * If {@link EnumInstrumentation} is installed, classes that use patched arrays are retransformed
     * to prevent JIT optimizations.
     *
     * @param classLoader   class loader whose switch/case synthetic classes must be extended
     * @param extendParents if true also parents of the class loader will be extended
     */
    @NotNull
    public synchronized EnumSwitchCaseExtender<E> patch(@NotNull ClassLoader classLoader, boolean extendParents) {
        Set<Class<?>> patchedClasses = new HashSet<>();
        Instrumentation instrumentation = EnumInstrumentation.getInstrumentation();
        Class<?>[] loadedClasses = instrumentation != null ? instrumentation.getAllLoadedClasses() : null;
        try {
            scan(classLoader, extendParents, loadedClasses, syntheticClass -> {
                if (instrumentation != null && !UnsafeUtils.isInitialized(syntheticClass)) {
                    return;
                }

                Field field = findSwitchMapField(syntheticClass);
                if (field != null && patchClass(syntheticClass, field)) {
                    patchedClasses.add(getOriginalClass(syntheticClass));
                }
            });
        } catch (Exception e) {
            throw new EnumExtendException(e);
        }

        if (instrumentation != null && !patchedClasses.isEmpty()) {
            deoptimize(instrumentation, loadedClasses, patchedClasses);
        }

        return this;
    }

    public synchronized void patchInitializedClass(@NotNull Class<?> syntheticClass) {
        Field field = findSwitchMapField(syntheticClass);
        if (field != null) {
            patchClass(syntheticClass, field);
        }
    }

    private boolean patchClass(@NotNull Class<?> syntheticClass, @NotNull Field field) {
        int patchVersion = patchVersions.getOrDefault(syntheticClass, 0);
        int currentPatchVersion = patchers.size();
        if (patchVersion >= currentPatchVersion) {
            return false;
        }

        int[] array = UnsafeUtils.getStaticFieldSafely(field);
        if (array == null) {
            return false;
        }

        patchVersions.put(syntheticClass, currentPatchVersion);

        Class<?> originalClass = getOriginalClass(syntheticClass);
        for (int i = patchVersion; i < currentPatchVersion; i++) {
            array = patchers.get(i).patch(originalClass, array);
        }

        UnsafeUtils.setStaticFieldSafely(field, array);
        return true;
    }

    @Nullable
    private Field findSwitchMapField(@NotNull Class<?> syntheticClass) {
        if (!syntheticClass.isSynthetic()) {
            return null;
        }

        String name = "$SwitchMap$" + enumExtender.getEnumClass().getName().replace('.', '$');
        for (Field field : syntheticClass.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (field.getName().equals(name) && Modifier.isStatic(modifiers) && field.isSynthetic()
                && field.getType() == int[].class) {
                return field;
            }
        }

        return null;
    }

    @NotNull
    private static Class<?> getOriginalClass(@NotNull Class<?> syntheticClass) {
        Class<?> originalClass = ReflectionUtils.getOriginalClass(syntheticClass);
        return originalClass != null ? originalClass : syntheticClass;
    }

    private void scan(
            @NotNull ClassLoader classLoader,
            boolean extendParents,
            Class<?> @Nullable [] loadedClasses,
            @NotNull Consumer<Class<?>> consumer
    ) {
        if (loadedClasses != null) {
            Set<ClassLoader> classLoaders = new HashSet<>();
            for (ClassLoader current = classLoader; current != null; current = extendParents ? current.getParent() : null) {
                classLoaders.add(current);
            }

            for (Class<?> clazz : loadedClasses) {
                if (classLoaders.contains(clazz.getClassLoader())) {
                    try {
                        consumer.accept(clazz);
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, e, () -> "Could not patch switch-case class '%s'".formatted(clazz));
                    }
                }
            }

            return;
        }

        Consumer<List<Class<?>>> classesConsumer = classes -> {
            for (Class<?> clazz : classes) {
                try {
                    consumer.accept(clazz);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, e, () -> "Could not patch switch-case class '%s'".formatted(clazz));
                }
            }
        };

        if (extendParents) {
            ClassLoaderUtils.getClassesWithParentsSynchronized(classLoader, classesConsumer);
        } else {
            ClassLoaderUtils.getClassesSynchronized(classLoader, classesConsumer);
        }
    }

    private void deoptimize(@NotNull Instrumentation instrumentation, Class<?> @NotNull [] loadedClasses,
                            @NotNull Set<Class<?>> originalClasses) {
        Map<ClassLoader, Set<String>> originalNames = new HashMap<>();
        for (Class<?> originalClass : originalClasses) {
            originalNames.computeIfAbsent(originalClass.getClassLoader(), k -> new HashSet<>()).add(originalClass.getName());
        }

        List<Class<?>> classes = new ArrayList<>();
        for (Class<?> clazz : loadedClasses) {
            Set<String> names = originalNames.get(clazz.getClassLoader());
            if (names != null && isNestedIn(clazz.getName(), names) && instrumentation.isModifiableClass(clazz)) {
                classes.add(clazz);
            }
        }

        try {
            instrumentation.retransformClasses(classes.toArray(Class<?>[]::new));
        } catch (Exception e) {
            logger.log(Level.SEVERE, e, () -> "Could not deoptimize classes that use patched switch-cases");
        }
    }

    private static boolean isNestedIn(@NotNull String name, @NotNull Set<String> originalNames) {
        if (originalNames.contains(name)) {
            return true;
        }

        for (int i = name.indexOf('$'); i > 0; i = name.indexOf('$', i + 1)) {
            if (originalNames.contains(name.substring(0, i))) {
                return true;
            }
        }

        return false;
    }

    @NotNull
    @UnmodifiableView
    public synchronized List<SwitchCasePatcher<E>> getPatchers() {
        return List.copyOf(patchers);
    }
}
