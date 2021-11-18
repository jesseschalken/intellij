/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
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
package com.google.idea.blaze.android.run.binary.mobileinstall;

import com.android.ddmlib.IDevice;
import com.android.tools.idea.run.ApkFileUnit;
import com.android.tools.idea.run.ApkInfo;
import com.android.tools.idea.run.ApkProvisionException;
import com.android.tools.idea.run.ApplicationIdProvider;
import com.android.tools.idea.run.ConsolePrinter;
import com.android.tools.idea.run.ConsoleProvider;
import com.android.tools.idea.run.LaunchOptions;
import com.android.tools.idea.run.activity.DefaultStartActivityFlagsProvider;
import com.android.tools.idea.run.activity.StartActivityFlagsProvider;
import com.android.tools.idea.run.editor.AndroidDebugger;
import com.android.tools.idea.run.editor.AndroidDebuggerState;
import com.android.tools.idea.run.editor.ProfilerState;
import com.android.tools.idea.run.tasks.LaunchTask;
import com.android.tools.idea.run.tasks.LaunchTasksProvider;
import com.android.tools.idea.run.util.LaunchStatus;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.idea.blaze.android.run.BlazeAndroidDeploymentService;
import com.google.idea.blaze.android.run.binary.BlazeAndroidBinaryApplicationIdProvider;
import com.google.idea.blaze.android.run.binary.BlazeAndroidBinaryApplicationLaunchTaskProvider;
import com.google.idea.blaze.android.run.binary.BlazeAndroidBinaryConsoleProvider;
import com.google.idea.blaze.android.run.binary.BlazeAndroidBinaryRunConfigurationState;
import com.google.idea.blaze.android.run.binary.DeploymentTimingReporterTask;
import com.google.idea.blaze.android.run.binary.UserIdHelper;
import com.google.idea.blaze.android.run.deployinfo.BlazeAndroidDeployInfo;
import com.google.idea.blaze.android.run.runner.BlazeAndroidDeviceSelector;
import com.google.idea.blaze.android.run.runner.BlazeAndroidLaunchTasksProvider;
import com.google.idea.blaze.android.run.runner.BlazeAndroidRunContext;
import com.google.idea.blaze.android.run.runner.BlazeApkBuildStep;
import com.google.idea.blaze.base.sync.data.BlazeDataStorage;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import java.util.Collections;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.jetbrains.android.facet.AndroidFacet;

/** Run context for android_binary. */
abstract class BlazeAndroidBinaryMobileInstallRunContextBase implements BlazeAndroidRunContext {
  protected final Project project;
  protected final AndroidFacet facet;
  protected final RunConfiguration runConfiguration;
  protected final ExecutionEnvironment env;
  protected final BlazeAndroidBinaryRunConfigurationState configState;
  protected final ConsoleProvider consoleProvider;
  protected final ApplicationIdProvider applicationIdProvider;
  protected final BlazeApkBuildStep buildStep;
  private final String launchId;

  public BlazeAndroidBinaryMobileInstallRunContextBase(
      Project project,
      AndroidFacet facet,
      RunConfiguration runConfiguration,
      ExecutionEnvironment env,
      BlazeAndroidBinaryRunConfigurationState configState,
      BlazeApkBuildStep buildStep,
      String launchId) {
    this.project = project;
    this.facet = facet;
    this.runConfiguration = runConfiguration;
    this.env = env;
    this.configState = configState;
    this.consoleProvider = new BlazeAndroidBinaryConsoleProvider(project);
    this.buildStep = buildStep;
    this.applicationIdProvider = new BlazeAndroidBinaryApplicationIdProvider(buildStep);
    this.launchId = launchId;
  }

  @Override
  public BlazeAndroidDeviceSelector getDeviceSelector() {
    return new BlazeAndroidDeviceSelector.NormalDeviceSelector();
  }

  @Override
  public void augmentLaunchOptions(LaunchOptions.Builder options) {
    options
        .setDeploy(StudioDeployerExperiment.isEnabled())
        .setOpenLogcatAutomatically(configState.showLogcatAutomatically());
    options.addExtraOptions(
        ImmutableMap.of(ProfilerState.ANDROID_PROFILER_STATE_ID, configState.getProfilerState()));
  }

  @Override
  public ConsoleProvider getConsoleProvider() {
    return consoleProvider;
  }

  @Override
  public ApplicationIdProvider getApplicationIdProvider() throws ExecutionException {
    return applicationIdProvider;
  }

  @Override
  public BlazeApkBuildStep getBuildStep() {
    return buildStep;
  }

  @Override
  public ImmutableList<LaunchTask> getDeployTasks(IDevice device, LaunchOptions launchOptions)
      throws ExecutionException {
    if (!StudioDeployerExperiment.isEnabled()) {
      return ImmutableList.of();
    }

    BlazeAndroidDeployInfo deployInfo;
    try {
      deployInfo = buildStep.getDeployInfo();
    } catch (ApkProvisionException e) {
      throw new ExecutionException(e);
    }

    String packageName = deployInfo.getMergedManifest().packageName;
    if (packageName == null) {
      throw new ExecutionException("Could not determine package name from deploy info");
    }

    ApkInfo info =
        new ApkInfo(
            deployInfo.getApksToDeploy().stream()
                .map(file -> new ApkFileUnit(BlazeDataStorage.WORKSPACE_MODULE_NAME, file))
                .collect(Collectors.toList()),
            packageName);

    LaunchTask deployTask =
        BlazeAndroidDeploymentService.getInstance(project)
            .getDeployTask(Collections.singletonList(info), launchOptions);
    return ImmutableList.of(new DeploymentTimingReporterTask(launchId, deployTask));
  }

  @Nullable
  @Override
  public Integer getUserId(IDevice device, ConsolePrinter consolePrinter)
      throws ExecutionException {
    return UserIdHelper.getUserIdFromConfigurationState(device, consolePrinter, configState);
  }

  @Override
  public LaunchTasksProvider getLaunchTasksProvider(LaunchOptions.Builder launchOptionsBuilder)
      throws ExecutionException {
    return new BlazeAndroidLaunchTasksProvider(
        project, this, applicationIdProvider, launchOptionsBuilder);
  }

  @Override
  public LaunchTask getApplicationLaunchTask(
      LaunchOptions launchOptions,
      @Nullable Integer userId,
      String contributorsAmStartOptions,
      AndroidDebugger androidDebugger,
      AndroidDebuggerState androidDebuggerState,
      LaunchStatus launchStatus)
      throws ExecutionException {

    String extraFlags = UserIdHelper.getFlagsFromUserId(userId);
    if (!contributorsAmStartOptions.isEmpty()) {
      extraFlags += (extraFlags.isEmpty() ? "" : " ") + contributorsAmStartOptions;
    }

    final StartActivityFlagsProvider startActivityFlagsProvider =
        new DefaultStartActivityFlagsProvider(
            androidDebugger, androidDebuggerState, project, launchOptions.isDebug(), extraFlags);
    BlazeAndroidDeployInfo deployInfo;
    try {
      deployInfo = buildStep.getDeployInfo();
    } catch (ApkProvisionException e) {
      throw new ExecutionException(e);
    }

    return BlazeAndroidBinaryApplicationLaunchTaskProvider.getApplicationLaunchTask(
        applicationIdProvider,
        deployInfo.getMergedManifest(),
        configState,
        startActivityFlagsProvider,
        launchStatus);
  }

  @Override
  public String getAmStartOptions() {
    return configState.getAmStartOptions();
  }
}
