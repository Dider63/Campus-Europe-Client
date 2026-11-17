package org.example.config;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

/**
 * Verifies the configured Oracle connection and confirms that the application
 * is connected to the schema containing the existing Visa Agency tables.
 * Passwords and password hashes are never logged.
 */
@Component
public class OracleDatabaseStartupCheck implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(OracleDatabaseStartupCheck.class);
    private final JdbcTemplate jdbc;
    private final Environment environment;

    public OracleDatabaseStartupCheck(JdbcTemplate jdbc, Environment environment) {
        this.jdbc = jdbc;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String configuredPassword = environment.getProperty("spring.datasource.password");
        if (configuredPassword == null || configuredPassword.isBlank()) {
            throw new IllegalStateException(
                    "Oracle DB_PASSWORD is empty. In IntelliJ Run > Edit Configurations > Environment variables, " +
                            "set DB_PASSWORD to the same password that succeeds in SQL Developer. The password is not stored in the project.");
        }
        String jdbcUrl = environment.getProperty("spring.datasource.url", "");
        String connectionMode = jdbcUrl.contains("@//") ? "service-name" : "SID/descriptor";
        String dbUser = environment.getProperty("spring.datasource.username", "(not configured)");
        log.info("Checking Oracle datasource using {} connection format as user={}", connectionMode, dbUser);
        try {
            String sessionUser = jdbc.queryForObject("SELECT USER FROM DUAL", String.class);
            String currentSchema = jdbc.queryForObject(
                    "SELECT SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') FROM DUAL", String.class);
            Integer accounts = jdbc.queryForObject("SELECT COUNT(*) FROM user_account", Integer.class);
            Integer roles = jdbc.queryForObject("SELECT COUNT(*) FROM app_role", Integer.class);

            log.info("Oracle connection verified. Session user={}, current schema={}, USER_ACCOUNT rows={}, APP_ROLE rows={}",
                    sessionUser, currentSchema, accounts, roles);
            if (accounts == null || accounts == 0) {
                log.warn("USER_ACCOUNT is accessible but contains no users. Run the project's account setup/repair SQL if this is a fresh development schema.");
            }
            Integer validRoles = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM app_role WHERE role_name IN ('USER', 'CONSULTANT', 'ADMIN')", Integer.class);
            if (roles == null || roles != 3 || validRoles == null || validRoles != 3) {
                log.error("AUTH CONFIGURATION ERROR: APP_ROLE must contain exactly USER, CONSULTANT, ADMIN; " +
                        "currently {} total roles and {} expected names. Run database/migrate_three_roles.sql " +
                        "in current schema {} before trying to register or sign in.", roles, validRoles, currentSchema);
            }
            Integer unmapped = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM user_account u WHERE " +
                            "(SELECT COUNT(*) FROM user_role ur WHERE ur.user_id = u.user_id) <> 1", Integer.class);
            if (unmapped != null && unmapped > 0) {
                log.error("AUTH CONFIGURATION ERROR: {} accounts have zero or multiple role assignments. " +
                        "Run database/migrate_three_roles.sql and verify database/diagnose_authentication.sql.", unmapped);
            }
        } catch (DataAccessException ex) {
            SQLException sqlException = findSqlException(ex);
            String oracleDetail = describeOracleFailure(sqlException);
            log.error("Oracle startup verification failed. {} Check DB_URL, DB_USERNAME, DB_PASSWORD, Oracle listener status, and the schema that owns USER_ACCOUNT and APP_ROLE.",
                    oracleDetail, ex);
            throw new IllegalStateException(
                    "Cannot start Visa Processing Agency because Oracle verification failed. " + oracleDetail +
                            " See the first ORA- error in the application log and follow docs/ORACLE_CONNECTION_FIX.md.", ex);
        }
    }

    private static SQLException findSqlException(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                if (sqlException.getErrorCode() != 0) return sqlException;
                SQLException next = sqlException.getNextException();
                if (next != null) return next;
            }
            current = current.getCause();
        }
        return null;
    }

    private static String describeOracleFailure(SQLException error) {
        if (error == null) return "No Oracle SQLException was exposed by the JDBC driver; inspect the nested exception in the stack trace.";
        int code = Math.abs(error.getErrorCode());
        String hint = switch (code) {
            case 1017 -> "Invalid Oracle username/password (ORA-01017). Re-enter DB_USERNAME and DB_PASSWORD exactly as used by SQL Developer.";
            case 12505 -> "The listener does not know the configured SID (ORA-12505). Match the SID selected in SQL Developer; SID URL example: jdbc:oracle:thin:@localhost:1521:XE.";
            case 12514 -> "The listener does not know the configured service name (ORA-12514). Match the Service name selected in SQL Developer; service URL example: jdbc:oracle:thin:@//localhost:1521/xe.";
            case 12541 -> "The Oracle listener refused the connection (ORA-12541). Start/check the Oracle listener and confirm port 1521.";
            case 17002 -> "Oracle JDBC could not establish the network connection (error 17002). Check that the listener is running on localhost:1521, then test the same SID/service in SQL Developer.";
            case 28000 -> "The Oracle account is locked (ORA-28000). Unlock the database account using an authorized DBA connection.";
            case 28001 -> "The Oracle password has expired (ORA-28001). Change it in Oracle SQL Developer and update DB_PASSWORD.";
            case 942 -> "A required table is missing or not visible (ORA-00942). Connect using the schema owner that contains USER_ACCOUNT and APP_ROLE.";
            default -> "Oracle JDBC error code " + code + " (SQLState " + error.getSQLState() + ").";
        };
        return hint + " Driver message: " + safeMessage(error.getMessage());
    }

    private static String safeMessage(String message) {
        if (message == null || message.isBlank()) return "(no driver message)";
        // Do not print connect descriptors if a user has embedded credentials in a URL.
        return message.replaceAll("(?i)(password\\s*=\\s*)[^,; )]+", "$1[redacted]");
    }
}

