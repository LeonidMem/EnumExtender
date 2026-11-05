package ru.leonidm.enumextender.instrument;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.instrument.ClassFileTransformer;
import java.nio.charset.StandardCharsets;
import java.security.ProtectionDomain;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adds call of the switch/case hook at the end of the static initializer of every switch/case
 * synthetic class, so its arrays are patched before any other thread can see them.
 *
 * @author LeonidM
 */
final class SwitchMapTransformer implements ClassFileTransformer {

    private static final byte[] MARKER = "$SwitchMap$".getBytes(StandardCharsets.US_ASCII);
    private static final String ERROR_MESSAGE = "Could not add switch/case hook to class ";
    private static final Logger LOGGER = Logger.getLogger(SwitchMapTransformer.class.getName());

    @Override
    public byte @Nullable [] transform(
            @Nullable ClassLoader loader,
            @Nullable String className,
            @Nullable Class<?> classBeingRedefined,
            @Nullable ProtectionDomain protectionDomain,
            byte @NotNull [] classfileBuffer
    ) {
        if (classBeingRedefined != null || className == null || className.indexOf('$') < 0 || !contains(classfileBuffer, MARKER)) {
            return null;
        }

        try {
            ClassReader reader = new ClassReader(classfileBuffer);
            if ((reader.getAccess() & Opcodes.ACC_SYNTHETIC) == 0 || reader.readShort(6) < Opcodes.V1_5) {
                return null;
            }

            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
            boolean[] found = {false};
            reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {

                @Override
                public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                    if (isSwitchMap(access, name, descriptor)) {
                        found[0] = true;
                    }

                    return super.visitField(access, name, descriptor, signature, value);
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                                 String[] exceptions) {
                    MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                    if (!found[0] || !name.equals("<clinit>")) {
                        return methodVisitor;
                    }

                    return new MethodVisitor(Opcodes.ASM9, methodVisitor) {

                        @Override
                        public void visitInsn(int opcode) {
                            if (opcode == Opcodes.RETURN) {
                                super.visitLdcInsn(Type.getObjectType(className));
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, SwitchMapHookClass.NAME, SwitchMapHookClass.METHOD,
                                        SwitchMapHookClass.METHOD_DESCRIPTOR, false);
                            }

                            super.visitInsn(opcode);
                        }
                    };
                }
            }, 0);

            return found[0] ? writer.toByteArray() : null;
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, ERROR_MESSAGE.concat(className), t);
            return null;
        }
    }

    private static boolean isSwitchMap(int access, @NotNull String name, @NotNull String descriptor) {
        int required = Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC;
        return (access & required) == required && name.startsWith("$SwitchMap$") && descriptor.equals("[I");
    }

    private static boolean contains(byte @NotNull [] bytes, byte @NotNull [] pattern) {
        outer:
        for (int i = 0; i <= bytes.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (bytes[i + j] != pattern[j]) {
                    continue outer;
                }
            }

            return true;
        }

        return false;
    }
}
