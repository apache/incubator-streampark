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

package org.apache.streampark.storage.oss;

import org.apache.streampark.storage.StorageService;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.PutObjectRequest;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class OssStorageService implements StorageService {

    final OssConfig ossConfig;
    final OSS ossClient;

    public OssStorageService(OssConfig config) {
        this.ossConfig = config;
        this.ossClient = new OSSClientBuilder()
            .build(ossConfig.getEndpoint(), ossConfig.getAccessKeyId(), ossConfig.getAccessKeySecret());
    }

    @Override
    public void getData(String objectPath, String localFilePath) throws Exception {
        String bucket = ossConfig.getBucket();

        if (!ossClient.doesObjectExist(bucket, objectPath)) {
            throw new RuntimeException(String.format("File '%s' not exist", objectPath));
        }

        try {
            ossClient.getObject(new GetObjectRequest(bucket, objectPath), new File(localFilePath));
        } catch (Exception e) {
            log.error("GetData failed. ObjectPath: {}, local path: {}.", objectPath, localFilePath, e);
            throw handleOssException(e);
        }
    }

    @Override
    public void putData(String objectPath, String localFilePath) throws Exception {
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(ossConfig.getBucket(), objectPath, new File(localFilePath));
            ossClient.putObject(putObjectRequest);
        } catch (Exception e) {
            log.error("PutData failed. ObjectPath: {}, local path: {}.", objectPath, localFilePath, e);
            throw handleOssException(e);
        }
    }

    @VisibleForTesting
    static RuntimeException handleOssException(Exception e) {
        if (e instanceof OSSException) {
            OSSException oe = (OSSException) e;
            String errMsg = String.format("Caught an OSSException. Error Message: %s." +
                    " Error Code: %s. Request ID: %s", oe.getErrorMessage(), oe.getErrorCode(),
                oe.getRequestId());
            return new RuntimeException(errMsg, oe);
        } else if (e instanceof ClientException) {
            ClientException ce = (ClientException) e;
            String errMsg = String.format("Caught an ClientException. Error Message: %s.", ce.getMessage());
            return new RuntimeException(errMsg, ce);
        } else {
            return new RuntimeException(e);
        }
    }
}
