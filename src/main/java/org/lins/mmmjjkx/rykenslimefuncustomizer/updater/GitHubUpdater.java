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
import java.io.InputStream;
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
            t.setDaemon(true);
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

        return CompletableFuture.supplyAsync(
            () -> checkAndUpdate(currentVer, author, repo, prjId, folderName),
            UPDATE_EXECUTOR
        );
    }

    private static boolean checkAndUpdate(
            @NonNull String currentVer,
            @NonNull String author,
            @NonNull String repo,
            @NonNull String prjId,
            @NonNull String folderName) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            String url = "https://api.github.com/repos/" + author + "/" + repo + "/releases/latest";
            Debug.debug(() -> "Checking addon release: " + url + " (" + prjId + ")");

            HttpGet get = new HttpGet(url);
            get.setHeader("Accept", "application/vnd.github+json");
            get.setHeader("X-GitHub-Api-Version", "2022-11-28");
            get.setHeader("User-Agent", "RykenSlimefunCustomizer-Legacy");

            HttpResponse response = client.execute(get);
            int status = response.getStatusLine().getStatusCode();
            String entity = response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity());

            if (status == 404) {
                // GitHub's /releases/latest endpoint intentionally ignores prereleases.
                // An addon with prerelease-only builds therefore has no "latest" release.
                Debug.debug(() -> "No stable GitHub release found for addon " + prjId);
                return false;
            }
            if (status == 403 || status == 429) {
                Debug.info("Addon update check deferred for " + prjId + " because GitHub rate-limited the request.");
                return false;
            }
            if (status < 200 || status >= 300) {
                Debug.info("Addon update check skipped for " + prjId + " (GitHub HTTP " + status + ").");
                return false;
            }

            GitHubRelease release = new Gson().fromJson(entity, GitHubRelease.class);
            if (release == null || release.getName() == null || release.getName().isBlank()) {
                Debug.info("Addon update check returned no usable release metadata for " + prjId + ".");
                return false;
            }

            String releaseName = release.getName();
            if (releaseName.startsWith("v") && !currentVer.startsWith("v")) {
                releaseName = releaseName.substring(1);
            }

            if (Objects.equals(currentVer, releaseName)) {
                return true;
            }

            if (release.isPrerelease()
                && !RykenSlimefunCustomizer.INSTANCE.getConfig().getBoolean("update.pre-releases", false)) {
                return false;
            }

            if (!downloadFolder.exists() && !downloadFolder.mkdirs()) {
                throw new IOException("Unable to create addon download folder: " + downloadFolder);
            }

            File zip = new File(downloadFolder, prjId + "-" + releaseName + ".zip");
            String zipUrl = selectDownloadUrl(release, prjId);
            if (zipUrl == null || zipUrl.isBlank()) {
                Debug.info("No downloadable archive was found for addon " + prjId + ".");
                return false;
            }

            URL urlObj = new URL(zipUrl);
            try (InputStream input = urlObj.openStream()) {
                long result = Files.copy(input, zip.toPath(), StandardCopyOption.REPLACE_EXISTING);
                if (result < 1 || !zip.exists()) return false;
            }

            File projectFolder = new File(ProjectAddonManager.ADDONS_DIRECTORY, folderName);

            // Never unzip over the live directory. A merge leaves removed YAML/scripts
            // behind and can make an updated addon continue loading obsolete content.
            ZipUtils.replaceDirectoryFromZip(zip, projectFolder);
            Debug.info("Updated addon " + prjId + " to " + releaseName + " with a clean directory replacement.");
            return true;
        } catch (Exception e) {
            Debug.warn("Unable to update addon " + prjId, e);
            return false;
        }
    }

    private static String selectDownloadUrl(GitHubRelease release, String prjId) {
        List<GitHubRelease.Asset> assets = release.getAssets();
        if (assets == null || assets.isEmpty()) {
            return release.getZipball_url();
        }

        ProjectAddon prj = RykenSlimefunCustomizer.addonManager.get(prjId);
        if (prj == null || prj.getDownloadZipName() == null || prj.getDownloadZipName().isBlank()) {
            return release.getZipball_url();
        }

        GitHubRelease.Asset asset = assets.stream()
            .filter(z -> z.getName() != null && z.getName().equalsIgnoreCase(prj.getDownloadZipName()))
            .findFirst()
            .orElse(null);

        return asset == null ? release.getZipball_url() : asset.getBrowser_download_url();
    }
}
