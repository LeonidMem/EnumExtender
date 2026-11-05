package ru.leonidm.enumextender.instrument;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;
import java.util.jar.JarFile;

/**
 * Defines {@code SwitchMapHook} in the bootstrap class loader, so its method can be called from static initializers
 * of switch/case synthetic classes in any class loader. The hook class must never be loaded by the library class
 * loader.
 *
 * @author LeonidM
 */
final class SwitchMapHookClass {

    public static final String NAME = "ru/leonidm/enumextender/boot/SwitchMapHook";
    public static final String METHOD = "onInit";
    public static final String METHOD_DESCRIPTOR = "(Ljava/lang/Class;)V";

    private static final String FIELD = "hook";

    private SwitchMapHookClass() {

    }

    /**
     * Defines the hook class in the bootstrap class loader if it was not defined yet
     * and adds {@link EnumExtenderHook} to its consumer
     *
     * @param instrumentation instrumentation used to append the hook jar to the bootstrap class loader
     */
    public static void install(@NotNull Instrumentation instrumentation) throws IOException, ReflectiveOperationException {
        registerHook(define(instrumentation));
    }

    @NotNull
    private static Class<?> define(@NotNull Instrumentation instrumentation) throws IOException {
        String name = NAME.replace('/', '.');
        try {
            return Class.forName(name, false, null);
        } catch (ClassNotFoundException ignored) {

        }

        Path hookJar = TempJars.write("enum-extender-hook", null, Map.of(NAME + ".class", TempJars.readClass(NAME)));
        instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(hookJar.toFile()));
        try {
            return Class.forName(name, false, null);
        } catch (ClassNotFoundException e) {
            throw new IOException("Could not define hook class in the bootstrap class loader", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void registerHook(@NotNull Class<?> hookClass) throws ReflectiveOperationException {
        Field field = hookClass.getField(FIELD);
        Consumer<Class<?>> hook = EnumExtenderHook::onSwitchMapInit;
        synchronized (hookClass) {
            Consumer<Class<?>> previous = (Consumer<Class<?>>) field.get(null);
            field.set(null, previous != null ? previous.andThen(hook) : hook);
        }
    }
}
