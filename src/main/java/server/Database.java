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

    // username and password in
    // 0 = success
    // 1 = error
    public int logIn(String login, String password) {
        int result = 1;
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE login = ? AND password = ?");
            stmt.setString(1, login);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                result = 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // id, name, email, registrationcode in
    // 0 = success
    // 1 = error
    public int register(String id, String name, String email, String registrationCode) {
        int result = 1;
        System.out.println("registration code: " + registrationCode);
        if(registrationCode.equals("FRC116")) {
            try {
                // check if user exists
                PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
                stmt.setString(1, id);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    result = 1;
                } else {
                    // add user
                    stmt = conn.prepareStatement("INSERT INTO users (login, name, email) VALUES (?, ?, ?)");
                    stmt.setString(1, id);
                    stmt.setString(2, name);
                    stmt.setString(3, email);
                    stmt.executeUpdate();
                    result = 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    // username, meetingId in
    // 0 = success
    // 1 = error
    public int checkIn(String username, String meetingId) {
        int result = 1;
        try {
            //get attendee_id from users table
            PreparedStatement stmt = conn.prepareStatement("SELECT id FROM users WHERE login = ?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String attendeeId = rs.getString("id");
                // insert into attendees table meeting_id, attendee_id
                stmt = conn.prepareStatement("INSERT INTO attendees (meeting_id, attendee_id) VALUES (?, ?)");
                stmt.setString(1, meetingId);
                stmt.setString(2, attendeeId);
                stmt.executeUpdate();
                result = 0;

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // name, email, username in
    // 0 = success
    // 1 = error
    public int addUser(String name, String email, String username) {
        int result = 1;
        if (name == null || name.trim().length() == 0) {
            return result;
        }
        if (email == null || email.trim().length() == 0) {
            return result;
        }
        if (username == null || username.trim().length() == 0) {
            return result;
        }
        try {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (name, email, login) VALUES (?, ?, ?)");
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, username);
            stmt.executeUpdate();
            result = 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // return all users in JSON format
    public String getUsers() {
        String result = "";
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result += "{\"id\":\"" + rs.getString("id") + "\",\"name\":\"" + rs.getString("name") + "\",\"email\":\"" + rs.getString("email") + "\",\"login\":\"" + rs.getString("login") + "\"},";
            }
            if (result.length()>0) {
                result = result.substring(0, result.length() - 1);
            }
            result = "[" + result + "]";
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }


    // return all meetings in JSON format
    public String getMeetings() {
        String result = "";
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT m.id, m.opentime, m.closetime, m.location, COUNT(a.id) as attendees FROM meetings m LEFT JOIN attendees a ON m.id = a.meeting_id GROUP BY m.id ORDER BY m.opentime ASC");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result += "{\"id\":\"" + rs.getInt("id") + 
                            "\",\"opentime\":\"" + emptyIfNull(rs.getTimestamp("opentime")) + 
                            "\",\"closetime\":\"" + emptyIfNull(rs.getTimestamp("closetime")) + 
                            "\",\"location\":\"" + rs.getString("location") + 
                            "\",\"attendees\":\"" + rs.getString("attendees") + 
                            "\"},";
            }
            if (result.length()>0) {
                result = result.substring(0, result.length() - 1);
            }
            result = "[" + result + "]";
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // create meeting
    // 0 = success
    // 1 = error
    public int createMeeting() {
        int result = 1;
        try {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO meetings (opentime) VALUES (now())");
            stmt.executeUpdate();
            result = 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }


    // username and account in
    // 0 = success
    // 1 = error
    public int checkIn_old(String student_id, String account) {
        int result = 1;
        int count = 0;
        String mtngresult = "";
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM logs WHERE student_id = ?");
            //stmt.setString(1, account);
            stmt.setString(1, student_id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {

                //check if student_id is in meeting
                PreparedStatement mtngstmt = conn.prepareStatement("SELECT students FROM meetings WHERE meeting = curdate()");
                ResultSet mtrs = mtngstmt.executeQuery();
                if (mtrs.next()) {
                    mtngresult = rs.getString("students");
                }

                // check if student_id is in result
                if (!mtngresult.contains(student_id)) {


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
                }

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

    public String getStudents() {
        //get every student_id from the logs table
        String result = "";
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT student_id FROM logs");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result += rs.getString("student_id") + ",";
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public String getPercentages(int percent) {
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

    double meetingThreshold = meetings * (percent / 100.0);

    //go through every student in logs table and check if their meeting count is above the meeting threshold
    //if so, add them to the result string
        String result = "";
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM logs");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                if (rs.getInt("count") > meetingThreshold) {
                    result += rs.getString("student_id") + ",";
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }



    public int startMeeting_old() {
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

    private String emptyIfNull(Object o) {
        if (o == null) {
            return "";
        } else {
            return o.toString();
        }
    }

}