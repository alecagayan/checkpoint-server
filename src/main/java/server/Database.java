package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

import org.json.JSONObject;

/***
 * Interface between API and database.
 */
public class Database {
    private static final String url = "jdbc:mysql://localhost:3306/checkpoint";
    private static final String username = "checkin";
    private static final String password = "Chkpntuser!23";
    private static Connection conn = null;

    public Database() {
        try {
            // load driver
            conn = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // username and account in
    // 0 = success
    // 1 = error
    public int checkIn(String student_id, String account) {
        int result = 1;
        int count = 0;
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM logs WHERE student_id = ?");
            //stmt.setString(1, account);
            stmt.setString(1, student_id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                //add one to the count and append current time to the database
                count = rs.getInt("count") + 1;
                PreparedStatement stmt2 = conn.prepareStatement("UPDATE logs SET count = ? WHERE student_id = ?");
                //stmt2.setString(1, account);
                stmt2.setInt(1, count);
                stmt2.setString(2, student_id);
                stmt2.executeUpdate();

                //append the current date and time to meetings column
                PreparedStatement stmt3 = conn.prepareStatement("UPDATE logs SET meetings = CONCAT(meetings, ',', CURRENT_TIMESTAMP) WHERE student_id = ?");
                //stmt3.setString(1, account);
                stmt3.setString(1, student_id);
                stmt3.executeUpdate();

                //append the student_id to the students column in the meetings table
                PreparedStatement stmt4 = conn.prepareStatement("UPDATE meetings SET students = CONCAT(students, ',', ?) WHERE meeting = curdate()");
                stmt4.setString(1, student_id);
                stmt4.executeUpdate();
                result = 0;

            } else {
                //create a new row in the database
                PreparedStatement stmt5 = conn.prepareStatement("INSERT INTO logs (student_id, count, meetings) VALUES (?, 1, CURRENT_TIMESTAMP)");
                //stmt4.setString(1, account);
                stmt5.setString(1, student_id);
                stmt5.executeUpdate();

                //append the student_id to the students column in the meetings table
                PreparedStatement stmt6 = conn.prepareStatement("UPDATE meetings SET students = CONCAT(students, ',', ?) WHERE meeting = curdate()");
                stmt6.setString(1, student_id);
                stmt6.executeUpdate();
                result = 0;

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public String getAttendees() {
        //get the students column from the meetings table
        String result = "";
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT students FROM meetings WHERE meeting = curdate()");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                result = rs.getString("students");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public int getPercentages() {
        //get the amount of meetings from the meetings table
        // each row is a meeting
        int meetings = 0;
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM meetings");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                meetings = rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
    }

    return meetings;
    }



    public int startMeeting() {
        //create new row in meetings table with current date 
        int result = 1;
        try {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO meetings (meeting, students) VALUES (curdate(), '0000000')");
            stmt.executeUpdate();
            result = 0;
        } catch (SQLException e) {
            e.printStackTrace();

        }
        return result;
    }

    public int checkMeeting() {
        //check if there is a meeting today
        int result = 1;
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM meetings WHERE meeting = curdate()");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                result = 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

}