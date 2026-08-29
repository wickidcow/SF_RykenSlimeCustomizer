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
package org.lins.mmmjjkx.rykenslimefuncustomizer.updater;

import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jspecify.annotations.NonNull;
import org.lins.mmmjjkx.rykenslimefuncustomizer.ProjectAddonManager;
import org.lins.mmmjjkx.rykenslimefuncustomizer.RykenSlimefunCustomizer;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.ZipUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GitHubUpdater {
    private static final ExecutorService UPDATE_EXECUTOR =
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "RSC-Update-Thread");
            t.setDaemon(true); // 设置为守护线程，防止阻止服务器关闭
            return t;
        });

    public static final File downloadFolder =
            new File(RykenSlimefunCustomizer.INSTANCE.getDataFolder(), "temp-downloads");

    public static CompletableFuture<Boolean> checkAndUpdateAsync(
        String currentVer,
        String author,
        String repo,
        String prjId,
        String folderName) {

        return CompletableFuture.supplyAsync(() -> {
            // 实际的更新操作在后台线程执行
            return checkAndUpdate(currentVer, author, repo, prjId, folderName);
        }, UPDATE_EXECUTOR);
    }

    private static boolean checkAndUpdate(
            @NonNull String currentVer,
            @NonNull String author,
            @NonNull String repo,
            @NonNull String prjId,
            @NonNull String folderName) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            String url = "https://api.github.com/repos/" + author + "/" + repo + "/releases/latest";
            Debug.info("RSC message" + url + " addon " + prjId);
            HttpGet get = new HttpGet(url);
            HttpResponse response = client.execute(get);
            String entity = EntityUtils.toString(response.getEntity());

            GitHubRelease release = new Gson().fromJson(entity, GitHubRelease.class);

            String releaseName = release.getName();

            if (releaseName == null) {
                Debug.warn("Unable toaddon " + prjId + ": GitHub API (60 /h)");
                return false;
            }

            if (releaseName.startsWith("v") && !currentVer.startsWith("v"))
                releaseName = releaseName.replaceFirst("v", "");


            if (!Objects.equals(currentVer, releaseName)) {
                if (release.isPrerelease() && !RykenSlimefunCustomizer.INSTANCE.getConfig().getBoolean("update.pre-releases", false)) {
                    return false;
                }

                if (!downloadFolder.exists()) {
                    downloadFolder.mkdirs();
                }

                File zip = new File(downloadFolder, prjId + "-" + releaseName + ".zip");

                String zipUrl;
                List<GitHubRelease.Asset> assets = release.getAssets();
                if (assets == null || assets.isEmpty()) {
                    zipUrl = release.getZipball_url();
                } else {
                    ProjectAddon prj = RykenSlimefunCustomizer.addonManager.get(prjId);
                    if (prj == null) {
                        zipUrl = release.getZipball_url();
                    } else {
                        GitHubRelease.Asset asset = assets.stream()
                                .filter(z -> z.getName().equalsIgnoreCase(prj.getDownloadZipName()))
                                .findFirst()
                                .orElse(null);
                        if (asset == null) {
                            zipUrl = release.getZipball_url();
                        } else {
                            zipUrl = asset.getBrowser_download_url();
                        }
                    }
                }

                URL urlObj = new URL(zipUrl);

                if (!zip.exists() && !zip.createNewFile())
                    throw new IOException("failed");


                long result = Files.copy(urlObj.openStream(), zip.toPath(), StandardCopyOption.REPLACE_EXISTING);

                if (result < 1 || !zip.exists()) return false;

                File projectFolder = new File(ProjectAddonManager.ADDONS_DIRECTORY, folderName);

                if (!projectFolder.exists())
                    ZipUtils.mkdir(projectFolder);

                ZipUtils.unzip(zip, projectFolder);
                return true;

            }
            return true;
        } catch (Exception e) {
            Debug.warn("Unable toaddon " + prjId, e);
            return false;
        }
    }
}
