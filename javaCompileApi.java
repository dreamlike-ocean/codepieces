public static void main(String[] args) throws Exception {
        String source = "public class Main {" +
                "public static void main(String[] args) {" +
                "System.out.println(\"Hello World!\");" +
                "} " +
                "}";
        FileWriter writer = new FileWriter(new File("Main.class"));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager =  compiler.getStandardFileManager(null, null, null);
        StringSourceJavaObject sourceObject = new StringSourceJavaObject("Main", source);
        List<StringSourceJavaObject> fileObjects =  Arrays.asList(sourceObject);
        var task = compiler.getTask(writer, fileManager, null, null, null, fileObjects);

        boolean result = task.call();
        if (result) {
            System.out.println("Compile succeeded!");
        }  else {
            System.out.println("Compile failed!");
        }
    }

    static class StringSourceJavaObject extends SimpleJavaFileObject {

        private String content = null;
        public StringSourceJavaObject(String name, String content) throws URISyntaxException {
            super(URI.create("string:///" + name.replace('.','/') +  Kind.SOURCE.extension), Kind.SOURCE);
            this.content = content;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return content;
        }
    }
