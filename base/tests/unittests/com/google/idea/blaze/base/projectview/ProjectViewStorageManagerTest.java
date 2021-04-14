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
package com.google.idea.blaze.base.projectview;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test for {@link ProjectViewStorageManager}. */
@RunWith(JUnit4.class)
public class ProjectViewStorageManagerTest {

  @Test
  public void testIsProjectViewFile() {
    assertThat(ProjectViewStorageManager.isProjectViewFile(".blazeproject")).isTrue();
    assertThat(ProjectViewStorageManager.isProjectViewFile(".bazelproject")).isTrue();
    assertThat(ProjectViewStorageManager.isProjectViewFile(".asproject")).isTrue();

    assertThat(ProjectViewStorageManager.isProjectViewFile(".buckproject")).isFalse();
    assertThat(ProjectViewStorageManager.isProjectViewFile(".bazel")).isFalse();

    assertThat(ProjectViewStorageManager.isProjectViewFile("foo.blazeproject")).isTrue();
    assertThat(ProjectViewStorageManager.isProjectViewFile("bar.baz.bazelproject")).isTrue();

    assertThat(ProjectViewStorageManager.isProjectViewFile("blazeproject")).isFalse();
    assertThat(ProjectViewStorageManager.isProjectViewFile("bazelproject")).isFalse();

    assertThat(ProjectViewStorageManager.isProjectViewFile("blazeproject.foo")).isFalse();
    assertThat(ProjectViewStorageManager.isProjectViewFile("bar.bazelproject.baz")).isFalse();

    assertThat(ProjectViewStorageManager.isProjectViewFile(".")).isFalse();
    assertThat(ProjectViewStorageManager.isProjectViewFile("")).isFalse();
  }
}
