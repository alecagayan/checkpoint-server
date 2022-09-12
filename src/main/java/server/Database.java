package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/***
 * Interface between API and database.
 */
public class Database {
    private static final String url = "jdbc:mysql://checkpointdb:3306/checkpoint";
    private static final String username = "checkin";
    private static final String password = "Chkpntuser!23";
    private static Connection conn = null;

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


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
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE login = ? AND role = 1 AND status = 1");
            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                // get password from db
                String storedPass = rs.getString("password");
                // check password
                if (passwordEncoder.matches(password, storedPass)) {
                    result = 0;
                }
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
        if (registrationCode.equalsIgnoreCase("FRC116")) {
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
    // 1 = already checked in
    // 2 = doesn't exist
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

            } else {
                result = 2;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // username, meetingId in
    // 0 = success
    // 1 = error
    public int checkOut(String username, String meetingId) {
        int result = 1;
        try {

            PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE attendees SET checkouttime = CURRENT_TIMESTAMP WHERE meeting_id = ? AND attendee_id = (SELECT id FROM users WHERE login = ?)");

            stmt.setString(1, meetingId);
            stmt.setString(2, username);
            stmt.executeUpdate();
            result = 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // name, email, username in
    // 0 = success
    // 1 = error
    public int addUser(String name, String email, String username, int role, int status, String password) {
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
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (name, email, login, role, status, password) VALUES (?, ?, ?, ?, ?, ?)");
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, username);
            stmt.setInt(4, role);
            stmt.setInt(5, status);
            stmt.setString(6, password);
            stmt.executeUpdate();
            result = 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public int updateUser(int id, String login, String name, String email, int role, int status) {
        int result = 1;
        try {
            PreparedStatement stmt = conn.prepareStatement("UPDATE users SET login = ?, name = ?, email = ?, role = ?, status = ? WHERE id = ?");
            stmt.setString(1, login);
            stmt.setString(2, name);
            stmt.setString(3, email);
            stmt.setInt(4, role);
            stmt.setInt(5, status);
            stmt.setInt(6, id);
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

    public String getUser(String userId) {
        String result = "";
        try {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT users.id, users.name, users.email, users.login, users.role, users.status FROM users WHERE users.id = ?");
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                result += "{\"id\":\"" + rs.getString("id") +
                        "\",\"name\":\"" + rs.getString("name") +
                        "\",\"email\":\"" + rs.getString("email") +
                        "\",\"login\":\"" + rs.getString("login") + 
                        "\",\"role\":\"" + rs.getInt("role") + 
                        "\",\"status\":\"" + rs.getInt("status") + "\"}";
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public String getAttendance(String userId) {
        String result = "";
        try {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT meetings.id, meetings.opentime, meetings.closetime, attendees.checkintime, attendees.checkouttime FROM meetings JOIN attendees ON meetings.id = attendees.meeting_id WHERE attendees.attendee_id = ?");
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                result += "{\"id\":\"" + rs.getString("id") +
                        "\",\"opentime\":\"" + rs.getString("opentime") +
                        "\",\"closetime\":\"" + rs.getString("closetime") +
                        "\",\"checkintime\":\"" + rs.getString("checkintime") +
                        "\",\"checkouttime\":\"" + rs.getString("checkouttime") + "\"},";
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
            // select all users from users table and select attendee count from attendees
            // table and join
            // calculate hours based on checkin and checkout times for each user            

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT users.id, users.name, users.email, users.login, COUNT(attendees.attendee_id) AS attendee_count, SUM(TIMESTAMPDIFF(MINUTE,checkintime,checkouttime))/60 AS total_hours FROM users LEFT JOIN attendees ON users.id = attendees.attendee_id WHERE attendees.meeting_id IN (SELECT id FROM meetings WHERE opentime >= ? AND opentime  <= ?) GROUP BY users.id order by users.name asc");
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
            }

            DecimalFormat df = new DecimalFormat();
            df.setMaximumFractionDigits(2);

            while (rs.next()) {
                result += "{\"id\":\"" + rs.getString("id") +
                        "\",\"name\":\"" + rs.getString("name") +
                        "\",\"email\":\"" + rs.getString("email") +
                        "\",\"login\":\"" + rs.getString("login") +
                        "\",\"attendee_count\":\"" + df.format(rs.getInt("attendee_count") / (countmeeting + 0.0) * 100) +
                        "%\",\"total_hours\":\"" + df.format(rs.getDouble("total_hours")) + "\"},";
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

    // return all raw data from attenees table in JSON format between given dates
    // append meeting checkin and checkout times to result
    // append user name and login to result
    public String getRawDataBetweenDates(String startDate, String endDate) {
        String result = "";
        try {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT attendees.id, attendees.meeting_id, attendees.attendee_id, attendees.checkintime, attendees.checkouttime, users.name, users.login FROM attendees JOIN users ON attendees.attendee_id = users.id WHERE attendees.meeting_id IN (SELECT id FROM meetings WHERE opentime >= ? AND opentime  <= ?)");
            stmt.setString(1, startDate);
            stmt.setString(2, endDate);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result += "{\"id\":\"" + rs.getString("id") +
                        "\",\"meeting_id\":\"" + rs.getString("meeting_id") +
                        "\",\"attendee_id\":\"" + rs.getString("attendee_id") +
                        "\",\"checkintime\":\"" + rs.getString("checkintime") +
                        "\",\"checkouttime\":\"" + rs.getString("checkouttime") +
                        "\",\"name\":\"" + rs.getString("name") +
                        "\",\"login\":\"" + rs.getString("login") + "\"},";
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

        // get attendee count for last few meetings
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
                    "SELECT m.id, m.opentime, (select name from users where id = m.openedby) as openedby, m.closetime, m.location, COUNT(a.id) as attendee_count FROM meetings m LEFT JOIN attendees a ON m.id = a.meeting_id GROUP BY m.id ORDER BY m.opentime DESC ");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result += "{\"id\":\"" + rs.getInt("id") +
                        "\",\"opentime\":\"" + emptyIfNull(rs.getTimestamp("opentime")) +
                        "\",\"openedby\":\"" + emptyIfNull(rs.getString("openedby")) +
                        "\",\"closetime\":\"" + emptyIfNull(rs.getTimestamp("closetime")) +
                        "\",\"attendee_count\":\"" + rs.getInt("attendee_count") +
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

    public String getMeeting(String meetingId) {
        String result = "";
        try {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT m.id, m.opentime, (select name from users where id = m.openedby) as openedby, m.closetime, m.location FROM meetings m WHERE m.id = ?");
            stmt.setString(1, meetingId);
            ResultSet rs = stmt.executeQuery();
            result = "{";
            if (rs.next()) {
                result += "\"id\":\"" + rs.getInt("id") +
                        "\",\"opentime\":\"" + emptyIfNull(rs.getTimestamp("opentime")) +
                        "\",\"openedby\":\"" + emptyIfNull(rs.getString("openedby")) +
                        "\",\"closetime\":\"" + emptyIfNull(rs.getTimestamp("closetime")) +
                        "\",\"location\":\"" + rs.getString("location") + "\"";
            }
            // get id of the next meeting
            stmt = conn.prepareStatement(
                    "SELECT id FROM meetings WHERE opentime > (SELECT opentime FROM meetings WHERE id = ?) ORDER BY opentime ASC LIMIT 1");
            stmt.setString(1, meetingId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                result += ",\"next_meeting_id\":\"" + rs.getInt("id") + "\"";
            }
            // get id of the previous meeting
            stmt = conn.prepareStatement(
                    "SELECT id FROM meetings WHERE opentime < (SELECT opentime FROM meetings WHERE id = ?) ORDER BY opentime DESC LIMIT 1");
            stmt.setString(1, meetingId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                result += ",\"prev_meeting_id\":\"" + rs.getInt("id") + "\"";
            }
            result += "}";

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
                PreparedStatement stmt2 = conn
                        .prepareStatement("UPDATE params SET value = ? WHERE name = 'secret_key'");
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

    // create meeting, returns the id of the new meeting or 0 if error
    public int createMeeting(String username) {
        int result = 0;
        try {
            // check if a meeting is already open
            PreparedStatement stmt = conn.prepareStatement("SELECT id FROM meetings WHERE closetime IS NULL");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return 0;
            }

            stmt = conn.prepareStatement(
                    "INSERT INTO meetings (opentime, openedby) VALUES (now(), (select id from users where login = ?))",
                    Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, username);
            stmt.executeUpdate();
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                result = rs.getInt(1);
            
                // check in the user
                stmt = conn.prepareStatement("INSERT INTO attendees (meeting_id, attendee_id) VALUES (?, (SELECT id FROM users WHERE login = ?))");
                stmt.setInt(1, result);
                stmt.setString(2, username);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // check if meeting is closed
    public boolean isMeetingClosed(String meetingId) {
        boolean result = false;
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT closetime FROM meetings WHERE id = ?");
            stmt.setString(1, meetingId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                result = rs.getTimestamp("closetime") != null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // close meeting
    // 0 = success
    // 1 = error
    public int closeMeeting(String meetingId) {
        int result = 1;
        try {

            PreparedStatement stmt = conn.prepareStatement("UPDATE attendees SET checkouttime = now() WHERE meeting_id = ? AND checkouttime IS NULL");
            stmt.setString(1, meetingId);
            stmt.executeUpdate();

            PreparedStatement stmt2 = conn.prepareStatement("UPDATE meetings SET closetime = now() WHERE id = ?");
            stmt2.setString(1, meetingId);
            stmt2.executeUpdate();
            result = 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    private String emptyIfNull(String s) {
        if (s == null) {
            return "";
        } else {
            return s.toString();
        }
    }

    private String emptyIfNull(Timestamp ts) {
        if (ts == null) {
            return "";
        } else {
            return dateFormat.format(ts);
        }
    }


}
