package com.skcraft.launcher.launch.runtime;

import com.skcraft.launcher.util.Environment;
import com.skcraft.launcher.util.WinRegistry;
import com.sun.jna.platform.win32.WinReg;
import lombok.extern.java.Log;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

@Log
public class WindowsRuntimeFinder implements PlatformRuntimeFinder {

    private static final Map<String, List<JavaRuntime>> registryCache = new HashMap<>();

    @Override
    public Set<File> getLauncherDirectories(Environment env) {
        Set<File> launcherDirs = new HashSet<>();

        // Add FoxFord Java directory as the first priority
        File foxFordJavaDir = new File(System.getenv("APPDATA"), ".foxford\\java");
        if (foxFordJavaDir.exists()) {
            launcherDirs.add(foxFordJavaDir);
            log.info("Found FoxFord Java installation: " + foxFordJavaDir.getAbsolutePath());
        } else {
            log.info("FoxFord Java directory not found.");
        }

        return launcherDirs;
    }

    @Override
    public List<File> getCandidateJavaLocations() {
        List<File> candidateJavaLocations = new ArrayList<>();

        // Add the main .FoxFord\java directory
        File foxFordJavaDir = new File(System.getenv("APPDATA"), ".foxford\\java");
        addJavaLocations(candidateJavaLocations, foxFordJavaDir);

        // Add common Java installation paths
        addJavaLocations(candidateJavaLocations, new File(System.getenv("ProgramFiles"), "Java"));
        addJavaLocations(candidateJavaLocations, new File(System.getenv("ProgramFiles(x86)"), "Java"));
        addJavaLocations(candidateJavaLocations, new File("C:\\Program Files"));

        return candidateJavaLocations;
    }

    private void addJavaLocations(Collection<File> locations, File directory) {
        if (directory.exists() && directory.isDirectory()) {
            File[] javaDirs = directory.listFiles((dir, name) -> name.toLowerCase().startsWith("jdk") && new File(dir, "bin/java.exe").exists());
            if (javaDirs != null) {
                locations.addAll(Arrays.asList(javaDirs));
                for (File javaDir : javaDirs) {
                    log.info("Found Java installation: " + javaDir.getAbsolutePath());
                }
            }
        }
    }

    @Override
    public List<JavaRuntime> getExtraRuntimes() {
        List<JavaRuntime> entries = new ArrayList<>();

        try {
            entries.addAll(getEntriesFromRegistry("SOFTWARE\\JavaSoft\\Java Runtime Environment"));
            entries.addAll(getEntriesFromRegistry("SOFTWARE\\JavaSoft\\Java Development Kit"));
        } catch (Throwable err) {
            log.warning("Failed to read Java locations from registry: " + err.getMessage());
        }

        return entries;
    }

    private List<JavaRuntime> getEntriesFromRegistry(String basePath) {
        if (registryCache.containsKey(basePath)) {
            return registryCache.get(basePath);
        }

        List<JavaRuntime> entries = new ArrayList<>();

        try {
            List<String> subKeys = WinRegistry.readStringSubKeys(WinReg.HKEY_LOCAL_MACHINE, basePath);
            List<CompletableFuture<JavaRuntime>> futures = new ArrayList<>();

            for (String subKey : subKeys) {
                futures.add(CompletableFuture.supplyAsync(() -> getEntryFromRegistry(basePath, subKey)));
            }

            for (CompletableFuture<JavaRuntime> future : futures) {
                JavaRuntime entry = future.get();
                if (entry != null) {
                    entries.add(entry);
                }
            }

            registryCache.put(basePath, entries);
        } catch (InterruptedException | ExecutionException err) {
            log.log(Level.WARNING, "Failed to read Java locations from registry: " + err.getMessage());
        }

        return entries;
    }

    private JavaRuntime getEntryFromRegistry(String basePath, String version) {
        String regPath = basePath + "\\" + version;
        String path = WinRegistry.readString(WinReg.HKEY_LOCAL_MACHINE, regPath, "JavaHome");
        if (path != null && !path.isEmpty()) {
            File dir = new File(path);
            if (dir.exists() && new File(dir, "bin/java.exe").exists()) {
                return new JavaRuntime(dir, version, guessIf64BitWindows(dir));
            }
        }
        return null;
    }

    private boolean guessIf64BitWindows(File path) {
        try {
            String programFilesX86 = System.getenv("ProgramFiles(x86)");
            return programFilesX86 == null || !path.getCanonicalPath().startsWith(new File(programFilesX86).getCanonicalPath());
        } catch (Exception ignored) {
            return false;
        }
    }
}