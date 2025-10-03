package eu.europeana.processing;


import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import eu.europeana.processing.job.JobParamName;
import java.io.Serial;
import org.apache.flink.util.ParameterTool;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbConnectionProvider implements Serializable, AutoCloseable {

  private static final Logger LOGGER = LoggerFactory.getLogger(DbConnectionProvider.class);
    @Serial
    private static final long serialVersionUID = 1;

    private final HikariDataSource dataSource;


    public DbConnectionProvider(ParameterTool parameterTool) {
        HikariConfig config=new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl(parameterTool.getRequired(JobParamName.DATASOURCE_URL));
        config.setUsername(parameterTool.get(JobParamName.DATASOURCE_USERNAME));
        config.setPassword(parameterTool.get(JobParamName.DATASOURCE_PASSWORD));
        config.addDataSourceProperty("ApplicationName", "metis-processing-engine");
        config.setMaximumPoolSize(1);
        dataSource = new HikariDataSource(config);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
      if (!dataSource.isClosed()) {
        LOGGER.debug("Closing DB connection provider");
        dataSource.close();
      } else {
        LOGGER.debug("ConnectionProvider already closed");
      }
    }
}
