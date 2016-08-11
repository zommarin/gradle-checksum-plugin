/*
 * Copyright [2016] Shawn Crain
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.scrain.gradle

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class ChecksumPlugin implements Plugin<Project> {
    protected final static String TASK_GROUP = "Checksum Tasks"

    private ChecksumExtension checksumExt

    void apply(Project project) {
        checksumExt = project.extensions.create(ChecksumExtension.NAME, ChecksumExtension, project)

        Task computeChecksums = project.tasks.create(ComputeChecksumsTask.NAME, ComputeChecksumsTask)

        Task saveChecksums = project.tasks.create(SaveChecksumsTask.NAME, SaveChecksumsTask)

        saveChecksums.dependsOn computeChecksums

        project.afterEvaluate {
            checksumExt.tasks.each { ChecksumItem item ->
                println ":checksum-plugin: item - ${item}"
                createChecksumTask(project, item)
            }
        }

        project.tasks.withType(SourceChecksumTask) {
            computeChecksums.dependsOn it
        }
    }

    private Task createChecksumTask( Project project, ChecksumItem item ) {
        Task task = findTask(project, item)

        String checksumTaskName = checksumExt.checksumTaskName(item)

        println ":checksum-plugin: configuring checksum task ${checksumTaskName}"

        Task checksumTask = project.tasks.create(checksumTaskName, SourceChecksumTask)

        checksumTask.description  = "Generates checksum for ${item.useSource?'sources':'output'} of task '${task.name}'"
        checksumTask.propertyName = checksumExt.checksumPropertyName(item)
        checksumTask.source       = item.useSource ? task.source : task

        checksumTask
    }

    private Task findTask(Project project, ChecksumItem item) {
        Task task = project.tasks.findByName(item.name)

        if (!task) {
            throw new GradleException("Task '${item.name}' not found")
        }

        if (task instanceof SourceChecksumTask || task instanceof SaveChecksumsTask || task instanceof ComputeChecksumsTask) {
            throw new GradleException("Task '${item.name}' is a checksum plugin task")
        }

        task
    }


}