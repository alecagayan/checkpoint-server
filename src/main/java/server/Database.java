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
    public int register(String id, String name, String email, String registrationCode, Token userToken) {
        int result = 1;
        System.out.println("registration code: " + registrationCode);
        String orgId = userToken.getOrgId();

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
                    stmt = conn.prepareStatement("INSERT INTO users (login, name, email, org) VALUES (?, ?, ?, ?)");
                    stmt.setString(1, id);
                    stmt.setString(2, name);
                    stmt.setString(3, email);
                    stmt.setString(4, orgId);
                    stmt.executeUpdate();
                    result = 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public int registerOrg(String orgName, String orgId, String name, String email, String username, String password) {
        //create new organization with name orgName and short orgId
        int result = 1;
        int orgIdInt = 0;
        int ownerId = 0;

        try {
            PreparedStatement userReg = conn.prepareStatement("INSERT INTO users (login, password, name, email, role) VALUES (?, ?, ?, ?, 1)");
            userReg.setString(1, username);
            userReg.setString(2, passwordEncoder.encode(password));
            userReg.setString(3, name);
            userReg.setString(4, email);
            userReg.executeUpdate();

            // get users id
            PreparedStatement stmt = conn.prepareStatement("SELECT id FROM users WHERE login = ?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                ownerId = rs.getInt("id");
            }

            // add organization
            PreparedStatement orgReg = conn.prepareStatement("INSERT INTO organizations (name, short, owner) VALUES (?, ?, ?)");
            orgReg.setString(1, orgName);
            orgReg.setString(2, orgId);
            orgReg.setInt(3, ownerId);
            orgReg.executeUpdate();

            // get org id
            stmt = conn.prepareStatement("SELECT id FROM organizations WHERE short = ?");
            stmt.setString(1, orgId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                orgIdInt = rs.getInt("id");
            }
            
            //update user to assign to org
            PreparedStatement userUpdate = conn.prepareStatement("UPDATE users SET org = ? WHERE id = ?");
            userUpdate.setInt(1, orgIdInt);
            userUpdate.setInt(2, ownerId);
            userUpdate.executeUpdate();

            result = 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return result;
        }
        
        return result;

    }


    // username, meetingId in
    // 0 = success
    // 1 = already checked in
    // 2 = doesn't exist
    public int checkIn(String username, String meetingId, Token userToken) {
        int result = 1;
        try {

            String orgId = userToken.getOrgId();

            // get attendee_id from users table
            PreparedStatement stmt = conn.prepareStatement("SELECT id FROM users WHERE login = ? AND org = ?");
            stmt.setString(1, username);
            stmt.setString(2, orgId);   
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String attendeeId = rs.getString("id");
                stmt = conn.prepareStatement("INSERT INTO attendees (meeting_id, attendee_id, org) VALUES (?, ?, ?)");
                stmt.setString(1, meetingId);
                stmt.setString(2, attendeeId);
                stmt.setString(3, orgId);
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
        // encrypt password
        String encryptedPassword = passwordEncoder.encode(password);
        try {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (name, email, login, role, status, password) VALUES (?, ?, ?, ?, ?, ?)");
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, username);
            stmt.setInt(4, role);
            stmt.setInt(5, status);
            stmt.setString(6, encryptedPassword);
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
    public String getUsers(String orgId) {
        String result = "";
        try {
            // select all users from users table and select attendee count from attendees
            // table and join
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT users.id, users.name, users.email, users.login, COUNT(attendees.attendee_id) AS attendee_count FROM users LEFT JOIN attendees ON users.id = attendees.attendee_id WHERE users.org = ? GROUP BY users.id");
            stmt.setString(1, orgId);
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

    public String getUserByLogin(String login) {
        String result = "";
        System.out.println("login: " + login);
        try {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT users.id, users.name, users.email, users.login, users.role, users.status FROM users WHERE users.login = ?");
            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                result += "{\"id\":\"" + rs.getString("id") +
                        "\",\"login\":\"" + rs.getString("login") + "\"}";
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("result: " + result);
        return result;
    }

    public String getAttendance(String userId, String startDate, String endDate) {
        String result = "";
        PreparedStatement stmt = null;
        try {
            // if startdate and enddate are blank, then select all
            if (startDate == null || startDate.trim().length() == 0) {
                stmt = conn.prepareStatement(
                    "SELECT meetings.id, meetings.opentime, meetings.closetime, attendees.checkintime, attendees.checkouttime FROM meetings JOIN attendees ON meetings.id = attendees.meeting_id WHERE attendees.attendee_id = ? ORDER BY meetings.opentime DESC");
                    stmt.setString(1, userId);
            } else {
                stmt = conn.prepareStatement(
                    "SELECT meetings.id, meetings.opentime, meetings.closetime, attendees.checkintime, attendees.checkouttime FROM meetings JOIN attendees ON meetings.id = attendees.meeting_id WHERE attendees.attendee_id = ? AND meetings.opentime >= ? AND meetings.closetime <= ? ORDER BY meetings.opentime DESC");
                    stmt.setString(1, userId);
                    stmt.setString(2, startDate);
                    stmt.setString(3, endDate);
                }

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

    //percentage of total hours attended by user between start and end date
    public String getPercentages(String userId, String startDate, String endDate) {
        String result = "";


        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(2);

        try {
            //get total amount of meeting hours between given dates
            PreparedStatement meeting_hours = conn.prepareStatement("SELECT SUM(TIMESTAMPDIFF(MINUTE,opentime,closetime))/60 AS total_meeting_hours FROM meetings WHERE opentime >= ? AND opentime  <= ? AND type = 0");
            meeting_hours.setString(1, startDate);
            meeting_hours.setString(2, endDate);
            ResultSet hours_rs = meeting_hours.executeQuery();

            //get total amount of hours attended by user between given dates
            PreparedStatement attended_hours = conn.prepareStatement("SELECT SUM(TIMESTAMPDIFF(MINUTE,checkintime,checkouttime))/60 AS total_attended_hours FROM meetings JOIN attendees ON meetings.id = attendees.meeting_id WHERE attendees.attendee_id = ? AND meetings.opentime >= ? AND meetings.opentime <= ?");
            attended_hours.setString(1, userId);
            attended_hours.setString(2, startDate);
            attended_hours.setString(3, endDate);
            ResultSet attended_rs = attended_hours.executeQuery();

            //calculate percentage
            if (hours_rs.next() && attended_rs.next()) {
                double total_meeting_hours = hours_rs.getDouble("total_meeting_hours");
                double total_attended_hours = attended_rs.getDouble("total_attended_hours");
                double percentage = total_attended_hours / total_meeting_hours * 100;
                result = String.valueOf(df.format(percentage));
            }

            System.out.println("total meeting hours: " + hours_rs.getDouble("total_meeting_hours"));

            // if (result.length() > 0) {
            //     result = result.substring(0, result.length() - 1);
            // }
            // result = "[" + result + "]";

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    // return all users and meeting percentage between given dates in JSON format
    public String getUsersBetweenDates(String orgId, String startDate, String endDate) {
        String result = "";
        try {
            // select all users from users table and select attendee count from attendees
            // table and join
            // calculate hours based on checkin and checkout times for each user   
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT users.id, users.name, users.email, users.login, COUNT(attendees.attendee_id) AS attendee_count, SUM(TIMESTAMPDIFF(MINUTE,checkintime,checkouttime))/60 AS total_hours FROM users LEFT JOIN attendees ON users.id = attendees.attendee_id WHERE attendees.meeting_id IN (SELECT id FROM meetings WHERE opentime >= ? AND opentime <= ? AND org = ?) GROUP BY users.id order by users.name asc");
            stmt.setString(1, startDate);
            stmt.setString(2, endDate);
            stmt.setString(3, orgId);
            ResultSet rs = stmt.executeQuery();

            //get total amount of meeting hours between given dates
            PreparedStatement meeting_hours = conn.prepareStatement(
                    "SELECT SUM(TIMESTAMPDIFF(MINUTE,opentime,closetime))/60 AS total_meeting_hours FROM meetings WHERE opentime >= ? AND opentime  <= ? AND org = ? AND type = 0");
            meeting_hours.setString(1, startDate);
            meeting_hours.setString(2, endDate);
            meeting_hours.setString(3, orgId);
            ResultSet hours_rs = meeting_hours.executeQuery();

            PreparedStatement meeting_count = conn.prepareStatement(
                    "SELECT COUNT(id) AS meeting_count FROM meetings WHERE opentime >= ? AND opentime <= ? AND org = ?");
            meeting_count.setString(1, startDate);
            meeting_count.setString(2, endDate);
            meeting_count.setString(3, orgId);
            ResultSet meeting_rs = meeting_count.executeQuery();

            int countmeeting = 1;
            while (meeting_rs.next()) {
                countmeeting = meeting_rs.getInt("meeting_count");
            }

            double total_meeting_hours = 0;
            while (hours_rs.next()) {
                total_meeting_hours = hours_rs.getDouble("total_meeting_hours");
            }

            DecimalFormat df = new DecimalFormat();
            df.setMaximumFractionDigits(2);

            while (rs.next()) {
                result += "{\"id\":\"" + rs.getString("id") +
                        "\",\"name\":\"" + rs.getString("name") +
                        "\",\"email\":\"" + rs.getString("email") +
                        "\",\"login\":\"" + rs.getString("login") +
                        "\",\"attendee_count\":\"" + df.format(rs.getInt("attendee_count") / (countmeeting + 0.0) * 100) +
                        "\",\"hour_percentage\":\"" + df.format(rs.getDouble("total_hours") / (total_meeting_hours + 0.0) * 100) +
                        "\",\"total_hours\":\"" + df.format(rs.getDouble("total_hours")) + "\"},";
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
    public String getRawDataBetweenDates(String orgId, String startDate, String endDate) {
        String result = "";
        try {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT attendees.id, attendees.meeting_id, attendees.attendee_id, attendees.checkintime, attendees.checkouttime, users.name, users.login FROM attendees JOIN users ON attendees.attendee_id = users.id WHERE attendees.meeting_id IN (SELECT id FROM meetings WHERE opentime >= ? AND opentime  <= ? and org = ?) ORDER BY attendees.meeting_id");
            stmt.setString(1, startDate);
            stmt.setString(2, endDate);
            stmt.setString(3, orgId);
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

    public String getRecentMeetings(int limit, Token userToken) {
        String result = "";
        String orgId = userToken.getOrgId();        
        try {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT m.opentime as meeting_time, COUNT(a.attendee_id) as attendee_count FROM attendees a, meetings m WHERE m.id = a.meeting_id AND m.org = ? group by m.opentime order by m.opentime desc limit ?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            stmt.setString(1, orgId);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            //read rs backwards
            rs.afterLast();
            while (rs.previous()) {
                result += "{\"meeting_time\":\"" + rs.getString("meeting_time") +
                        "\",\"attendee_count\":\"" + rs.getString("attendee_count") + "\"},";
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

    // return all meetings with orgId in JSON format
    public String getMeetings(String orgId) {
        String result = "";
        System.out.println("getting meetings with orgid: " + orgId);
        try {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT m.id, m.opentime, (select name from users where id = m.openedby) as openedby, m.closetime, m.location, m.type, COUNT(a.id) as attendee_count FROM meetings m LEFT JOIN attendees a ON m.id = a.meeting_id WHERE m.org = ? GROUP BY m.id ORDER BY m.opentime DESC ");
            stmt.setString(1, orgId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String type = "";
                if (rs.getInt("type") == 0) {
                    type = "Required";
                } else if (rs.getInt("type") == 1) {
                    type = "Optional";
                }
                result += "{\"id\":\"" + rs.getInt("id") +
                        "\",\"opentime\":\"" + emptyIfNull(rs.getTimestamp("opentime")) +
                        "\",\"openedby\":\"" + emptyIfNull(rs.getString("openedby")) +
                        "\",\"closetime\":\"" + emptyIfNull(rs.getTimestamp("closetime")) +
                        "\",\"attendee_count\":\"" + rs.getInt("attendee_count") +
                        "\",\"type\":\"" + type +
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
                    "SELECT m.id, m.opentime, (select name from users where id = m.openedby) as openedby, m.closetime, m.type, m.location FROM meetings m WHERE m.id = ?");
            stmt.setString(1, meetingId);
            ResultSet rs = stmt.executeQuery();
            result = "{";
            if (rs.next()) {
                result += "\"id\":\"" + rs.getInt("id") +
                        "\",\"opentime\":\"" + emptyIfNull(rs.getTimestamp("opentime")) +
                        "\",\"openedby\":\"" + emptyIfNull(rs.getString("openedby")) +
                        "\",\"closetime\":\"" + emptyIfNull(rs.getTimestamp("closetime")) +
                        "\",\"type\":\"" + rs.getInt("type") +
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
    public int createMeeting(String username, Token userToken) {
        int result = 0;
        try {
            // check if a meeting is already open
            PreparedStatement stmt = conn.prepareStatement("SELECT id FROM meetings WHERE closetime IS NULL");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return 0;
            }

            stmt = conn.prepareStatement(
                    "INSERT INTO meetings (opentime, openedby, org) VALUES (now(), (select id from users where login = ?), ?)",
                    Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, username);
            stmt.setString(2, userToken.getOrgId());
            stmt.executeUpdate();
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                result = rs.getInt(1);
            
                // check in the user
                stmt = conn.prepareStatement("INSERT INTO attendees (meeting_id, attendee_id, org, type) VALUES (?, (SELECT id FROM users WHERE login = ?), ?, 0)");
                stmt.setInt(1, result);
                stmt.setString(2, username);
                stmt.setString(3, userToken.getOrgId());
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

    public int changeMeetingType(String meetingId) {
        //get meeting type
        int result = 1;
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT type FROM meetings WHERE id = ?");
            stmt.setString(1, meetingId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String type = rs.getString("type");
                if (type.equals("1")) {
                    type = "0";
                } else {
                    type = "1";
                }
                PreparedStatement stmt2 = conn.prepareStatement("UPDATE meetings SET type = ? WHERE id = ?");
                stmt2.setString(1, type);
                stmt2.setString(2, meetingId);
                stmt2.executeUpdate();
                result = 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public String getOrgIdFromUsername(String username) {
        String result = null;
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT org FROM users WHERE login = ?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                result = rs.getString("org");
            }
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
