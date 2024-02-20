package top.dreamlike.jdk22ea

import java.io.File
import java.io.PrintStream
import java.lang.classfile.AccessFlags
import java.lang.classfile.ClassFile
import java.lang.classfile.TypeKind
import java.lang.classfile.constantpool.ClassEntry
import java.lang.classfile.constantpool.ConstantPoolBuilder
import java.lang.classfile.constantpool.MethodRefEntry
import java.lang.constant.ClassDesc
import java.lang.constant.ConstantDesc
import java.lang.constant.DirectMethodHandleDesc
import java.lang.constant.DynamicCallSiteDesc
import java.lang.constant.MethodHandleDesc
import java.lang.constant.MethodTypeDesc
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.AccessFlag
import java.lang.reflect.Modifier

fun main() {
    val classFile = ClassFile.of()
    var className = ClassDesc.of("top.dreamlike.jdk22ea", "DemoGenerated")
    val bytes = classFile.build(
            className
    ) {
        it.withInterfaceSymbols(ClassDesc.of(Runnable::class.java.name))
        it.withField("name", ClassDesc.of(MethodHandle::class.java.name)) {
            it.withFlags(AccessFlag.PUBLIC, AccessFlag.STATIC, AccessFlag.FINAL)
        }
        it.withMethodBody("run", MethodTypeDesc.ofDescriptor("()V"), AccessFlags.ofMethod(AccessFlag.PUBLIC).flagsMask()) {
            it.getstatic(ClassDesc.of(System::class.java.name), "out", ClassDesc.of(PrintStream::class.java.name))

            it.ldc(java.lang.String("${Thread.currentThread()}"))
            it.invokevirtual(ClassDesc.of(PrintStream::class.java.name), "println", MethodTypeDesc.ofDescriptor("(Ljava/lang/String;)V"))
            it.return_()
        }

        it.withMethodBody("say", MethodTypeDesc.ofDescriptor("(Ljava/lang/String;)V"), AccessFlags.ofMethod(AccessFlag.PUBLIC).flagsMask()) {
            //  GETSTATIC top/dreamlike/jdk22ea/Main.b : Ljava/lang/invoke/MethodHandle;
            //    ALOAD 0
            //    ILOAD 1
            //    ALOAD 2
            //    ILOAD 3
            //    INVOKEVIRTUAL java/lang/invoke/MethodHandle.invokeExact (Ltop/dreamlike/jdk22ea/Main;ILjava/lang/String;I)I
            //    IRETURN
            it.getstatic(className, "name", ClassDesc.of(MethodHandle::class.java.name))
            it.aload(0)
            it.aload(1)
            it.invokevirtual(ClassDesc.of(MethodHandle::class.java.name), "invokeExact", MethodTypeDesc.ofDescriptor("(Ljava/lang/Object;Ljava/lang/String;)V"))
            it.return_()
        }

        it.withMethodBody("invokeDynamicTest", MethodTypeDesc.ofDescriptor("(Ljava/lang/String;)I"), AccessFlags.ofMethod(AccessFlag.PUBLIC).flagsMask()) {
            //      public static CallSite boostrapMethod(MethodHandles.Lookup lookup, String name, MethodType methodType) throws NoSuchMethodException, IllegalAccessException {
            //        MethodHandle handle = MethodHandles.lookup().findVirtual(String.class, "length", MethodType.methodType(int.class));
            //        System.out.println(name);
            //        return new ConstantCallSite(handle);
            //    }
            it.aload(1)
            it.invokeDynamicInstruction(
                    DynamicCallSiteDesc.of(
                            MethodHandleDesc.ofMethod(
                                    DirectMethodHandleDesc.Kind.STATIC, ClassDesc.of(Main::class.java.name), "boostrapMethod",
                                    MethodTypeDesc.ofDescriptor("(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;")
                            ),
                            "iii name",
                            MethodTypeDesc.ofDescriptor("(Ljava/lang/String;)I")
                    )
            );
            it.ireturn()
        }

        it.withMethodBody("<clinit>", MethodTypeDesc.ofDescriptor("()V"), (AccessFlag.STATIC.mask())) {
            it.getstatic(ClassDesc.of(System::class.java.name), "out", ClassDesc.of(PrintStream::class.java.name))
            it.ldc(java.lang.String("${Thread.currentThread()}"))
            it.invokevirtual(ClassDesc.of(PrintStream::class.java.name), "println", MethodTypeDesc.ofDescriptor("(Ljava/lang/String;)V"))

            it.getstatic(ClassDesc.of(Main::class.java.name), "a", ClassDesc.of(ThreadLocal::class.java.name))
            it.invokevirtual(ClassDesc.of(ThreadLocal::class.java.name), "get", MethodTypeDesc.ofDescriptor("()Ljava/lang/Object;"))
            it.checkcast(ClassDesc.of(MethodHandle::class.java.name))
            it.astore(0)
            it.aload(0)
            it.putstatic(className, "name", ClassDesc.of(MethodHandle::class.java.name))
            it.return_()
        }

        it.withMethodBody("<init>", MethodTypeDesc.ofDescriptor("()V"), Modifier.PUBLIC) {
            it.aload(0)
            it.invokespecial(ClassDesc.of(Any::class.java.name), "<init>", MethodTypeDesc.ofDescriptor("()V"))
            it.return_()
        }

    }
    File("DemoGenerated.class")
            .writeBytes(bytes)
    val anyClass = MethodHandles.lookup().defineClass(bytes)
    val static = MethodHandles.lookup()
            .findStatic(Main::class.java, "mh", MethodType.methodType(Void.TYPE, Any::class.java, String::class.java))
    Main.a.set(static)
    val runnable = anyClass.newInstance() as Runnable
    runnable.run()
    runnable::class.java.getDeclaredMethod("invokeDynamicTest", String::class.java)
            .invoke(runnable, "!23")

}

//    GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
//    LDC "Da"
//    INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V

// ALOAD 0
//    INVOKESPECIAL java/lang/Object.<init> ()V
