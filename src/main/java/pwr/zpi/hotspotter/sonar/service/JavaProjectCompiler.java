package pwr.zpi.hotspotter.sonar.service;

import lombok.extern.slf4j.Slf4j;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class JavaProjectCompiler {
    public boolean isJavaProject(Path projectPath) {
        return Files.exists(projectPath.resolve("pom.xml"))
                || Files.exists(projectPath.resolve("build.gradle"))
                || Files.exists(projectPath.resolve("build.gradle.kts"))
                || Files.exists(projectPath.resolve(Path.of("src", "main", "java")));
    }

    public Optional<Path> findCommonJavaSourceRoot(Path projectPath) throws IOException {
        if (projectPath == null || !Files.exists(projectPath)) {
            return Optional.empty();
        }

        if (isJavaProject(projectPath)) {
            return Optional.of(projectPath);
        }

        Optional<Path> pomDir = findAnyPomDirectory(projectPath);
        if (pomDir.isPresent()) {
            return pomDir;
        }

        Optional<Path> gradleDir = findAnyBuildGradleDirectory(projectPath);
        if (gradleDir.isPresent()) {
            return gradleDir;
        }

        List<Path> javaParents;
        try (Stream<Path> walk = Files.walk(projectPath)) {
            javaParents = walk
                    .filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".java"))
                    .map(Path::toAbsolutePath)
                    .map(Path::normalize)
                    .map(Path::getParent)
                    .distinct()
                    .toList();
        }

        if (javaParents.isEmpty()) {
            return Optional.empty();
        }

        Path candidate = javaParents.getFirst();
        while (candidate != null) {
            final Path cand = candidate;
            boolean allUnder = javaParents.stream().allMatch(p -> p.startsWith(cand));
            if (allUnder) {
                return Optional.of(candidate);
            }
            candidate = candidate.getParent();
        }

        return Optional.empty();
    }

    public static Optional<Path> findAnyPomDirectory(Path start) throws IOException {
        if (start == null || !Files.exists(start)) return Optional.empty();
        try (Stream<Path> stream = Files.walk(start)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> "pom.xml".equals(p.getFileName().toString()))
                    .findFirst()
                    .map(Path::getParent);
        }
    }

    public static Optional<Path> findAnyBuildGradleDirectory(Path start) throws IOException {
        if (start == null || !Files.exists(start)) return Optional.empty();
        try (Stream<Path> stream = Files.walk(start)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> "build.gradle".equals(p.getFileName().toString())
                            || "build.gradle.kts".equals(p.getFileName().toString()))
                    .findFirst()
                    .map(Path::getParent);
        }
    }

    public List<String> compileJavaProject(Path projectPath) throws Exception {
        log.info("Starting compilation for project: {}", projectPath);

        if (!Files.exists(projectPath)) {
            throw new IllegalArgumentException("Project path does not exist: " + projectPath);
        }

        boolean isMaven = Files.exists(projectPath.resolve("pom.xml"));
        boolean isGradle = Files.exists(projectPath.resolve("build.gradle"))
                || Files.exists(projectPath.resolve("build.gradle.kts"));

        if (isMaven) {
            log.info("Detected Maven project.");
            return compileMavenProject(projectPath);
        } else if (isGradle) {
            log.info("Detected Gradle project.");
            return compileGradleProject(projectPath);
        } else {
            log.info("Detected plain Java project (no pom.xml or build.gradle).");
            return compilePlainJavaProject(projectPath);
        }
    }

    public List<String> compileMavenProject(Path projectPath) throws Exception {
        log.info("Invoking external mvn for project: {}", projectPath);
        ProcessBuilder pb = new ProcessBuilder("mvn", "-B", "clean", "compile");
        pb.directory(projectPath.toFile());
        pb.redirectErrorStream(true);

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new Exception("Failed to start external mvn executable. Ensure Maven is installed and 'mvn' is in PATH.", e);
        }

        int exit = process.waitFor();
        if (exit != 0) {
            throw new RuntimeException("Failed to start external mvn executable. Exit value is " + exit);
        }

        try (Stream<Path> walk = Files.walk(projectPath)) {
            return walk.filter(p -> p.endsWith(Path.of("target", "classes")))
                    .map(Path::toString)
                    .collect(Collectors.toList());
        }
    }

    private List<String> compileGradleProject(Path projectPath) throws Exception {
        try (ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(projectPath.toFile())
                .connect()) {
            connection.newBuild()
                    .forTasks("classes")
                    .run();
        } catch (Exception e) {
            throw new Exception("Gradle build failed: " + e.getMessage(), e);
        }

        try (Stream<Path> walk = Files.walk(projectPath)) {
            return walk.filter(p -> p.endsWith(Path.of("build", "classes", "java", "main"))
                            || p.endsWith(Path.of("build", "classes", "kotlin", "main"))
                            || p.endsWith(Path.of("build", "classes", "groovy", "main")))
                    .map(Path::toString)
                    .collect(Collectors.toList());
        }
    }

    private List<String> compilePlainJavaProject(Path projectPath) throws Exception {
        List<Path> srcDirs;
        try (Stream<Path> walk = Files.walk(projectPath)) {
            srcDirs = walk
                    .filter(p -> Files.isDirectory(p) && p.endsWith(Path.of("src", "main", "java")))
                    .toList();
        }

        if (srcDirs.isEmpty()) {
            throw new Exception("No source directories found in plain Java project: " + projectPath);
        }

        List<String> binaries = new ArrayList<>();

        for (Path srcDir : srcDirs) {
            Path moduleRoot = srcDir.getParent().getParent().getParent();
            Path targetClasses = moduleRoot.resolve("target").resolve("classes");
            Files.createDirectories(targetClasses);

            List<String> sources;
            try (Stream<Path> walk = Files.walk(srcDir)) {
                sources = walk
                        .map(Path::toString)
                        .filter(string -> string.endsWith(".java"))
                        .toList();
            }

            javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                throw new IllegalStateException("No Java compiler available. Run with JDK, not JRE!");
            }

            List<String> args = new ArrayList<>();
            args.add("-d");
            args.add(targetClasses.toString());
            args.addAll(sources);

            int result = compiler.run(null, null, null, args.toArray(new String[0]));
            if (result != 0) {
                throw new Exception("javac compilation failed for: " + moduleRoot);
            }

            binaries.add(targetClasses.toString());
        }

        return binaries;
    }

}