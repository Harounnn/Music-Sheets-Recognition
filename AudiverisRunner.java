import java.io.*;
import java.nio.file.*;
import java.util.*;

public class AudiverisRunner {
    public static void main(String[] args) throws Exception {
        Path input = Paths.get("data/sheet3.jpg");
        Path outputDir = Paths.get("output");
        Files.createDirectories(outputDir);

        List<String> cmd = Arrays.asList(
            "C:\\Audiveris\\Audiveris.exe",
            "-batch",
            "-export",
            "-option", "org.audiveris.omr.sheet.BookManager.useOpus=true",
            "-output", outputDir.toString(),
            input.toString()
        );

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader in = new BufferedReader(
                 new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Audiveris failed with exit code " + exitCode);
        }

        // Process the single .mxl file
        Files.walk(outputDir)
            .filter(path -> path.toString().endsWith(".mxl"))
            .findFirst()
            .ifPresent(path -> {
                System.out.println("✅ Created: " + path.toAbsolutePath());
                // Add your processing logic here
            });
    }
}
