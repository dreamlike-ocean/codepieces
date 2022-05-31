
import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Main{
    public static String string = " ad";

    public static void main(String[] args) throws Exception {
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager stdManager = jc.getStandardFileManager(null, null, null);
        try (MemoryJavaFileManager manager = new MemoryJavaFileManager(stdManager)){
            JavaFileObject javaFileObject = manager.makeStringSource("R", getJavaCode());
            JavaFileObject javaFileObject1 = manager.makeStringSource("Run", getJ1());
            JavaCompiler.CompilationTask task = jc.getTask(null, manager, null, null, null, Arrays.asList(javaFileObject,javaFileObject1));
            task.call();
            ClassLoader loader = new ProxyClassLoader(Thread.currentThread().getContextClassLoader(), manager.getClassBytes());
            Class<?> r = loader.loadClass("R");
            Object instance = r.newInstance();
            Method method = r.getMethod("run");
            method.invoke(instance );
        }
    }

    public static String getJavaCode(){
        return "import com.company.Run;\n" +
                "\n" +
                "public class R implements Run {\n" +
                "\n" +
                "}";
    }

    public static String getJ1(){
        return "package com.company;\n" +
                "\n" +
                "public interface Run {\n" +
                "    default void run(){\n" +
                "        System.out.println(\"run\");\n" +
                "    }\n" +
                "}\n";
    }

}

class ProxyClassLoader extends ClassLoader {
    private Map<String, byte[]> mClassBytes;

    public ProxyClassLoader(ClassLoader parent, Map<String, byte[]> classBytes) {
        super(parent);

        if (classBytes == null) {
            mClassBytes = new HashMap<>();
        } else {
            mClassBytes = classBytes;
        }
    }

    public void addClassBytes(String key, byte[] bytes) {
        mClassBytes.put(key, bytes);
    }

    public void addClassBytesAll(Map<String, byte[]> classBytes) {
        mClassBytes.putAll(classBytes);
    }


    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = mClassBytes.remove(name);
        if (bytes != null) {
            return defineClass(name, bytes, 0, bytes.length);
        }

        return super.findClass(name);
    }
}
class MemoryJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {

    // compiled classes in bytes:
    final Map<String, byte[]> classBytes = new HashMap<String, byte[]>();

    MemoryJavaFileManager(JavaFileManager fileManager) {
        super(fileManager);
    }

    public Map<String, byte[]> getClassBytes() {
        return new HashMap<String, byte[]>(this.classBytes);
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
        classBytes.clear();
    }

    @Override
    public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className, JavaFileObject.Kind kind,
                                               FileObject sibling) throws IOException {
        if (kind == JavaFileObject.Kind.CLASS) {
            return new MemoryOutputJavaFileObject(className);
        } else {
            return super.getJavaFileForOutput(location, className, kind, sibling);
        }
    }

    JavaFileObject makeStringSource(String name, String code) {
        return new MemoryInputJavaFileObject(name, code);
    }

    static class MemoryInputJavaFileObject extends SimpleJavaFileObject {

        final String code;

        MemoryInputJavaFileObject(String name, String code) {
            super(URI.create("string:///" + name.replace('.','/') +  Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

    class MemoryOutputJavaFileObject extends SimpleJavaFileObject {
        final String name;

        MemoryOutputJavaFileObject(String name) {
            super(URI.create("string:///" + name), Kind.CLASS);
            this.name = name;
        }

        @Override
        public OutputStream openOutputStream() {
            return new FilterOutputStream(new ByteArrayOutputStream()) {
                @Override
                public void close() throws IOException {
                    out.close();
                    ByteArrayOutputStream bos = (ByteArrayOutputStream) out;
                    classBytes.put(name, bos.toByteArray());
                }
            };
        }

    }
}
