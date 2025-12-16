package top.bearcabbage.twodimensional_bedwars.mechanic;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class InternalAdapter {
    
    public static void restoreMap(String backupPath, String worldPath) {
        try {
            File worldDir = new File(worldPath);
            if (worldDir.exists()) {
                deleteDirectory(worldDir.toPath());
            }
            // Unzip logic (Java standard lib doesn't have simple one-line unzip, usually assumes external lib or complex helper)
            // For this minimal impl, we'll placeholder the unzip or copy logic
            // copyDirectory(new File(backupPath).toPath(), worldDir.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void deleteDirectory(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
