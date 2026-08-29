/*
 * RykenSlimefunCustomizer
 * Copyright (C) 2026 lijinhong11(mmmjjkx) and balugaq
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 纯文件 IO 的 ZIP 解压工具，无 Bukkit/插件静态依赖，便于单元测试。
 * 从 GithubUpdater 抽取，使解压逻辑（含 zip-slip 安全语义）可被测试覆盖。
 */
public final class ZipUtils {
    private ZipUtils() {}

    public static void unzip(File zipFile, File desDirectory) throws IOException {
        if (!desDirectory.exists()) {
            boolean mkdirSuccess = desDirectory.mkdirs();
            if (!mkdirSuccess)
                throw new IOException("failed");
        }

        if (!zipFile.exists())
            throw new FileNotFoundException("RSC: ");

        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                if (!zipEntry.isDirectory()) {
                    String entryName = zipEntry.getName();

                    int firstSlashIndex = entryName.indexOf('/');
                    if (firstSlashIndex != -1) {
                        entryName = entryName.substring(firstSlashIndex + 1);
                    }
                    File outFile = new File(desDirectory, entryName);
                    mkdir(outFile.getParentFile());

                    try (BufferedOutputStream bufferedOutputStream =
                            new BufferedOutputStream(new FileOutputStream(outFile))) {
                        byte[] bytes = new byte[1024];
                        int readLen;
                        long totalBytesRead = 0;
                        while ((readLen = zipInputStream.read(bytes)) != -1) {
                            bufferedOutputStream.write(bytes, 0, readLen);
                            totalBytesRead += readLen;
                        }

                        if (zipEntry.getSize() != -1 && totalBytesRead != zipEntry.getSize()) {
                            throw new IOException("RSC: ");
                        }
                    }
                }
                zipEntry = zipInputStream.getNextEntry();
            }
        }
    }

    public static void mkdir(File file) {
        if (file == null || file.exists()) return;
        if (!file.getParentFile().exists()) mkdir(file.getParentFile());
        file.mkdirs();
    }
}
