package com.skcraft.launcher.launch.runtime;

import com.google.common.collect.Lists;
import com.skcraft.launcher.util.Environment;
import com.skcraft.launcher.util.WinRegistry;
import com.sun.jna.platform.win32.WinReg;
import lombok.extern.java.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log
public class WindowsRuntimeFinder implements PlatformRuntimeFinder {
    @Override
    public Set<File> getLauncherDirectories(Environment env) {
        HashSet<File> launcherDirs = new HashSet<>();

        // Add FoxFord Java directory as the first priority
        File foxFordJavaDir = new File(System.getenv("APPDATA"), ".FoxFord\\java");
        log.info("Checking for FoxFord Java installation in: " + foxFordJavaDir.getAbsolutePath());
        if (foxFordJavaDir.exists()) {
            launcherDirs.add(foxFordJavaDir);
            log.info("Found FoxFord Java installation.");
        } else {
            log.info("FoxFord Java directory not found.");
        }

        try {
            CompletableFuture.runAsync(() -> {
                try {
                    String launcherPath = WinRegistry.readString(WinReg.HKEY_CURRENT_USER,
                            "SOFTWARE\\Mojang\\InstalledProducts\\Minecraft Launcher", "InstallLocation");

                    log.info("Checking for Minecraft Launcher installation in: " + launcherPath);
                    if (launcherPath != null && !launcherPath.isEmpty()) {
                        launcherDirs.add(new File(launcherPath));
                        log.info("Found Minecraft Launcher installation.");
                    } else {
                        log.info("Minecraft Launcher installation not found in the registry.");
                    }
                } catch (Throwable err) {
                    log.log(Level.WARNING, "Failed to read launcher location from registry", err);
                }
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            log.log(Level.WARNING, "Error while reading launcher location from registry asynchronously", e);
        }

        String programFiles = Objects.equals(env.getArchBits(), "64")
                ? System.getenv("ProgramFiles(x86)")
                : System.getenv("ProgramFiles");

        // Mojang likes to move the java runtime directory
        log.info("Checking for Minecraft installation in: " + programFiles);
        addIfDirectoryExists(launcherDirs, new File(programFiles, "Minecraft"));
        addIfDirectoryExists(launcherDirs, new File(programFiles, "Minecraft Launcher"));
        addIfDirectoryExists(launcherDirs, new File(System.getenv("LOCALAPPDATA"), "Packages\\Microsoft.4297127D64EC6_8wekyb3d8bbwe\\LocalCache\\Local"));

        return launcherDirs;
    }

    @Override
    public List<File> getCandidateJavaLocations() {
        ArrayList<File> candidateJavaLocations = Lists.newArrayList();

        // Add the main .FoxFord\java directory
        File foxFordJavaDir = new File(System.getenv("APPDATA"), ".FoxFord\\java");
        log.info("Checking for FoxFord Java installations in: " + foxFordJavaDir.getAbsolutePath());
        addJavaLocations(candidateJavaLocations, foxFordJavaDir);

        // Add common Java installation paths
        addJavaLocations(candidateJavaLocations, new File(System.getenv("ProgramFiles"), "Java"));
        addJavaLocations(candidateJavaLocations, new File(System.getenv("ProgramFiles(x86)"), "Java"));
        addJavaLocations(candidateJavaLocations, new File("C:\\Program Files"));

        return candidateJavaLocations;
    }

    // Add Java locations from the given directory
    private static void addJavaLocations(Collection<File> locations, File directory) {
        log.info("Checking for Java installations in: " + directory.getAbsolutePath());
        if (directory.exists() && directory.isDirectory()) {
            File[] javaDirs = directory.listFiles((dir, name) -> name.toLowerCase().startsWith("jdk") && new File(dir, "bin/java.exe").exists());
            if (javaDirs != null) {
                locations.addAll(Arrays.asList(javaDirs));
                for (File javaDir : javaDirs) {
                    log.info("Found Java installation: " + javaDir.getAbsolutePath());
                }
            } else {
                log.info("No Java installations found.");
            }
        } else {
            log.info("Directory not found.");
        }
    }

    // Helper method to add directory to the collection if it exists
    private static void addIfDirectoryExists(Collection<File> collection, File directory) {
        log.info("Checking for directory: " + directory.getAbsolutePath());
        if (directory.exists() && directory.isDirectory()) {
            collection.add(directory);
            log.info("Found directory: " + directory.getAbsolutePath());
        } else {
            log.info("Directory not found.");
        }
    }

    @Override
    public List<JavaRuntime> getExtraRuntimes() {
        ArrayList<JavaRuntime> entries = Lists.newArrayList();

        getEntriesFromRegistry(entries, "SOFTWARE\\JavaSoft\\Java Runtime Environment");
        getEntriesFromRegistry(entries, "SOFTWARE\\JavaSoft\\Java Development Kit");
        getEntriesFromRegistry(entries, "SOFTWARE\\JavaSoft\\JDK");

        return entries;
    }



    private static void getEntriesFromRegistry(Collection<JavaRuntime> entries, String basePath)
            throws IllegalArgumentException {
        try {
            List<String> subKeys = WinRegistry.readStringSubKeys(WinReg.HKEY_LOCAL_MACHINE, basePath);
            for (String subKey : subKeys) {
                JavaRuntime entry = getEntryFromRegistry(basePath, subKey);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        } catch (Throwable err) {
            log.log(Level.INFO, "Failed to read Java locations from registry in " + basePath);
        }
    }

    private static JavaRuntime getEntryFromRegistry(String basePath, String version) {
        String regPath = basePath + "\\" + version;
        String path = WinRegistry.readString(WinReg.HKEY_LOCAL_MACHINE, regPath, "JavaHome");
        File dir = new File(path);
        if (dir.exists() && new File(dir, "bin/java.exe").exists()) {
            return new JavaRuntime(dir, version, guessIf64BitWindows(dir));
        } else {
            return null;
        }
    }

    private static boolean guessIf64BitWindows(File path) {
        try {
            String programFilesX86 = System.getenv("ProgramFiles(x86)");
            return programFilesX86 == null || !path.getCanonicalPath().startsWith(new File(programFilesX86).getCanonicalPath());
        } catch (IOException ignored) {
            return false;
        }
    }
}
