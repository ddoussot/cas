import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is {@link CheckSpringConfigurationFactories}.
 *
 * @author Misagh Moayyed
 * @since 6.3.0
 */
public class CheckSpringConfigurationFactories {
    public static void main(final String[] args) throws Exception {
        checkPattern(args[0]);
    }

    private static void print(final String message, final Object... args) {
        //CHECKSTYLE:OFF
        System.out.printf(message, args);
        System.out.println();
        //CHECKSTYLE:ON
    }

    private static String readFile(final Path file) {
        try {
            return Files.readString(file);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean checkProjectContainsSpringConfigurations(String projectPath,
                                                                    File springFactoriesFile) throws Exception {
        if (springFactoriesFile.exists()) {
            return true;
        }
        final var pass = new AtomicBoolean(true);
        Files.walk(Paths.get(projectPath))
            .filter(Files::isRegularFile)
            .forEach(file -> {
                if (file.toFile().getName().endsWith("Configuration.java")
                    && readFile(file).contains("@Configuration")) {
                    print("Configuration class %s is missing from %s", file, springFactoriesFile);
                    pass.set(false);
                }
            });
        if (!pass.get()) {
            print("Project %s is missing a spring.factories file at %s", projectPath, springFactoriesFile);
        }
        return pass.get();
    }

    private static boolean checkForSpringConfigurationFactories(String projectPath,
                                                                String configurations,
                                                                File springFactoriesFile) {
        var classes = configurations.split(",");
        for (var it : Arrays.asList(classes)) {
            var sourcePath = "/src/main/java/".replace("/", String.valueOf(File.separator));
            var clazz = projectPath + sourcePath + it.replace(".", String.valueOf(File.separator)) + ".java";
            var configurationFile = new File(clazz);
            if (!configurationFile.exists()) {
                print("Spring configuration class %s does not exist in %s", clazz, springFactoriesFile);
                return false;
            }
        }
        return true;
    }

    protected static void checkPattern(final String arg) throws IOException {
        final var count = new AtomicInteger(0);

        Files.walk(Paths.get(arg))
            .filter(file -> Files.isDirectory(file) && file.toFile().getAbsolutePath().endsWith("src/main/resources/META-INF"))
            .forEach(dir -> {
                try {
                    var projectPath = dir.getParent().getParent().getParent().getParent().toFile().getPath();
                    var springFactoriesFile = new File(dir.toFile(), "spring.factories");
                    if (springFactoriesFile.exists()) {
                        var properties = new Properties();

                        properties.load(new FileReader(springFactoriesFile));

                        if (properties.isEmpty()) {
                            print("spring.factories file %s is empty", springFactoriesFile);
                            count.incrementAndGet();
                        }

                        if (properties.containsKey("org.springframework.cloud.bootstrap.BootstrapConfiguration")) {
                            var classes = (String) properties.get("org.springframework.cloud.bootstrap.BootstrapConfiguration");
                            if (!checkForSpringConfigurationFactories(projectPath, classes, springFactoriesFile)) {
                                count.incrementAndGet();
                            }
                        }
                        if (properties.containsKey("org.springframework.boot.autoconfigure.EnableAutoConfiguration")) {
                            var classes = (String) properties.get("org.springframework.boot.autoconfigure.EnableAutoConfiguration");
                            if (!checkForSpringConfigurationFactories(projectPath, classes, springFactoriesFile)) {
                                count.incrementAndGet();
                            }
                        }
                    } else if (!checkProjectContainsSpringConfigurations(projectPath, springFactoriesFile)) {
                        count.incrementAndGet();
                    }
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            });

        if (count.intValue() > 0) {
            System.exit(1);
        }
    }
}
