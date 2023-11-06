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
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log
public class WindowsRuntimeFinder implements PlatformRuntimeFinder {
    @Override
    public Set<File> getLauncherDirectories(Environment env) {
        HashSet<File> launcherDirs = new HashSet<>();

        try {
            String launcherPath = WinRegistry.readString(WinReg.HKEY_CURRENT_USER,
                    "SOFTWARE\\Mojang\\InstalledProducts\\Minecraft Launcher", "InstallLocation");

            launcherDirs.add(new File(launcherPath));
        } catch (Throwable err) {
            log.log(Level.WARNING, "Failed to read launcher location from registry");
        }

        String programFiles = Objects.equals(env.getArchBits(), "64")
                ? System.getenv("ProgramFiles(x86)")
                : System.getenv("ProgramFiles");

        // Mojang likes to move the java runtime directory
        launcherDirs.add(new File(programFiles, "Minecraft"));
        launcherDirs.add(new File(programFiles, "Minecraft Launcher"));
        launcherDirs.add(new File(System.getenv("LOCALAPPDATA"), "Packages\\Microsoft.4297127D64EC6_8wekyb3d8bbwe\\LocalCache\\Local"));

        return launcherDirs;
    }

    @Override
    public List<File> getCandidateJavaLocations() {
        return Collections.emptyList();
    }

    @Override
    public List<JavaRuntime> getExtraRuntimes() {
        ArrayList<JavaRuntime> entries = Lists.newArrayList();

        getEntriesFromRegistry(entries, "SOFTWARE\\JavaSoft\\Java Runtime Environment");
        getEntriesFromRegistry(entries, "SOFTWARE\\JavaSoft\\Java Development Kit");
        getEntriesFromRegistry(entries, "SOFTWARE\\JavaSoft\\JDK");

        // JAVA_HOME
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null) {
            File javaHomeDir = new File(javaHome);
            File javaExecutable = new File(javaHomeDir, "bin/java.exe");
            if (javaExecutable.exists()) {
                JavaRuntime javaHomeRuntime = new JavaRuntime(javaHomeDir, "JAVA_HOME", false);
                entries.add(javaHomeRuntime);
            }
        }

        // ADOPTIUM
        File javaDirectory = new File("C:\\Program Files\\Eclipse Adoptium");

        File[] subDirectories = javaDirectory.listFiles(File::isDirectory);

        if (subDirectories != null) {
            for (File subDir : subDirectories) {
                String dirName = subDir.getName();

                String version = extractVersionFromDirectoryName(dirName);

                if (version != null) {
                    JavaRuntime javaRuntime = new JavaRuntime(subDir, version, false);
                    if (javaRuntime.getJavaExecutable().exists()) {
                        entries.add(javaRuntime);
                    }
                }
            }
        }

        // TLauncher JRE
        String userHome = System.getProperty("user.home");
        File tLauncherJreDirectory = new File(userHome + File.separator + "AppData" + File.separator + "Roaming" + File.separator + ".tlauncher" + File.separator + "legacy" + File.separator + "Minecraft" + File.separator + "jre");
        if (tLauncherJreDirectory.exists()) {
            JavaRuntime tLauncherJreRuntime = new JavaRuntime(tLauncherJreDirectory, "TLauncher JRE", false);
            entries.add(tLauncherJreRuntime);
        }

        return entries;
    }

    private String extractVersionFromDirectoryName(String dirName) {
        Pattern pattern = Pattern.compile("jdk-(\\d+\\.\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(dirName);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
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
