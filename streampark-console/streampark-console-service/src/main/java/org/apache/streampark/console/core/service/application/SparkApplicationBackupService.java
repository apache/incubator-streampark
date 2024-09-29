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

import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.exception.InternalException;
import org.apache.streampark.console.core.entity.SparkApplication;
import org.apache.streampark.console.core.entity.SparkApplicationBackup;
import org.apache.streampark.console.core.entity.SparkSql;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/** Interface representing a service for application backup operations. */
public interface SparkApplicationBackupService extends IService<SparkApplicationBackup> {

    /**
     * Deletes an object specified by the given ID.
     *
     * @param id The ID of the object to delete.
     * @return true if the object was successfully deleted, false otherwise.
     * @throws InternalException if an internal error occurs during the deletion process.
     */
    Boolean removeById(Long id) throws InternalException;

    /**
     * Performs a backup for the given application and Spark SQL parameters.
     *
     * @param appParam The application to back up.
     * @param sparkSqlParam The Spark SQL to back up.
     */
    void backup(SparkApplication appParam, SparkSql sparkSqlParam);

    /**
     * Retrieves a page of {@link SparkApplicationBackup} objects based on the provided parameters.
     *
     * @param bakParam The {@link SparkApplicationBackup} object containing the search criteria.
     * @param request The {@link RestRequest} object used for pagination and sorting.
     * @return An {@link IPage} containing the retrieved {@link SparkApplicationBackup} objects.
     */
    IPage<SparkApplicationBackup> getPage(SparkApplicationBackup bakParam, RestRequest request);

    /**
     * Rolls back the changes made by the specified application backup.
     *
     * @param bakParam The SparkApplicationBackUp object representing the backup to roll back.
     */
    void rollback(SparkApplicationBackup bakParam);

    /**
     * Revoke the given application.
     *
     * @param appParam The application to be revoked.
     */
    void revoke(SparkApplication appParam);

    /**
     * Removes the specified application.
     *
     * @param appParam the application to be removed
     */
    void remove(SparkApplication appParam);

    /**
     * Rolls back a Spark SQL application to its previous state.
     *
     * @param appParam The application to rollback.
     * @param sparkSqlParam The Spark SQL instance associated with the application.
     */
    void rollbackSparkSql(SparkApplication appParam, SparkSql sparkSqlParam);
}
