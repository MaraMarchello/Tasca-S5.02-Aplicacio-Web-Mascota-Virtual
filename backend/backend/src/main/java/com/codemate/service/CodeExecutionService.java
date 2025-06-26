package com.codemate.service;

import com.codemate.exception.AIServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.tools.*;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeExecutionService {

    private static final int EXECUTION_TIMEOUT_SECONDS = 10;
    private static final int MAX_OUTPUT_LENGTH = 10000;
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("public\\s+class\\s+(\\w+)");
    private static final Pattern MAIN_METHOD_PATTERN = Pattern.compile("public\\s+static\\s+void\\s+main\\s*\\(");

    /**
     * Execute Java code and return the result
     */
    public CodeExecutionResult executeJavaCode(String code, String input) {
        log.info("Executing Java code");
        
        try {
            // Validate and prepare code
            String validatedCode = validateAndPrepareCode(code);
            String className = extractClassName(validatedCode);
            
            // Compile the code
            CompilationResult compilationResult = compileJavaCode(validatedCode, className);
            if (!compilationResult.isSuccess()) {
                return CodeExecutionResult.builder()
                        .success(false)
                        .output("")
                        .error(compilationResult.getErrorMessage())
                        .executionTime(0)
                        .build();
            }
            
            // Execute the compiled code
            return executeCompiledCode(className, compilationResult.getClassLoader(), input);
            
        } catch (Exception e) {
            log.error("Error executing Java code", e);
            return CodeExecutionResult.builder()
                    .success(false)
                    .output("")
                    .error("Execution error: " + e.getMessage())
                    .executionTime(0)
                    .build();
        }
    }

    /**
     * Validate and prepare code for execution
     */
    private String validateAndPrepareCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new AIServiceException("Code cannot be empty");
        }
        
        // Remove package declarations for security
        code = code.replaceAll("package\\s+[^;]+;", "");
        
        // Check for prohibited imports and statements
        validateCodeSecurity(code);
        
        // Ensure code has a main method
        if (!MAIN_METHOD_PATTERN.matcher(code).find()) {
            // If no main method, wrap in a simple class with main
            if (!code.contains("class")) {
                code = "public class CodeExecution {\n" +
                       "    public static void main(String[] args) {\n" +
                       "        " + code + "\n" +
                       "    }\n" +
                       "}";
            }
        }
        
        return code;
    }

    /**
     * Validate code for security concerns
     */
    private void validateCodeSecurity(String code) {
        // List of prohibited patterns for security
        String[] prohibitedPatterns = {
                "Runtime\\.getRuntime\\(\\)",
                "ProcessBuilder",
                "System\\.exit\\(",
                "File\\(",
                "FileInputStream",
                "FileOutputStream",
                "FileReader",
                "FileWriter",
                "Socket\\(",
                "ServerSocket\\(",
                "Thread\\(",
                "ThreadPoolExecutor",
                "ExecutorService",
                "Class\\.forName\\(",
                "java\\.lang\\.reflect",
                "sun\\.",
                "com\\.sun\\.",
                "java\\.io\\.File",
                "java\\.net\\.",
                "java\\.nio\\.file\\.",
                "javax\\.script\\.",
                "java\\.lang\\.management\\.",
                "java\\.security\\.",
                "java\\.util\\.concurrent\\.ThreadPoolExecutor"
        };
        
        for (String pattern : prohibitedPatterns) {
            if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(code).find()) {
                throw new AIServiceException("Code contains prohibited operations: " + pattern);
            }
        }
        
        // Check for infinite loops (basic detection)
        if (code.contains("while(true)") || code.contains("while (true)") || 
            code.contains("for(;;)") || code.contains("for (;;)")) {
            throw new AIServiceException("Infinite loops are not allowed");
        }
    }

    /**
     * Extract class name from code
     */
    private String extractClassName(String code) {
        Matcher matcher = CLASS_NAME_PATTERN.matcher(code);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "CodeExecution"; // Default class name
    }

    /**
     * Compile Java code in memory
     */
    private CompilationResult compileJavaCode(String code, String className) {
        try {
            // Get the Java compiler
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                throw new AIServiceException("Java compiler not available");
            }
            
            // Create in-memory file manager
            InMemoryFileManager fileManager = new InMemoryFileManager(
                    compiler.getStandardFileManager(null, null, null));
            
            // Create source file object
            JavaFileObject sourceFile = new InMemoryJavaFileObject(className, code);
            
            // Compilation options
            List<String> options = Arrays.asList("-cp", System.getProperty("java.class.path"));
            
            // Compile
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null, fileManager, diagnostics, options, null, Arrays.asList(sourceFile));
            
            boolean success = task.call();
            
            if (success) {
                return CompilationResult.builder()
                        .success(true)
                        .classLoader(fileManager.getClassLoader())
                        .build();
            } else {
                StringBuilder errorMessage = new StringBuilder();
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    errorMessage.append("Line ").append(diagnostic.getLineNumber())
                              .append(": ").append(diagnostic.getMessage(null)).append("\n");
                }
                return CompilationResult.builder()
                        .success(false)
                        .errorMessage(errorMessage.toString())
                        .build();
            }
            
        } catch (Exception e) {
            log.error("Compilation error", e);
            return CompilationResult.builder()
                    .success(false)
                    .errorMessage("Compilation failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Execute compiled code with timeout
     */
    private CodeExecutionResult executeCompiledCode(String className, ClassLoader classLoader, String input) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Load the class
            Class<?> clazz = classLoader.loadClass(className);
            Method mainMethod = clazz.getMethod("main", String[].class);
            
            // Capture output
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            
            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;
            
            // Set up input if provided
            InputStream originalIn = System.in;
            if (input != null && !input.isEmpty()) {
                System.setIn(new ByteArrayInputStream(input.getBytes()));
            }
            
            try {
                System.setOut(new PrintStream(outputStream));
                System.setErr(new PrintStream(errorStream));
                
                // Execute with timeout
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<Void> future = executor.submit(() -> {
                    try {
                        mainMethod.invoke(null, (Object) new String[0]);
                        return null;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                
                try {
                    future.get(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    future.cancel(true);
                    throw new AIServiceException("Code execution timed out after " + 
                                               EXECUTION_TIMEOUT_SECONDS + " seconds");
                } finally {
                    executor.shutdown();
                }
                
            } finally {
                System.setOut(originalOut);
                System.setErr(originalErr);
                System.setIn(originalIn);
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            String output = outputStream.toString();
            String error = errorStream.toString();
            
            // Limit output length
            if (output.length() > MAX_OUTPUT_LENGTH) {
                output = output.substring(0, MAX_OUTPUT_LENGTH) + "\n... (output truncated)";
            }
            
            boolean success = error.isEmpty();
            String finalOutput = success ? output : error;
            
            return CodeExecutionResult.builder()
                    .success(success)
                    .output(finalOutput)
                    .error(success ? null : error)
                    .executionTime(executionTime)
                    .build();
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Code execution error", e);
            
            return CodeExecutionResult.builder()
                    .success(false)
                    .output("")
                    .error("Runtime error: " + e.getMessage())
                    .executionTime(executionTime)
                    .build();
        }
    }

    // Inner classes for in-memory compilation
    
    private static class InMemoryJavaFileObject extends SimpleJavaFileObject {
        private final String code;
        
        protected InMemoryJavaFileObject(String className, String code) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension),
                  Kind.SOURCE);
            this.code = code;
        }
        
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
    
    private static class InMemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        private final Map<String, ByteArrayOutputStream> classBytes = new HashMap<>();
        private final InMemoryClassLoader classLoader = new InMemoryClassLoader();
        
        protected InMemoryFileManager(JavaFileManager fileManager) {
            super(fileManager);
        }
        
        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className,
                                                  JavaFileObject.Kind kind, FileObject sibling) {
            return new InMemoryClassFile(className, classBytes);
        }
        
        public ClassLoader getClassLoader() {
            return classLoader;
        }
        
        private class InMemoryClassLoader extends ClassLoader {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                ByteArrayOutputStream baos = classBytes.get(name);
                if (baos == null) {
                    throw new ClassNotFoundException(name);
                }
                byte[] bytes = baos.toByteArray();
                return defineClass(name, bytes, 0, bytes.length);
            }
        }
    }
    
    private static class InMemoryClassFile extends SimpleJavaFileObject {
        private final String className;
        private final Map<String, ByteArrayOutputStream> classBytes;
        
        protected InMemoryClassFile(String className, Map<String, ByteArrayOutputStream> classBytes) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.CLASS.extension),
                  Kind.CLASS);
            this.className = className;
            this.classBytes = classBytes;
        }
        
        @Override
        public OutputStream openOutputStream() {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            classBytes.put(className, baos);
            return baos;
        }
    }
    
    // Result classes
    
    public static class CodeExecutionResult {
        private boolean success;
        private String output;
        private String error;
        private long executionTime;
        
        public static CodeExecutionResultBuilder builder() {
            return new CodeExecutionResultBuilder();
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getOutput() { return output; }
        public String getError() { return error; }
        public long getExecutionTime() { return executionTime; }
        
        public static class CodeExecutionResultBuilder {
            private boolean success;
            private String output;
            private String error;
            private long executionTime;
            
            public CodeExecutionResultBuilder success(boolean success) {
                this.success = success;
                return this;
            }
            
            public CodeExecutionResultBuilder output(String output) {
                this.output = output;
                return this;
            }
            
            public CodeExecutionResultBuilder error(String error) {
                this.error = error;
                return this;
            }
            
            public CodeExecutionResultBuilder executionTime(long executionTime) {
                this.executionTime = executionTime;
                return this;
            }
            
            public CodeExecutionResult build() {
                CodeExecutionResult result = new CodeExecutionResult();
                result.success = this.success;
                result.output = this.output;
                result.error = this.error;
                result.executionTime = this.executionTime;
                return result;
            }
        }
    }
    
    private static class CompilationResult {
        private boolean success;
        private String errorMessage;
        private ClassLoader classLoader;
        
        public static CompilationResultBuilder builder() {
            return new CompilationResultBuilder();
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public ClassLoader getClassLoader() { return classLoader; }
        
        public static class CompilationResultBuilder {
            private boolean success;
            private String errorMessage;
            private ClassLoader classLoader;
            
            public CompilationResultBuilder success(boolean success) {
                this.success = success;
                return this;
            }
            
            public CompilationResultBuilder errorMessage(String errorMessage) {
                this.errorMessage = errorMessage;
                return this;
            }
            
            public CompilationResultBuilder classLoader(ClassLoader classLoader) {
                this.classLoader = classLoader;
                return this;
            }
            
            public CompilationResult build() {
                CompilationResult result = new CompilationResult();
                result.success = this.success;
                result.errorMessage = this.errorMessage;
                result.classLoader = this.classLoader;
                return result;
            }
        }
    }
} 