package ru.leonidm.enumextender.instrument;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Writes jars that JVM keeps opened until exit (agent and bootstrap jars) into the temporary directory.
 *
 * @author LeonidM
 */
final class TempJars {

    private TempJars() {

    }

    /**
     * Reads bytes of the class from the library jar without loading the class
     *
     * @param internalName internal name of the class, for example {@code java/lang/Object}
     * @return bytes of the class file
     */
    public static byte @NotNull [] readClass(@NotNull String internalName) throws IOException {
        try (InputStream input = TempJars.class.getClassLoader().getResourceAsStream(internalName + ".class")) {
            if (input == null) {
                throw new IOException("Could not find bytes of class " + internalName);
            }

            return input.readAllBytes();
        }
    }

    /**
     * Writes jar into the temporary directory under the name with the hash of its content, so the same jar
     * is reused instead of creating new one on every start. Jars with the same prefix left by previous starts
     * are deleted, except ones that are still used by running JVMs.
     *
     * @param prefix   prefix of the jar name
     * @param manifest manifest of the jar or null if it is not needed
     * @param entries  names and bytes of the jar entries
     * @return path to the written jar
     */
    @NotNull
    public static Path write(
            @NotNull String prefix,
            @Nullable Manifest manifest,
            @NotNull Map<String, byte[]> entries
    ) throws IOException {
        Path jar = Path.of(System.getProperty("java.io.tmpdir"), prefix + "-" + hash(manifest, entries) + ".jar");
        deleteOld(jar.getParent(), prefix, jar);
        if (Files.exists(jar)) {
            return jar;
        }

        Path temp = Files.createTempFile(jar.getParent(), prefix, ".tmp");
        try {
            try (OutputStream fileOutput = Files.newOutputStream(temp);
                 JarOutputStream output = manifest != null ? new JarOutputStream(fileOutput, manifest) : new JarOutputStream(fileOutput)) {
                for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                    output.putNextEntry(new JarEntry(entry.getKey()));
                    output.write(entry.getValue());
                    output.closeEntry();
                }
            }

            try {
                Files.move(temp, jar, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                if (!Files.exists(jar)) {
                    throw e;
                }
            }
        } finally {
            Files.deleteIfExists(temp);
        }

        return jar;
    }

    @NotNull
    private static String hash(@Nullable Manifest manifest, @NotNull Map<String, byte[]> entries) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }

        if (manifest != null) {
            ByteArrayOutputStream manifestOutput = new ByteArrayOutputStream();
            manifest.write(manifestOutput);
            digest.update(manifestOutput.toByteArray());
        }

        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            digest.update(entry.getKey().getBytes(StandardCharsets.UTF_8));
            digest.update(entry.getValue());
        }

        return HexFormat.of().formatHex(digest.digest(), 0, 8);
    }

    private static void deleteOld(@NotNull Path directory, @NotNull String prefix, @NotNull Path current) {
        try (DirectoryStream<Path> jars = Files.newDirectoryStream(directory, prefix + "*.jar")) {
            for (Path jar : jars) {
                if (jar.equals(current)) {
                    continue;
                }

                try {
                    Files.deleteIfExists(jar);
                } catch (IOException ignored) {

                }
            }
        } catch (IOException ignored) {

        }
    }
}
