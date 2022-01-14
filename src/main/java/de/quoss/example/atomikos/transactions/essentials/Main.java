package de.quoss.example.atomikos.transactions.essentials;

import com.atomikos.icatch.jta.UserTransactionManager;
import com.atomikos.jdbc.AtomikosDataSourceBean;
import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private void run() throws HeuristicMixedException, HeuristicRollbackException, RollbackException, NotSupportedException, SQLException, SystemException {

        // Set up h2 xa data source
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setUser("sa");
        dataSource.setPassword("");
        dataSource.setURL("jdbc:h2:~/test");

        // Atomikos implementations
        UserTransactionManager utm = new UserTransactionManager();
        AtomikosDataSourceBean adsb = new AtomikosDataSourceBean();
        adsb.setXaDataSource(dataSource);
        adsb.setUniqueResourceName("atomikosJdbc");

        // Standard interfaces
        TransactionManager tm = utm;
        DataSource ds = adsb;

        // init
        utm.init();

        // begin transaction
        tm.begin();

        // do something on database
        try (Connection connection = ds.getConnection();
             Statement statement = connection.createStatement()) {
            // drop table if exists
            String sql = "drop table customer if exists";
            statement.execute(sql);
            // create table
            sql = "create table customer( id integer primary key, name varchar(50) )";
            statement.execute(sql);
            // insert customer zero
            sql = "insert into customer values ( 0, 'Clemens Quoß' )";
            statement.execute(sql);
        } catch (SQLException e) {
            tm.rollback();
            throw e;
        } finally {
            if (tm.getTransaction().getStatus() == Status.STATUS_ACTIVE) {
                tm.commit();
            }
        }

        // shut down
        adsb.close();
        utm.close();

    }

    public static void main(final String[] args) {
        try {
            new Main().run();
        } catch (HeuristicMixedException | HeuristicRollbackException | NotSupportedException | RollbackException | SQLException | SystemException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
            System.exit(1);
        }
    }

}
