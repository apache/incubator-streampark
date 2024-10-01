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

import org.apache.streampark.console.core.bean.DatabaseParam;
import org.apache.streampark.console.core.entity.Database;

import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface DatabaseService extends IService<Database> {

    /**
     * Checks if the specified database exists.
     *
     * @param databaseParam The database to check
     * @return true if the database exists, false otherwise
     */
    boolean databaseExists(DatabaseParam databaseParam);

    /**
     * Creates a new database given {@link Database}.
     *
     * @param databaseParam The {@link DatabaseParam} object that contains the detail of the created
     *     database
     * @return true if the operation is successful, false otherwise
     */
    boolean createDatabase(DatabaseParam databaseParam);

    /**
     * Lists databases given catalog id.
     *
     * @return The list of databases of given catalog
     */
    List<DatabaseParam> listDatabases(Long catalogId);

    /**
     * Drops database given database name.
     *
     * @param databaseParam The dropping database
     * @return true if the operation is successful, false otherwise
     */
    boolean dropDatabase(DatabaseParam databaseParam);
}
