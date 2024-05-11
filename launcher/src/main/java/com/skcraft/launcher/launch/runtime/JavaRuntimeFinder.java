/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.launch.runtime;

import com.skcraft.launcher.model.minecraft.JavaVersion;
import com.skcraft.launcher.util.Environment;
import lombok.extern.java.Log;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Finds the best Java runtime to use.
 */
@Log
public final class JavaRuntimeFinder {

    private JavaRuntimeFinder() {
    }

    /**
     * Get all available Java runtimes on the system
     *
     * @return List of available Java runtimes sorted by newest first
     */
    public static List<JavaRuntime> getAvailableRuntimes() {
        Environment env = Environment.getInstance();
        PlatformRuntimeFinder runtimeFinder = getRuntimeFinder(env);

        if (runtimeFinder == null) {
            return Collections.emptyList();
        }

        // Add Minecraft javas asynchronously
        CompletableFuture<List<JavaRuntime>> mcRuntimesFuture = CompletableFuture.supplyAsync(() ->
                MinecraftJavaFinder.scanLauncherDirectories(env, runtimeFinder.getLauncherDirectories(env)));

        // Add system Javas asynchronously
        CompletableFuture<List<JavaRuntime>> systemRuntimesFuture = CompletableFuture.supplyAsync(() ->
                runtimeFinder.getCandidateJavaLocations().stream()
                        .map(JavaRuntimeFinder::getRuntimeFromPath)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));

        // Wait for both tasks to complete
        CompletableFuture<List<JavaRuntime>> combinedFuture = mcRuntimesFuture.thenCombineAsync(systemRuntimesFuture,
                (mcRuntimes, systemRuntimes) -> {
                    Set<JavaRuntime> entries = new HashSet<>(mcRuntimes);
                    entries.addAll(systemRuntimes);
                    entries.addAll(runtimeFinder.getExtraRuntimes());
                    return new ArrayList<>(entries);
                });

        try {
            // Wait for both tasks to complete
            return combinedFuture.get().stream().sorted().collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            log.log(java.util.logging.Level.WARNING, "Error while getting available runtimes", e);
            return Collections.emptyList();
        }
    }

    /**
     * Find the best runtime for a given Java version
     *
     * @param targetVersion Version to match
     * @return Java runtime if available, empty Optional otherwise
     */
    public static Optional<JavaRuntime> findBestJavaRuntime(JavaVersion targetVersion) {
        List<JavaRuntime> entries = getAvailableRuntimes();

        return entries.stream().sorted()
                .filter(runtime -> runtime.getMajorVersion() == targetVersion.getMajorVersion())
                .findFirst();
    }

    public static Optional<JavaRuntime> findAnyJavaRuntime() {
        return getAvailableRuntimes().stream().sorted().findFirst();
    }

    public static JavaRuntime getRuntimeFromPath(String path) {
        return getRuntimeFromPath(new File(path));
    }

    public static JavaRuntime getRuntimeFromPath(File target) {
        // Normalize target to root first
        target = normalizePath(target);

        // Find the release file
        File releaseFile = new File(target, "release");
        if (!releaseFile.isFile()) {
            releaseFile = new File(target, "jre/release");
        }

        // Find the bin folder
        File binFolder = new File(target, "bin");
        if (!binFolder.isDirectory()) {
            binFolder = new File(target, "jre/bin");
        }

        if (!binFolder.isDirectory()) {
            // No bin folder, this isn't a usable install
            return null;
        }

        JavaReleaseFile release = JavaReleaseFile.parseFromRelease(releaseFile.getParentFile());
        if (release == null) {
            return new JavaRuntime(binFolder.getParentFile(), null, true);
        }

        return new JavaRuntime(binFolder.getParentFile(), release.getVersion(), release.isArch64Bit());
    }

    private static PlatformRuntimeFinder getRuntimeFinder(Environment env) {
        switch (env.getPlatform()) {
            case WINDOWS:
                return new WindowsRuntimeFinder();
            case MAC_OS_X:
                return new MacRuntimeFinder();
            case LINUX:
                return new LinuxRuntimeFinder();
            default:
                return null;
        }
    }

    private static File normalizePath(File target) {
        if (target.isFile()) {
            // Probably referring directly to bin/java, back up two levels
            target = target.getParentFile().getParentFile();
        } else if (target.getName().equals("bin")) {
            // Probably copied the bin directory that java.exe is in
            target = target.getParentFile();
        }
        return target;
    }
}
