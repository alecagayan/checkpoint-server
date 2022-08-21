package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;


/***
 * Interface between API and database.
 */
public class Database {
    private static final String url = "jdbc:mysql://checkpointdb:3306/checkpoint";
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
        if (registrationCode.equals("FRC116")) {
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
            // get attendee_id from users table
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
            // select all users from users table and select attendee count from attendees
            // table and join
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT users.id, users.name, users.email, users.login, COUNT(attendees.attendee_id) AS attendee_count FROM users LEFT JOIN attendees ON users.id = attendees.attendee_id GROUP BY users.id");
            ResultSet rs = stmt.executeQuery();
            /*
             * PreparedStatement meeting_count =
             * conn.prepareStatement("SELECT COUNT(id) AS meeting_count FROM meetings");
             * ResultSet meeting_rs = meeting_count.executeQuery();
             * 
             * int countmeeting = 1;
             * while (meeting_rs.next()) {
             * countmeeting = meeting_rs.getInt("meeting_count");
             * System.out.println(countmeeting);
             * }
             * if (countmeeting == 0) {
             * 
             * }
             */
            while (rs.next()) {
                result += "{\"id\":\"" + rs.getString("id") +
                        "\",\"name\":\"" + rs.getString("name") +
                        "\",\"email\":\"" + rs.getString("email") +
                        "\",\"login\":\"" + rs.getString("login") + "\"},";
                // "\",\"attendee_count\":\"" +
                // String.valueOf(rs.getInt("attendee_count")/(countmeeting+0.0)*100) + "%" +
                // "\"},";
            }
            if (result.length() > 0) {
                result = result.substring(0, result.length() - 1);
            }
            result = "[" + result + "]";
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // return all users and meeting percentage between given dates in JSON format
    public String getUsersBetweenDates(String startDate, String endDate) {
        String result = "";
        try {
            System.out.println("start date: " + startDate);
            System.out.println("end date: " + endDate);

            // select all users from users table and select attendee count from attendees
            // table and join
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT users.id, users.name, users.email, users.login, COUNT(attendees.attendee_id) AS attendee_count FROM users LEFT JOIN attendees ON users.id = attendees.attendee_id WHERE attendees.meeting_id IN (SELECT id FROM meetings WHERE opentime >= ? AND opentime  <= ?) GROUP BY users.id order by users.name asc");
            stmt.setString(1, startDate);
            stmt.setString(2, endDate);
            ResultSet rs = stmt.executeQuery();

            PreparedStatement meeting_count = conn.prepareStatement(
                    "SELECT COUNT(id) AS meeting_count FROM meetings WHERE opentime >= ? AND opentime <= ?");
            meeting_count.setString(1, startDate);
            meeting_count.setString(2, endDate);
            ResultSet meeting_rs = meeting_count.executeQuery();

            int countmeeting = 1;
            while (meeting_rs.next()) {
                countmeeting = meeting_rs.getInt("meeting_count");
                System.out.println(countmeeting);
            }

            while (rs.next()) {
                result += "{\"id\":\"" + rs.getString("id") +
                        "\",\"name\":\"" + rs.getString("name") +
                        "\",\"email\":\"" + rs.getString("email") +
                        "\",\"login\":\"" + rs.getString("login") +
                        "\",\"attendee_count\":\""
                        + String.valueOf(rs.getInt("attendee_count") / (countmeeting + 0.0) * 100) + "\"},";
            }
            if (result.length() > 0) {
                result = result.substring(0, result.length() - 1);
            }
            result = "[" + result + "]";
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

      // return all users and meeting percentage between given dates in JSON format
      public String getUsersBetweenDates(String startDate, String endDate, int limit) {
        String result = "";
        try {
            System.out.println("start date: " + startDate);
            System.out.println("end date: " + endDate);

            // select all users from users table and select attendee count from attendees
            // table and join
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT users.id, users.name, users.email, users.login, COUNT(attendees.attendee_id) AS attendee_count FROM users LEFT JOIN attendees ON users.id = attendees.attendee_id WHERE attendees.meeting_id IN (SELECT id FROM meetings WHERE opentime >= ? AND opentime  <= ?) GROUP BY users.id order by attendee_count desc limit ?");
            stmt.setString(1, startDate);
            stmt.setString(2, endDate);
            stmt.setInt(3, limit);
            ResultSet rs = stmt.executeQuery();

            PreparedStatement meeting_count = conn.prepareStatement(
                    "SELECT COUNT(id) AS meeting_count FROM meetings WHERE opentime >= ? AND opentime <= ?");
            meeting_count.setString(1, startDate);
            meeting_count.setString(2, endDate);
            ResultSet meeting_rs = meeting_count.executeQuery();

            int countmeeting = 1;
            while (meeting_rs.next()) {
                countmeeting = meeting_rs.getInt("meeting_count");
                System.out.println(countmeeting);
            }

            while (rs.next()) {
                result += "{\"id\":\"" + rs.getString("id") +
                        "\",\"name\":\"" + rs.getString("name") +
                        "\",\"email\":\"" + rs.getString("email") +
                        "\",\"login\":\"" + rs.getString("login") +
                        "\",\"attendee_count\":\""
                        + String.valueOf(rs.getInt("attendee_count") / (countmeeting + 0.0) * 100) + "\"},";
            }
            if (result.length() > 0) {
                result = result.substring(0, result.length() - 1);
            }
            result = "[" + result + "]";
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }
  
    public String getRecentMeetings(int limit) {
        String result = "";

        //get attendee count for last few meetings
        try {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT m.opentime as meeting_time, COUNT(a.attendee_id) as attendee_count FROM attendees a, meetings m WHERE m.id = a.meeting_id group by m.opentime order by m.opentime asc limit ?");
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result += "{\"attendee_count\":\"" + rs.getInt("attendee_count") +
                "\",\"meeting_time\":\"" + rs.getString("meeting_time") + "\"},";
            }
            if (result.length() > 0) {
                result = result.substring(0, result.length() - 1);
            }
            result = "[" + result + "]";
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // return amount of checkins for all users in JSON format
    // each checkin is a row in the attendees table

    // return all meetings in JSON format
    public String getMeetings() {
        String result = "";
        try {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT m.id, m.opentime, m.closetime, m.location, COUNT(a.id) as attendees FROM meetings m LEFT JOIN attendees a ON m.id = a.meeting_id GROUP BY m.id ORDER BY m.opentime ASC");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result += "{\"id\":\"" + rs.getInt("id") +
                        "\",\"opentime\":\"" + emptyIfNull(rs.getTimestamp("opentime")) +
                        "\",\"closetime\":\"" + emptyIfNull(rs.getTimestamp("closetime")) +
                        "\",\"location\":\"" + rs.getString("location") + "\"},";
            }
            if (result.length() > 0) {
                result = result.substring(0, result.length() - 1);
            }
            result = "[" + result + "]";
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // return all attendees in JSON format
    public String getAttendees(String meetingId) {
        String result = "";
        try {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT u.id, u.name, u.login, a.checkintime, a.checkouttime FROM attendees a, users u WHERE a.attendee_id = u.id AND a.meeting_id = ?");
            stmt.setString(1, meetingId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result += "{\"id\":\"" + rs.getString("id") +
                        "\",\"name\":\"" + rs.getString("name") +
                        "\",\"login\":\"" + rs.getString("login") +
                        "\",\"checkintime\":\"" + emptyIfNull(rs.getTimestamp("checkintime")) +
                        "\",\"checkouttime\":\"" + emptyIfNull(rs.getTimestamp("checkouttime")) +
                        "\"},";
            }
            if (result.length() > 0) {
                result = result.substring(0, result.length() - 1);
            }
            result = "[" + result + "]";
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // setparam
    public void setParam(String key, String val) {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT name FROM params WHERE name = ?");
            stmt.setString(1, key);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                PreparedStatement stmt2 = conn.prepareStatement("UPDATE params SET value = ? WHERE name = ?");
                stmt2.setString(1, val);
                stmt2.setString(2, key);
                stmt2.executeUpdate();
            } else {
                PreparedStatement stmt2 = conn.prepareStatement("INSERT INTO params (name, value) VALUES (?, ?)");
                stmt2.setString(1, key);
                stmt2.setString(2, val);
                stmt2.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // getparam
    public String getParam(String key) {
        String result = "";
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT value FROM params WHERE name = ?");
            stmt.setString(1, key);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                result = rs.getString("value");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // get secret key
    public String getSecretKey() {
        String result = null;
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT value FROM params WHERE name = 'secret_key'");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                result = rs.getString("value");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public void setSecretKey(String secretKey) {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT name FROM params WHERE name = 'secret_key'");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                PreparedStatement stmt2 = conn.prepareStatement("UPDATE params SET value = ? WHERE name = 'secret_key'");
                stmt2.setString(1, secretKey);
                stmt2.executeUpdate();
            } else {
                PreparedStatement stmt2 = conn
                        .prepareStatement("INSERT INTO params (name, value) VALUES ('secret_key', ?)");
                stmt2.setString(1, secretKey);
                stmt2.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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

    private String emptyIfNull(Object o) {
        if (o == null) {
            return "";
        } else {
            return o.toString();
        }
    }

}