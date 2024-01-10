package de.quoss.example.atomikos.transactions.essentials.h2;

import com.atomikos.icatch.jta.UserTransactionManager;
import com.atomikos.jdbc.AtomikosDataSourceBean;
import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        dataSource.setURL("jdbc:h2:tcp://localhost/C:/Users/Cleme/h2/test");

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

        // begin transaction, default timout
        tm.begin();

        // insert into database (customer table)
        try (Connection dbConnection = ds.getConnection();
            Statement statement = dbConnection.createStatement()) {

            // drop table if exists
            String sql = "drop table customer if exists";
            statement.execute(sql);
            // create table
            sql = "create table customer( id integer primary key, name varchar(50) )";
            statement.execute(sql);

            // insert customer
            final int id = 0;
            final String name = "Clemens";
            sql = String.format("insert into customer values ( %s, '%s' )", id, name);
            statement.execute(sql);

        } catch (final SQLException e) {

            // roll back user transaction on error
            tm.rollback();
            throw e;

        } finally {

            // commit transaction if still active
            Transaction tx = tm.getTransaction();
            if (tx != null && tx.getStatus() == Status.STATUS_ACTIVE) {
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
        } catch (HeuristicMixedException | HeuristicRollbackException |  NotSupportedException | RollbackException | SQLException | SystemException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
            System.exit(1);
        }
    }

}
