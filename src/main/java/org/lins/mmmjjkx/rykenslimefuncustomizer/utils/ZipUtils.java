/*
 * RykenSlimefunCustomizer
 * Copyright (C) 2026 lijinhong11(mmmjjjkx) and balugaq
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.lins.mmmjjkx.rykenslimefuncustomizer.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * ZIP extraction helpers used by the RSC addon updater.
 *
 * <p>The updater must never merge a new addon archive over an old addon directory.
 * Removed or renamed YAML/script files would otherwise survive forever and can keep
 * obsolete classes, recipes, or IDs active after an update. Runtime updates are
 * therefore unpacked to a staging directory and swapped into place as one clean tree.</p>
 */
public final class ZipUtils {
    private ZipUtils() {}

    public static void unzip(File zipFile, File desDirectory) throws IOException {
        if (!desDirectory.exists()) {
            boolean mkdirSuccess = desDirectory.mkdirs();
            if (!mkdirSuccess)
                throw new IOException("Failed to create destination directory: " + desDirectory);
        }

        if (!zipFile.exists())
            throw new FileNotFoundException("Archive does not exist: " + zipFile);

        String destinationRoot = desDirectory.getCanonicalPath() + File.separator;

        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                if (!zipEntry.isDirectory()) {
                    String entryName = zipEntry.getName().replace('\\', '/');

                    // Release archives normally contain one top-level addon folder.
                    // Strip exactly that first segment so the caller controls the final
                    // destination directory name.
                    int firstSlashIndex = entryName.indexOf('/');
                    if (firstSlashIndex != -1) {
                        entryName = entryName.substring(firstSlashIndex + 1);
                    }
                    if (entryName.isBlank()) {
                        zipEntry = zipInputStream.getNextEntry();
                        continue;
                    }

                    File outFile = new File(desDirectory, entryName);
                    String outputPath = outFile.getCanonicalPath();
                    if (!outputPath.startsWith(destinationRoot)) {
                        throw new IOException("Blocked unsafe zip entry: " + zipEntry.getName());
                    }

                    mkdir(outFile.getParentFile());

                    try (BufferedOutputStream bufferedOutputStream =
                            new BufferedOutputStream(new FileOutputStream(outFile))) {
                        byte[] bytes = new byte[8192];
                        int readLen;
                        long totalBytesRead = 0;
                        while ((readLen = zipInputStream.read(bytes)) != -1) {
                            bufferedOutputStream.write(bytes, 0, readLen);
                            totalBytesRead += readLen;
                        }

                        if (zipEntry.getSize() != -1 && totalBytesRead != zipEntry.getSize()) {
                            throw new IOException("Incomplete zip entry: " + zipEntry.getName());
                        }
                    }
                }
                zipEntry = zipInputStream.getNextEntry();
            }
        }
    }

    /**
     * Cleanly replaces {@code destination} with the contents of {@code zipFile}.
     * The new archive is fully extracted before the existing addon tree is touched.
     */
    public static void replaceDirectoryFromZip(File zipFile, File destination) throws IOException {
        File parent = destination.getAbsoluteFile().getParentFile();
        if (parent == null) {
            throw new IOException("Destination has no parent directory: " + destination);
        }
        mkdir(parent);

        File staging = new File(parent, destination.getName() + ".update-staging");
        File backup = new File(parent, destination.getName() + ".update-backup");
        deleteRecursively(staging);
        deleteRecursively(backup);

        unzip(zipFile, staging);

        boolean movedOldDirectory = false;
        try {
            if (destination.exists()) {
                Files.move(
                    destination.toPath(),
                    backup.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                );
                movedOldDirectory = true;
            }

            Files.move(
                staging.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            );

            deleteRecursively(backup);
        } catch (IOException updateFailure) {
            deleteRecursively(staging);

            if (movedOldDirectory && backup.exists() && !destination.exists()) {
                try {
                    Files.move(
                        backup.toPath(),
                        destination.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    );
                } catch (IOException restoreFailure) {
                    updateFailure.addSuppressed(restoreFailure);
                }
            }
            throw updateFailure;
        }
    }

    public static void deleteRecursively(File file) throws IOException {
        if (file == null || !file.exists()) return;

        try (Stream<java.nio.file.Path> paths = Files.walk(file.toPath())) {
            for (java.nio.file.Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    public static void mkdir(File file) {
        if (file == null || file.exists()) return;
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) mkdir(parent);
        file.mkdirs();
    }
}
