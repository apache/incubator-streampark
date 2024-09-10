/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.console.core.service;

import org.apache.streampark.console.SpringUnitTestBase;

import org.apache.hc.core5.http.ContentType;

import org.h2.store.fs.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** org.apache.streampark.console.core.service.ResourceServiceTest. */
class ResourceServiceTest extends SpringUnitTestBase {

    @Autowired
    private ResourceService resourceService;

    @Test
    void testUpload(@TempDir Path tempDir) throws Exception {
        // specify the file path
        File fileToStoreUploadFile =
            new File(tempDir.toFile().getAbsolutePath() + "/fileToStoreUploadFile");
        FileUtils.createFile(fileToStoreUploadFile.getAbsolutePath());

        File fileToUpload = new File(tempDir.toFile().getAbsolutePath() + "/fileToUpload.jar");
        FileUtils.createFile(fileToUpload.getAbsolutePath());
        assertThat(fileToUpload).exists();
        MultipartFile mulFile =
            new MockMultipartFile(
                "test", // fileName (eg: streampark.jar)
                fileToUpload.getAbsolutePath(), // originalFilename (eg: path + fileName =
                // /tmp/file/streampark.jar)
                ContentType.APPLICATION_OCTET_STREAM.toString(),
                Files.newInputStream(fileToStoreUploadFile.toPath()));
        resourceService.upload(mulFile);
    }
}
