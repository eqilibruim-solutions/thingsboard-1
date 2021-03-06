/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.service.install;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.util.PsqlDao;
import org.thingsboard.server.dao.util.TimescaleDBTsDao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Service
@TimescaleDBTsDao
@PsqlDao
@Profile("install")
@Slf4j
public class TimescaleTsDatabaseSchemaService extends SqlAbstractDatabaseSchemaService implements TsDatabaseSchemaService {

    private static final String QUERY = "query: {}";
    private static final String SUCCESSFULLY_EXECUTED = "Successfully executed ";
    private static final String FAILED_TO_EXECUTE = "Failed to execute ";
    private static final String FAILED_DUE_TO = " due to: {}";

    private static final String SUCCESSFULLY_EXECUTED_QUERY = SUCCESSFULLY_EXECUTED + QUERY;
    private static final String FAILED_TO_EXECUTE_QUERY = FAILED_TO_EXECUTE + QUERY + FAILED_DUE_TO;

    @Value("${sql.timescale.chunk_time_interval:86400000}")
    private long chunkTimeInterval;

    public TimescaleTsDatabaseSchemaService() {
        super("schema-timescale.sql", "schema-timescale-idx.sql");
    }

    @Override
    public void createDatabaseSchema() throws Exception {
        super.createDatabaseSchema();
        executeQuery("SELECT create_hypertable('tenant_ts_kv', 'ts', chunk_time_interval => " + chunkTimeInterval + ", if_not_exists => true);");
    }

    private void executeQuery(String query) {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
            conn.createStatement().execute(query); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
            log.info(SUCCESSFULLY_EXECUTED_QUERY, query);
            Thread.sleep(5000);
        } catch (InterruptedException | SQLException e) {
            log.info(FAILED_TO_EXECUTE_QUERY, query, e.getMessage());
        }
    }


}