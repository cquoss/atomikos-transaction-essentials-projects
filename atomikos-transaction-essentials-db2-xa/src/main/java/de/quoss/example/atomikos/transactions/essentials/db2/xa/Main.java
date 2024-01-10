package de.quoss.example.atomikos.transactions.essentials.db2.xa;

import com.atomikos.icatch.jta.UserTransactionManager;
import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.ibm.db2.jcc.DB2XADataSource;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static final AtomikosDataSourceBean DATA_SOURCE_BEAN = createDataSourceBean();

    private static final UserTransactionManager TRANSACTION_MANAGER = new UserTransactionManager();

    public static void main(final String[] args) {
        new Main().run();
    }

    private static AtomikosDataSourceBean createDataSourceBean() {
        final AtomikosDataSourceBean result = new AtomikosDataSourceBean();
        final DB2XADataSource dataSource = new DB2XADataSource();
        dataSource.setDriverType(4);
        dataSource.setServerName("zdev.ude.geno.lan");
        dataSource.setPortNumber(481);
        dataSource.setUser("dvdudbt");
        dataSource.setPassword("all4you");
        dataSource.setSslConnection(true);
        dataSource.setDatabaseName("UIG2_DSN");
        result.setXaDataSource(dataSource);
        result.setUniqueResourceName("db2-xa");
        result.setMinPoolSize(10);
        result.setMaxPoolSize(30);
        return result;
    }

    private List<Map<String, Object>> execute(final String sql) {
        final List<Map<String, Object>> result = new LinkedList<>();
        try (final Connection connection = getConnection();
             final Statement statement = connection.createStatement()) {
            if (statement.execute(sql)) {
                final ResultSet resultSet = statement.getResultSet();
                final ResultSetMetaData metaData = resultSet.getMetaData();
                while (resultSet.next()) {
                    final Map<String, Object> columns = new HashMap<>(metaData.getColumnCount());
                    for (int i = 1; i <= metaData.getColumnCount(); i++) {
                        columns.put(metaData.getColumnName(i), resultSet.getObject(i));
                    }
                    result.add(columns);
                }
            } else {
                result.add(Map.of("UPDATE_COUNT", statement.getUpdateCount()));
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
        if (result.isEmpty()) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(result);
        }
    }

    private Connection getConnection() {
        final Connection result;
        try {
            result = DATA_SOURCE_BEAN.getConnection();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
        LOGGER.info("Connection: {}", result);
        return result;
    }

    private void getLogConnection() {
        try (final Connection connection = getConnection()) {
        } catch (final SQLException e) {
            try {
                TRANSACTION_MANAGER.rollback();
            } catch (final SystemException e2) {
                throw new RuntimeException(e2);
            }
            throw new RuntimeException(e);
        }
    }

    private String getStatus() {
        try {
            return switch (TRANSACTION_MANAGER.getStatus()) {
                case Status.STATUS_ACTIVE -> "ACTIVE";
                case Status.STATUS_MARKED_ROLLBACK -> "MARKED ROLLBACK";
                case Status.STATUS_PREPARED -> "PREPARED";
                case Status.STATUS_COMMITTED -> "COMMITTED";
                case Status.STATUS_ROLLEDBACK -> "ROLLEDBACK";
                case Status.STATUS_UNKNOWN -> "UNKNOWN";
                case Status.STATUS_NO_TRANSACTION -> "NO TRANSACTION";
                case Status.STATUS_PREPARING -> "PREPARING";
                case Status.STATUS_COMMITTING -> "COMMITTING";
                case Status.STATUS_ROLLING_BACK -> "ROLLING BACK";
                default -> "UNDEFINED";
            };
        } catch (final SystemException e) {
            throw new RuntimeException(e);
        }
    }

    private void run() {
        try {
            TRANSACTION_MANAGER.init();
        } catch (final SystemException e) {
            throw new RuntimeException(e);
        }
        LOGGER.info("After init(): {}", getStatus());
        try {
            TRANSACTION_MANAGER.begin();
        } catch (final NotSupportedException | SystemException e) {
            throw new RuntimeException(e);
        }
        LOGGER.info("After begin(): {}", getStatus());
        final String select = "select * from ukd fetch first 1 rows only";
        LOGGER.info("{}", execute(select));
        LOGGER.info("After first select: {}", getStatus());
        getLogConnection();
        LOGGER.info("After log connection: {}", getStatus());
        LOGGER.info("{}", execute(select));
        LOGGER.info("After second select: {}", getStatus());
        try {
            TRANSACTION_MANAGER.commit();
        } catch (final HeuristicMixedException | HeuristicRollbackException | RollbackException | SystemException e) {
            throw new RuntimeException(e);
        }
        LOGGER.info("After commit(): {}", getStatus());
        TRANSACTION_MANAGER.close();
        LOGGER.info("After close(): {}", getStatus());
    }

}
