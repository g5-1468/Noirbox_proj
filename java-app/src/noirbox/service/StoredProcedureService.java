package noirbox.service;

import noirbox.db.DBConnection;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Calls the MySQL stored procedures with CallableStatement.
 * This is the bridge between the Java project and the DBMS project:
 * the procedure sp_book_ticket does the booking completely inside MySQL.
 */
public class StoredProcedureService {

    /** CALL sp_book_ticket(...) - returns the new booking id. */
    public int bookWithProcedure(int userId, int showId, String seats, String method)
            throws SQLException {

        try (Connection connection = DBConnection.getConnection();
             CallableStatement call = connection.prepareCall(
                     "{CALL sp_book_ticket(?, ?, ?, ?, ?, ?)}")) {

            call.setInt(1, userId);
            call.setInt(2, showId);
            call.setString(3, seats);
            call.setString(4, method);
            call.registerOutParameter(5, Types.INTEGER);
            call.registerOutParameter(6, Types.DECIMAL);

            call.execute();

            while (call.getMoreResults()) {
                // sp_book_ticket returns no result sets, only the OUT parameters
            }

            System.out.printf("Stored procedure total: Rs.%.2f%n", call.getDouble(6));
            return call.getInt(5);
        }
    }

    /** CALL sp_theatre_report('Mumbai') - the procedure uses a cursor. */
    public void showTheatreReport(String city) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             CallableStatement call = connection.prepareCall("{CALL sp_theatre_report(?)}")) {
            call.setString(1, city);
            printResultSets(call);
        }
    }

    /** CALL sp_movie_show_report(1) - cursor report of all shows. */
    public void showMovieReport(int movieId) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             CallableStatement call = connection.prepareCall("{CALL sp_movie_show_report(?)}")) {
            call.setInt(1, movieId);
            printResultSets(call);
        }
    }

    /**
     * A procedure can return several result sets (one SELECT per cursor row).
     * execute() only starts it, so each result set is read and printed here.
     */
    private void printResultSets(CallableStatement call) throws SQLException {
        boolean hasResult = call.execute();

        while (true) {
            if (hasResult) {
                try (ResultSet resultSet = call.getResultSet()) {
                    ResultSetMetaData metaData = resultSet.getMetaData();
                    while (resultSet.next()) {
                        StringBuilder line = new StringBuilder("  ");
                        for (int column = 1; column <= metaData.getColumnCount(); column++) {
                            line.append(resultSet.getString(column)).append(' ');
                        }
                        System.out.println(line);
                    }
                }
            } else if (call.getUpdateCount() == -1) {
                break;
            }
            hasResult = call.getMoreResults();
        }
    }

    /** CALL sp_cancel_booking(...) - cancellation through the procedure. */
    public String cancelWithProcedure(int bookingId) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             CallableStatement call = connection.prepareCall(
                     "{CALL sp_cancel_booking(?, ?)}")) {

            call.setInt(1, bookingId);
            call.registerOutParameter(2, Types.VARCHAR);
            call.execute();
            return call.getString(2);
        }
    }
}
