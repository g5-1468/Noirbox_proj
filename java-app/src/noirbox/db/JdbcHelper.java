package noirbox.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Small helper class that removes repeated JDBC code from the DAO classes.
 * It uses a generic interface (RowMapper) plus a lambda in every DAO, which
 * keeps the code short and type safe.
 */
public final class JdbcHelper {

    private JdbcHelper() {
    }

    /** Converts one row of a ResultSet into a Java object. */
    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet resultSet) throws SQLException;
    }

    /** Runs a SELECT and returns a list of objects. */
    public static <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        List<T> rows = new ArrayList<>();
        try (var connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(mapper.map(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Query failed: " + e.getMessage(), e);
        }
        return rows;
    }

    /** Runs an INSERT and returns the generated primary key (0 when none). */
    public static int insert(String sql, Object... params) {
        try (var connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            bind(statement, params);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Insert failed: " + e.getMessage(), e);
        }
    }

    /** Runs INSERT / UPDATE / DELETE and returns the number of affected rows. */
    public static int update(String sql, Object... params) {
        try (var connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            bind(statement, params);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Update failed: " + e.getMessage(), e);
        }
    }

    private static void bind(PreparedStatement statement, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
    }
}
