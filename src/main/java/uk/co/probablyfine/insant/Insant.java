package uk.co.probablyfine.insant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.ArrayList;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import uk.co.probablyfine.insant.annotations.Cat;
import uk.co.probablyfine.insant.annotations.Dog;

import com.google.common.io.Files;

public class Insant implements ClassFileTransformer {

	public static void main(final String[] args) throws FileNotFoundException, IOException {
		for (final String filename : args)
			fiddle(Files.toByteArray(new File(filename)));
	}


	public static void premain(String agentArguments, Instrumentation instrumentation) throws UnmodifiableClassException {
		instrumentation.addTransformer(new Insant());
	}

	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {

		//We don't want generated classes
		if (!className.contains("probablyfine") || className.contains("$")) {

			return classfileBuffer;
		}

		return fiddle(classfileBuffer);

	}

	private Insant() {}

	@SuppressWarnings("unchecked")
	private static byte[] fiddle(byte[] classfileBuffer) {

		ClassNode cn = new ClassNode();

		new ClassReader(classfileBuffer).accept(cn, 0);

		System.out.println("------");
		System.out.println(cn.name);

		for (final MethodNode m : new ArrayList<MethodNode>(cn.methods)) {

			if (m.name.startsWith("<")) // We don't want <init>, <clinit> etc.
				continue;

			if (null == m.visibleAnnotations)
				continue;

			for (final AnnotationNode n : new ArrayList<AnnotationNode>(m.visibleAnnotations)) {



				//Name of the annotation
				String annName = n.desc.substring(1,n.desc.length()-1).replaceAll("/", ".");

				if (annName.matches(Dog.class.getName())) {
					System.out.println("dog> "+m.name);

					//This is our list of instructions that we're going to insert
					InsnList list = new InsnList();

					//We want System.out, which is a java.io.PrintStream
					list.add(new FieldInsnNode(Opcodes.GETSTATIC, "Ljava/lang/System;", "out", "Ljava/io/PrintStream;"));

					//This is the message we want to write
					list.add(new LdcInsnNode("Woof! I'm a dog!"));

					//This calls the println of the Field we got with the argument we made
					list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream" , "println", "(Ljava/lang/Object;)V"));

					//We need to fiddle the stack size. WHYYYYYYYYY
					m.maxStack += 2;

					//Insert that!
					m.instructions.insert(list);
				} else if (annName.matches(Cat.class.getName())) {
					System.out.println("cat> "+m.name);
				} else {
					System.out.println("bad> "+m.name);
				}
			}
		}

		System.out.println("------");

		final ClassWriter cw = new ClassWriter(0);
		cn.accept(cw);
		return cw.toByteArray();
	}
}
