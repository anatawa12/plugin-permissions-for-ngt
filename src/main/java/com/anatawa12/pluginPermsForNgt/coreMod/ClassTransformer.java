package com.anatawa12.pluginPermsForNgt.coreMod;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class ClassTransformer implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if ("jp.ngt.ngtlib.util.PermissionManager".equals(name))
            return transformNGTPermissionManager(basicClass);
        if ("com.anatawa12.fixRtm.PermissionManager".equals(name))
            return transformFixRTMPermissionManager(basicClass);
        return basicClass;
    }

    private byte[] transformNGTPermissionManager(byte[] basicClass) {
        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, 0);

        // remove final flag
        node.access = node.access & ~Opcodes.ACC_FINAL;
        for (MethodNode method : node.methods) {
            if ("<clinit>".equals(method.name) && "()V".equals(method.desc)) {
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn.getOpcode() == Opcodes.NEW
                            && "jp/ngt/ngtlib/util/PermissionManager".equals(((TypeInsnNode) insn).desc)) {
                        ((TypeInsnNode) insn).desc = internalNameOfNGTPermissionManagerBukkit;
                    } else if (insn.getOpcode() == Opcodes.INVOKESPECIAL
                            && "jp/ngt/ngtlib/util/PermissionManager".equals(((MethodInsnNode) insn).owner)
                            && "<init>".equals(((MethodInsnNode) insn).name)) {
                        assert "()V".equals(((MethodInsnNode) insn).desc) : "invalid PermissionManager ctor call";
                        ((MethodInsnNode) insn).owner = internalNameOfNGTPermissionManagerBukkit;
                    }
                }
            } else if ("<init>".equals(method.name)) {
                method.access &= ~Opcodes.ACC_PRIVATE;
                method.access |= Opcodes.ACC_PROTECTED;
            }
        }

        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private byte[] transformFixRTMPermissionManager(byte[] basicClass) {
        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, 0);

        // remove final flag
        node.access = node.access & ~Opcodes.ACC_FINAL;
        for (MethodNode method : node.methods) {
            if ("registerPermission".equals(method.name) && "(Ljava/lang/String;)V".equals(method.desc)) {
                InsnList toInsert = new InsnList();
                // the first param
                toInsert.add(new VarInsnNode(Opcodes.ALOAD, 1));
                toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC, 
                        "com/anatawa12/pluginPermsForNgt/FixRTMPermissionManagerHooks",
                        "registerPermission", 
                        "(Ljava/lang/String;)V",
                        false));
                method.instructions.insert(toInsert);
            }
        }

        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static final String internalNameOfNGTPermissionManagerBukkit
            = "com/anatawa12/pluginPermsForNgt/NGTPermissionManagerBukkit";
}
