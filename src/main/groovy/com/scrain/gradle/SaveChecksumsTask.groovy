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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Gradle task class for saving all computed checksums to the configured checksum property file
 */
class SaveChecksumsTask extends DefaultTask {
    protected static final String NAME = 'saveChecksums'

    private final ChecksumExtension checksumExt = project.extensions.findByName(ChecksumExtension.NAME)

    @OutputFile
    File getChecksumsFile() {
        project.file checksumExt.propertyFile
    }

    SaveChecksumsTask() {
        group = ChecksumPlugin.TASK_GROUP
        description = 'Saves computed checksum values to checksum file configured in extension checksumExt.propertyFile'
    }

    @TaskAction
    def save() {
        createChecksumsFile()

        Properties existingProperties = loadProperties(checksumsFile)

        project.tasks.findAll { it instanceof ChecksumTask }.each { ChecksumTask it ->
            writeChecksum(
                checksumsFile,
                it.propertyName,
                it.checksumFile?.text,
                existingProperties.containsKey(it.propertyName)
            )
        }
    }

    private File createChecksumsFile() {
        if (!checksumsFile.exists()) {
            logger.lifecycle ":${name} checksums file does not exist, creating: ${checksumsFile}"
            assert checksumsFile.parentFile.exists() ?: checksumsFile.parentFile.mkdirs()
            assert checksumsFile.createNewFile(), "unable to create checksums file! ${checksumsFile}"
        } else if (!checksumsFile.isFile()) {
            throw new GradleException("checksumsFile ${checksumExt.propertyFile} is a directory!")
        }
        checksumsFile
    }

    protected void writeChecksum(File file, String key, String value, boolean keyAlreadyExists) {
        String val = value ?: ''
        if (keyAlreadyExists) {
            logger.lifecycle(":${name} updating - ${key}=${val}")
            project.ant.replaceregexp(file: file, byline: true) {
                regexp(pattern: "^(\\s*)$key((\\s*[=|:]\\s*)|(\\s+)).*\$")
                substitution(expression: "\\1$key\\2$val")
            }
        } else {
            logger.lifecycle(":${name} adding -   ${key}=${val}")
            file << "\n${key}=${val}"
        }
    }

    private Properties loadProperties(File file) {
        Properties props = new Properties()
        props.load(file.newReader())
        props
    }
}
