package com.skcraft.launcher.launch.runtime;

import com.skcraft.launcher.util.Environment;
import com.skcraft.launcher.util.Platform;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Scans Minecraft bundled Java directories
 */
public class MinecraftJavaFinder {
    public static CompletableFuture<List<JavaRuntime>> scanLauncherDirectoriesAsync(Environment env, Collection<File> launcherDirs) {
        return CompletableFuture.supplyAsync(() -> scanLauncherDirectories(env, launcherDirs));
    }

    public static List<JavaRuntime> scanLauncherDirectories(Environment env, Collection<File> launcherDirs) {
        ArrayList<JavaRuntime> entries = new ArrayList<>();

        // 1. Search in .FoxFord\java with the highest priority
        File foxFordJavaDir = new File(System.getenv("APPDATA"), ".FoxFord\\java");
        addJavaLocations(entries, foxFordJavaDir, env);

        // 2. Search in C:\Program Files\Eclipse Adoptium
        File adoptiumDir = new File("C:\\Program Files\\Eclipse Adoptium");
        addJavaLocations(entries, adoptiumDir, env);

        // 3. Process other directories
        for (File install : launcherDirs) {
            File runtimes = new File(install, "runtime");
            File[] runtimeList = runtimes.listFiles();
            if (runtimeList != null) {
                for (File potential : runtimeList) {
                    JavaRuntime runtime = scanPotentialRuntime(env, potential);
                    if (runtime != null) {
                        entries.add(runtime);
                    }
                }
            }
        }

        return entries;
    }

    private static void addJavaLocations(List<JavaRuntime> entries, File directory, Environment env) {
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles((dir, name) -> name.toLowerCase().equals("bin") && new File(dir, "java.exe").exists());
            if (files != null && files.length > 0) {
                // Found a valid Java installation
                JavaRuntime runtime = scanPotentialRuntime(env, directory);
                if (runtime != null) {
                    entries.add(runtime);
                }
            }
        }
    }

    private static JavaRuntime scanPotentialRuntime(Environment env, File potential) {
        String runtimeName = potential.getName();
        if (runtimeName.startsWith("jre-x")) {
            boolean is64Bit = runtimeName.equals("jre-x64");

            JavaReleaseFile release = JavaReleaseFile.parseFromRelease(potential);
            String version = release != null ? release.getVersion() : null;

            JavaRuntime runtime = new JavaRuntime(potential.getAbsoluteFile(), version, is64Bit);
            runtime.setMinecraftBundled(true);
            return runtime;
        } else {
            String[] children = potential.list((dir, name) -> new File(dir, name).isDirectory());
            if (children == null || children.length != 1) return null;
            String platformName = children[0];

            File javaDir = new File(potential, String.format("%s/%s", platformName, runtimeName));
            if (env.getPlatform() == Platform.MAC_OS_X) {
                javaDir = new File(javaDir, "jre.bundle/Contents/Home");
            }

            JavaReleaseFile release = JavaReleaseFile.parseFromRelease(javaDir);
            if (release == null) {
                return null;
            }

            JavaRuntime runtime = new JavaRuntime(javaDir.getAbsoluteFile(), release.getVersion(), release.isArch64Bit());
            runtime.setMinecraftBundled(true);
            return runtime;
        }
    }
}
