package ru.leonidm.enumextender.instrument;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.leonidm.enumextender.api.EnumExtendException;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Loads {@link EnumExtenderAgent} into the current JVM: attaches to itself or, if JVM forbids it,
 * uses the helper process with {@link EnumExtenderAttacher}.
 *
 * @author LeonidM
 */
final class AgentLoader {

    private AgentLoader() {

    }

    /**
     * Returns instrumentation of {@link EnumExtenderAgent}, loading the agent if it was not loaded yet
     *
     * @return instrumentation of the agent
     * @throws EnumExtendException if agent could not be loaded
     */
    @NotNull
    public static Instrumentation load() {
        Instrumentation instrumentation = findInstrumentation();
        if (instrumentation != null) {
            return instrumentation;
        }

        try {
            attach(createAgentJar());
        } catch (EnumExtendException e) {
            throw e;
        } catch (Exception e) {
            throw new EnumExtendException(e);
        }

        instrumentation = findInstrumentation();
        if (instrumentation == null) {
            throw new EnumExtendException("Agent was loaded, but instrumentation was not found");
        }

        return instrumentation;
    }

    @Nullable
    private static Instrumentation findInstrumentation() {
        try {
            Class<?> agentClass = Class.forName(EnumExtenderAgent.class.getName(), false, ClassLoader.getSystemClassLoader());
            return (Instrumentation) agentClass.getField("instrumentation").get(null);
        } catch (ClassNotFoundException e) {
            return null;
        } catch (ReflectiveOperationException e) {
            throw new EnumExtendException(e);
        }
    }

    @NotNull
    private static Path createAgentJar() throws IOException {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.putValue("Agent-Class", EnumExtenderAgent.class.getName());
        attributes.putValue("Premain-Class", EnumExtenderAgent.class.getName());
        attributes.putValue("Can-Redefine-Classes", "true");
        attributes.putValue("Can-Retransform-Classes", "true");

        Map<String, byte[]> entries = new LinkedHashMap<>();
        putClass(entries, EnumExtenderAgent.class);
        putClass(entries, EnumExtenderAttacher.class);
        return TempJars.write("enum-extender-agent", manifest, entries);
    }

    private static void putClass(@NotNull Map<String, byte[]> entries, @NotNull Class<?> clazz) throws IOException {
        String internalName = clazz.getName().replace('.', '/');
        entries.put(internalName + ".class", TempJars.readClass(internalName));
    }

    private static void attach(@NotNull Path agentJar) throws IOException, InterruptedException {
        String pid = String.valueOf(ProcessHandle.current().pid());
        Throwable selfAttachError;
        try {
            Class<?> virtualMachineClass = Class.forName("com.sun.tools.attach.VirtualMachine");
            Object virtualMachine = virtualMachineClass.getMethod("attach", String.class).invoke(null, pid);
            try {
                virtualMachineClass.getMethod("loadAgent", String.class).invoke(virtualMachine, agentJar.toString());
            } finally {
                virtualMachineClass.getMethod("detach").invoke(virtualMachine);
            }
            return;
        } catch (Throwable t) {
            selfAttachError = t;
        }

        Path bin = Path.of(System.getProperty("java.home"), "bin");
        Path java = Files.exists(bin.resolve("java.exe")) ? bin.resolve("java.exe") : bin.resolve("java");
        Path output = Files.createTempFile("enum-extender-attach", ".log");
        try {
            Process process;
            try {
                process = new ProcessBuilder(java.toString(), "-cp", agentJar.toString(), EnumExtenderAttacher.class.getName(),
                        pid, agentJar.toString())
                        .redirectErrorStream(true)
                        .redirectOutput(output.toFile())
                        .start();
            } catch (IOException e) {
                e.addSuppressed(selfAttachError);
                throw e;
            }

            if (!process.waitFor(1, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                throw attachError("Helper process did not attach agent in time", selfAttachError);
            }

            if (process.exitValue() != 0) {
                throw attachError("Helper process could not attach agent: "
                                  + new String(Files.readAllBytes(output), StandardCharsets.UTF_8), selfAttachError);
            }
        } finally {
            Files.deleteIfExists(output);
        }
    }

    @NotNull
    private static EnumExtendException attachError(@NotNull String message, @NotNull Throwable selfAttachError) {
        EnumExtendException exception = new EnumExtendException(message);
        exception.addSuppressed(selfAttachError);
        return exception;
    }
}
