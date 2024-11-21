package eu.europeana.processing;


import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import eu.europeana.processing.job.JobParamName;
import org.apache.flink.api.java.utils.ParameterTool;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;

public class DbConnectionProvider implements Serializable, AutoCloseable {

    private final HikariDataSource dataSource;


    public DbConnectionProvider(ParameterTool parameterTool) {
        HikariConfig config=new HikariConfig();
        config.setJdbcUrl(parameterTool.getRequired(JobParamName.DATASOURCE_URL));
        config.setUsername(parameterTool.get(JobParamName.DATASOURCE_USERNAME));
        config.setPassword(parameterTool.get(JobParamName.DATASOURCE_PASSWORD));
        config.setMaximumPoolSize(1);
        dataSource = new HikariDataSource(config);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() throws Exception {
        dataSource.close();
    }
}
