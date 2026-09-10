package git.prayoadmii.viaproxyplus.injection;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class PluginPackageRemapper implements ClassFileTransformer {

    private static final String UPSTREAM_PACKAGE = "net/raphimc/viaproxy";
    private static final String FORK_PACKAGE = "git/prayoadmii/viaproxyplus";

    @Override
    public byte[] transform(final ClassLoader loader, final String className, final Class<?> classBeingRedefined,
                            final ProtectionDomain protectionDomain, final byte[] classfileBuffer) {
        final ClassReader reader = new ClassReader(classfileBuffer);
        final ClassWriter writer = new ClassWriter(reader, 0);
        final ClassRemapper remapper = new ClassRemapper(writer, new Remapper(Opcodes.ASM9) {
            @Override
            public String map(final String internalName) {
                if (internalName.startsWith(UPSTREAM_PACKAGE + "/")) {
                    return FORK_PACKAGE + internalName.substring(UPSTREAM_PACKAGE.length());
                }
                return internalName;
            }
        });
        reader.accept(remapper, 0);
        return writer.toByteArray();
    }

}