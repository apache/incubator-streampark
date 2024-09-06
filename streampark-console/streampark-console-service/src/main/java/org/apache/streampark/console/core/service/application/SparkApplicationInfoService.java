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

package org.apache.streampark.console.core.service.application;

import org.apache.streampark.console.base.exception.ApplicationException;
import org.apache.streampark.console.core.entity.SparkApplication;
import org.apache.streampark.console.core.enums.AppExistsStateEnum;

import org.apache.hadoop.yarn.api.records.ApplicationReport;

import com.baomidou.mybatisplus.extension.service.IService;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * This interface defines the methods that can be used for various utility operations related to an
 * application.
 */
public interface SparkApplicationInfoService extends IService<SparkApplication> {

    /**
     * Checks the environment for the given application.
     *
     * @param appParam the application to check the environment for
     * @return true if the environment is valid for the application, false otherwise
     * @throws ApplicationException if an error occurs while checking the environment
     */
    boolean checkEnv(SparkApplication appParam) throws ApplicationException;

    /**
     * Checks if the given application meets the required alterations.
     *
     * @param appParam The application to be checked.
     * @return True if the application meets the required alterations, false otherwise.
     * @throws ApplicationException If an error occurs while checking the alterations.
     */
    boolean checkAlter(SparkApplication appParam);

    /**
     * Checks if a record exists in the database with the given team ID.
     *
     * @param teamId The ID of the team to check.
     * @return true if a record with the given team ID exists, false otherwise.
     */
    boolean existsByTeamId(Long teamId);

    /**
     * Checks if a record exists in the database with the given user ID.
     *
     * @param userId The ID of the user to check.
     * @return true if a record with the given user ID exists, false otherwise.
     */
    boolean existsByUserId(Long userId);

    /**
     * Checks if a job exists for a given Spark environment ID.
     *
     * @param sparkEnvId The ID of the Spark environment.
     * @return true if a job exists for the given Spark environment ID; otherwise, false.
     */
    boolean existsBySparkEnvId(Long sparkEnvId);

    /**
     * Gets the YARN name for the given application.
     *
     * @param appConfig The application's config for which to retrieve the YARN name.
     * @return The YARN name of the application as a String.
     */
    String getYarnName(String appConfig);

    /**
     * Checks if the given application exists in the system.
     *
     * @param appParam The application to check for existence.
     * @return AppExistsState indicating the existence state of the application.
     */
    AppExistsStateEnum checkExists(SparkApplication appParam);

    /**
     * Reads the configuration for the given application and returns it as a String.
     *
     * @param appConfig The application's config for which the configuration needs to be read.
     * @return The configuration for the given application as a String.
     * @throws IOException If an I/O error occurs while reading the configuration.
     */
    String readConf(String appConfig) throws IOException;

    /**
     * Returns the dashboard for the specified team.
     *
     * @param teamId the ID of the team
     * @return a map containing the dashboard data
     */
    Map<String, Serializable> getDashboardDataMap(Long teamId);

    /**
     * Retrieves the list of recent Kubernetes namespaces.
     *
     * @return The list of recent Kubernetes namespaces as a List of Strings.
     */
    List<String> listRecentK8sNamespace();

    /**
     * Retrieves the recent K8s container images
     *
     * @return a List of Strings representing the recent K8s container images.
     */
    List<String> listRecentK8sContainerImage();

    /**
     * check application before start
     *
     * @param id the application's id which need to check before start.
     * @return org.apache.streampark.console.core.enums.AppExistsStateEnum
     */
    AppExistsStateEnum checkStart(Long id);

    /**
     * @param appName
     * @return running,submitted, accepted job list in YARN
     */
    List<ApplicationReport> getYarnAppReport(String appName);
}
