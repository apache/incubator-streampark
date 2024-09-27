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
import org.apache.streampark.console.core.entity.FlinkApplication;
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
public interface FlinkApplicationInfoService extends IService<FlinkApplication> {

    /**
     * Checks the environment for the given application.
     *
     * @param appParam the application to check the environment for
     * @return true if the environment is valid for the application, false otherwise
     * @throws ApplicationException if an error occurs while checking the environment
     */
    boolean checkEnv(FlinkApplication appParam) throws ApplicationException;

    /**
     * Checks the savepoint path for the given application.
     *
     * @param appParam the application to check the savepoint path for
     * @return the check message
     * @throws Exception if an error occurs while checking the savepoint path
     */
    String checkSavepointPath(FlinkApplication appParam) throws Exception;

    /**
     * Checks if the given application meets the required alterations.
     *
     * @param appParam The application to be checked.
     * @return True if the application meets the required alterations, false otherwise.
     * @throws ApplicationException If an error occurs while checking the alterations.
     */
    boolean checkAlter(FlinkApplication appParam);

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
     * Checks if a job exists for a given Flink environment ID.
     *
     * @param flinkEnvId The ID of the Flink environment.
     * @return true if a job exists for the given Flink environment ID; otherwise, false.
     */
    boolean existsByFlinkEnvId(Long flinkEnvId);

    /**
     * Checks if a job is running for a given cluster ID.
     *
     * @param clusterId The ID of the cluster.
     * @return true if a job is running for the given cluster ID; otherwise, false.
     */
    boolean existsRunningByClusterId(Long clusterId);

    /**
     * Checks if there is a job that is associated with the given cluster ID.
     *
     * @param clusterId The ID of the cluster.
     * @return True if a job exists for the given cluster ID, false otherwise.
     */
    boolean existsByClusterId(Long clusterId);

    /**
     * Counts the number of items associated with the given cluster ID.
     *
     * @param clusterId The ID of the cluster.
     * @return The number of items associated with the given cluster ID.
     */
    Integer countByClusterId(Long clusterId);

    /**
     * Counts the number of items associated with the given cluster ID and database type.
     *
     * @param clusterId The ID of the cluster.
     * @param dbType The type of the database.
     * @return The number of items associated with the given cluster ID and database type.
     */
    Integer countAffectedByClusterId(Long clusterId, String dbType);

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
    AppExistsStateEnum checkExists(FlinkApplication appParam);

    /**
     * Reads the configuration for the given application and returns it as a String.
     *
     * @param appConfig The application's config for which the configuration needs to be read.
     * @return The configuration for the given application as a String.
     * @throws IOException If an I/O error occurs while reading the configuration.
     */
    String readConf(String appConfig) throws IOException;

    /**
     * Retrieves the main configuration value for the given Application.
     *
     * @param appParam the Application object for which to fetch the main configuration value
     * @return the main configuration value as a String
     */
    String getMain(FlinkApplication appParam);

    /**
     * Returns the dashboard for the specified team.
     *
     * @param teamId the ID of the team
     * @return a map containing the dashboard data
     */
    Map<String, Serializable> getDashboardDataMap(Long teamId);

    /**
     * Retrieves the Kubernetes start log for a specific ID with an optional offset and limit.
     *
     * @param id The ID of the Kubernetes resource.
     * @param offset The offset to start fetching log lines from.
     * @param limit The maximum number of log lines to fetch.
     * @return The Kubernetes start log as a string.
     * @throws Exception if an error occurs while retrieving the log.
     */
    String k8sStartLog(Long id, Integer offset, Integer limit) throws Exception;

    /**
     * Retrieves the list of recent Kubernetes namespaces.
     *
     * @return The list of recent Kubernetes namespaces as a List of Strings.
     */
    List<String> listRecentK8sNamespace();

    /**
     * Retrieves the list of recent K8s cluster IDs based on the specified execution mode.
     *
     * @param deployMode The execution mode to filter the recent K8s cluster IDs. 1: Production
     *     mode 2: Test mode 3: Development mode -1: All modes
     * @return The list of recent K8s cluster IDs based on the specified execution mode.
     */
    List<String> listRecentK8sClusterId(Integer deployMode);

    /**
     * Retrieves the list of recent Flink base images.
     *
     * @return a list of strings representing the recent Flink base images
     */
    List<String> listRecentFlinkBaseImage();

    /**
     * Retrieves the recent K8s pod templates.
     *
     * @return a List of Strings representing the recent K8s pod templates.
     */
    List<String> listRecentK8sPodTemplate();

    /**
     * Retrieves the list of recent Kubernetes Job Manager Pod templates.
     *
     * @return A List of string values representing the recent Kubernetes Job Manager Pod templates.
     */
    List<String> listRecentK8sJmPodTemplate();

    /**
     * Retrieves the list of recent K8s TM pod templates.
     *
     * @return The list of recent K8s TM pod templates as a List of String objects.
     */
    List<String> listRecentK8sTmPodTemplate();

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
