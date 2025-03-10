/*
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
package io.trino.plugin.clickhouse;

import org.testcontainers.containers.ClickHouseContainer;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static java.lang.String.format;
import static org.testcontainers.containers.ClickHouseContainer.HTTP_PORT;
import static org.testcontainers.utility.MountableFile.forClasspathResource;

public class TestingClickHouseServer
        implements Closeable
{
    private static final String CLICKHOUSE_IMAGE = "yandex/clickhouse-server:20.8";
    private final ClickHouseContainer dockerContainer;

    public TestingClickHouseServer()
    {
        // Use 2nd stable version
        dockerContainer = (ClickHouseContainer) new ClickHouseContainer(CLICKHOUSE_IMAGE)
                .withCopyFileToContainer(forClasspathResource("custom.xml"), "/etc/clickhouse-server/config.d/custom.xml")
                .withStartupAttempts(10);

        dockerContainer.start();
    }

    public void execute(String sql)
    {
        try (Connection connection = DriverManager.getConnection(getJdbcUrl());
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to execute statement: " + sql, e);
        }
    }

    public String getJdbcUrl()
    {
        return format("jdbc:clickhouse://%s:%s/", dockerContainer.getContainerIpAddress(),
                dockerContainer.getMappedPort(HTTP_PORT));
    }

    @Override
    public void close()
    {
        dockerContainer.stop();
    }
}
