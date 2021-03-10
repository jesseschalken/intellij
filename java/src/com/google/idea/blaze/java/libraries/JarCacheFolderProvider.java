/*
 * Copyright 2021 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.idea.blaze.java.libraries;

import com.google.idea.blaze.base.io.FileOperationProvider;
import com.google.idea.blaze.base.settings.BlazeImportSettings;
import com.google.idea.blaze.base.settings.BlazeImportSettingsManager;
import com.google.idea.blaze.base.sync.data.BlazeDataStorage;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import java.io.File;

/** Provides {@link File} instance to store local JAR cache. */
public class JarCacheFolderProvider {
  public static JarCacheFolderProvider getInstance(Project project) {
    return ServiceManager.getService(project, JarCacheFolderProvider.class);
  }

  private static final String JAR_CACHE_FOLDER_NAME = "libraries";

  private final Project project;

  public JarCacheFolderProvider(Project project) {
    this.project = project;
  }

  /** Gets the actual {@link File} object for storing JAR files. */
  public File getJarCacheFolder() {
    BlazeImportSettings importSettings =
        BlazeImportSettingsManager.getInstance(project).getImportSettings();
    return new File(BlazeDataStorage.getProjectDataDir(importSettings), JAR_CACHE_FOLDER_NAME);
  }

  /** Returns the {@link File} instance that represents a JAR file by the {@code key} value. */
  public File getCacheFileByKey(String key) {
    return new File(getJarCacheFolder(), key);
  }

  /** Checks if the JAR cache exists and is a folder. */
  public boolean isJarCacheFolderReady() {
    FileOperationProvider fileOperationProvider = FileOperationProvider.getInstance();
    if (fileOperationProvider.exists(getJarCacheFolder())
        && fileOperationProvider.isDirectory(getJarCacheFolder())) {
      return true;
    }
    return fileOperationProvider.mkdirs(getJarCacheFolder());
  }

  protected Project getProject() {
    return project;
  }
}
