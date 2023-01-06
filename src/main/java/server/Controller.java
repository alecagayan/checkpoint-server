package server;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.HttpMediaTypeNotAcceptableException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Date;

import org.json.*;

@RestController
@RequestMapping("/rbapi")
@CrossOrigin(origins = "*")
public class Controller {

    public static final int TOKEN_EXPIRY_SECONDS = 60 * 60 * 6;

    public static final String X_AUTH_TOKEN = "X-Auth-Token";

    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_USER = "user";

    @Autowired
    private JavaMailSender emailSender;

    public void sendMessage(String to, String from, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage(); 
        message.setFrom(from);
        message.setTo(to); 
        message.setSubject(subject); 
        message.setText(text);
        emailSender.send(message);
    }     

    @GetMapping("/hello")
    public String hello() {
        return "Hello World!";
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String login(@RequestBody String json) {
        JSONObject jsonObject = new JSONObject(json);
        String login = jsonObject.getString("username");
        String password = jsonObject.getString("password");
        String result = "";
        Database db = new Database();
        if (db.logIn(login, password) == 0) {
            String tokenValue = "";
            int role = db.getRoleByLogin(login);
            try {                
                tokenValue = generateTokenForUser(login, role == 1 ? ROLE_ADMIN : ROLE_USER );
            } catch (Exception e) {
                e.printStackTrace();
                result = "{\"error\":\"cannot generate token\"}";
                return result;
            }
            result = "{\"token\":\"" + tokenValue + "\", \"role\":\"" + (role == 1 ? ROLE_ADMIN : ROLE_USER) + "\"}";
        } else {
            result = "{\"error\":\"invalid credentials\"}";
        }
        return result;
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String register(@RequestHeader(X_AUTH_TOKEN) String token, @RequestBody String json) {
        String result = "";

        JSONObject jsonObject = new JSONObject(json);
        String id = jsonObject.getString("id");
        String name = jsonObject.getString("name");
        String email = jsonObject.getString("email");

        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired() || !ROLE_ADMIN.equals(userToken.getRole())) {
            result = "{\"error\":\"invalid token\"}";
            return result;
        }

        Database db = new Database();
        if (db.register(id, name, email, userToken) == 0) {
            result = "{\"result\":\"" + "1" + "\"}";
        } else {
            result = "{\"error\":\"badness occurred\"}";
        }
        return result;
    }

    //not authenticated
    @PostMapping(value = "/registerorg", consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String registerOrg(@RequestBody String json) {
        JSONObject jsonObject = new JSONObject(json);
        String orgName = jsonObject.getString("orgName");
        String name = jsonObject.getString("name");
        String email = jsonObject.getString("email");
        String username = jsonObject.getString("username");
        String captchaToken = jsonObject.getString("captchaToken");

        Database db = new Database();


        // check if captcha is valid with get request
        String url = String.format("https://www.google.com/recaptcha/api/siteverify?secret=%s&response=%s",
            URLEncoder.encode(db.getCaptchaSecret(), StandardCharsets.UTF_8),
            URLEncoder.encode(captchaToken, StandardCharsets.UTF_8));

        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(url, String.class);
        JSONObject captchaResponse = new JSONObject(response);
        if (!captchaResponse.getBoolean("success")) {
            return "{\"error\":\"invalid captcha\"}";
        }

        String result = "";
        if (db.registerOrg(orgName, name, email, username, "") == 0) {

            // send password creation email
            try {
                String passwordToken = generatePasswordResetTokenForUser(email);

                // get url from config
                String encodedToken = URLEncoder.encode(passwordToken, StandardCharsets.UTF_8);
                String resetUrl = Config.getProperty("base.url") + "/resetpassword/" + encodedToken;

                // send email
                String subject = "Checkpoint Password Reset";
                String body = "Welcome to your new organization! Please click the following link to create your password: " + resetUrl + ". This link will expire in 30 minutes.";
                String from = Config.getProperty("mail.from");
                sendMessage(email, from, subject, body);
                result = "{\"result\":\"" + "1" + "\"}";

            }
            catch (Exception e) {
                result = "{\"error\":\"badness occurred\"}";
            }

        } else {
            result = "{\"error\":\"badness occurred\"}";
        }
        return result;
    }

    @PostMapping(value = "/adduser", consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String adduser(@RequestHeader(X_AUTH_TOKEN) String token, @RequestBody String json) {
        JSONObject jsonObject = new JSONObject(json);
        String name = jsonObject.getString("name");
        String email = jsonObject.getString("email");
        String username = jsonObject.getString("username");
        int role = jsonObject.getInt("role");

        String result = "";

        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired() || !ROLE_ADMIN.equals(userToken.getRole())) {
            result = "{\"error\":\"invalid token\"}";
            return result;
        }

        Database db = new Database();
        if (db.addUser(name, email, username, role, 1, "", userToken) == 0) {
            result = "{\"user\":\"" + username + "\"}";

            //send user password creation email
            try {
                String passwordToken = generatePasswordResetTokenForUser(email);

                // get url from config
                String encodedToken = URLEncoder.encode(passwordToken, StandardCharsets.UTF_8);
                String resetUrl = Config.getProperty("base.url") + "/resetpassword/" + encodedToken;

                // send email
                String subject = "Checkpoint Password Reset";
                String body = "Please click the following link to create your password: " + resetUrl + ". This link will expire in 30 minutes.";
                String from = Config.getProperty("mail.from");
                sendMessage(email, from, subject, body);
            }
            catch (Exception e) {
                result = "{\"error\":\"badness occurred\"}";
            }
        } else {
            result = "{\"error\":\"badness occurred\"}";
        }
        return result;
    }

    @PostMapping(value = "/updateuser", consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String updateuser(@RequestHeader(X_AUTH_TOKEN) String token, @RequestBody String json) {
        JSONObject jsonObject = new JSONObject(json);
        System.out.println(jsonObject.toString());
        String name = jsonObject.getString("name");
        int id = jsonObject.getInt("id");
        String login = jsonObject.getString("login");
        String email = jsonObject.getString("email");
        int role = jsonObject.getInt("role");
        int status = jsonObject.getInt("status");
        String result = "";

        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired() || (!ROLE_ADMIN.equals(userToken.getRole()) && userToken.getUsername() != login)) {
            result = "{\"error\":\"invalid token\"}";
            return result;
        }
        Database db = new Database();
        if (db.updateUser(id, login, name, email, role, status) == 0) {
            result = "{\"user\":\"" + id + "\"}";
        } else {
            result = "{\"error\":\"badness occurred\"}";
        }
        return result;
    }

    @PostMapping(value = "/checkin", consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String checkin(@RequestHeader(X_AUTH_TOKEN) String token, @RequestBody String json) {
        JSONObject jsonObject = new JSONObject(json);
        System.out.println("jsonObject: " + jsonObject); 
        String username = jsonObject.getString("login");
        String meetingId = jsonObject.getString("meetingId");
        String result = "";

        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired() || !ROLE_ADMIN.equals(userToken.getRole())) {
            result = "{\"error\":\"invalid token\"}";
            return result;
        }

        System.out.println("checking in user " + username + " with token " + token + " for meeting " + meetingId);
        Database db = new Database();

        // logic to check if meeting is closed
        if (db.isMeetingClosed(meetingId)) {
            result = "{\"error\":\"meeting is closed\"}";
            return result;
        }

        int returnCode = db.checkIn(username, meetingId, userToken);
        result = "{\"result\":\"" + returnCode + "\"}";

        return result;
    }

    @PostMapping(value = "/checkout", consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String checkout(@RequestHeader(X_AUTH_TOKEN) String token, @RequestBody String json) {
        JSONObject jsonObject = new JSONObject(json);
        String username = jsonObject.getString("login");
        String meetingId = jsonObject.getString("meetingId");
        String result = "";

        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired() || !ROLE_ADMIN.equals(userToken.getRole())) {
            result = "{\"error\":\"invalid token\"}";
            return result;
        }
        System.out.println("checking out user " + username + " with token " + token + " for meeting " + meetingId);
        Database db = new Database();
        if (db.checkOut(username, meetingId) == 0) {
            result = "{\"result\":\"" + "1" + "\"}";
        } else {
            result = "{\"error\":\"badness occurred\"}";
        }
        return result;
    }

    //not authenticated
    @PostMapping(value = "/forgotpassword", consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String forgotPassword(@RequestBody String json) {
        JSONObject jsonObject = new JSONObject(json);
        String email = jsonObject.getString("email");
        String result = "";

        Database db = new Database();
        String jsonUser = db.getUserByEmail(email);
        if (jsonUser != null && !jsonUser.isEmpty()) {

            try {
                String token = generatePasswordResetTokenForUser(email);

                // get url from config
                String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
                String resetUrl = Config.getProperty("base.url") + "/resetpassword/" + encodedToken;

                // send email
                String subject = "Checkpoint Password Reset";
                String body = "Please click the following link to reset your password: " + resetUrl + ". This link will expire in 30 minutes.";
                String from = Config.getProperty("mail.from");
                sendMessage(email, from, subject, body);
            
                result = "{\"result\":\"" + "1" + "\"}";
            }
            catch (Exception e) {
                result = "{\"error\":\"badness occurred\"}";
            }
        }
        return result;
    }

    //not authenticated
    @PostMapping(value = "/resetpassword", consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String resetPassword(@RequestBody String json) {
        JSONObject jsonObject = new JSONObject(json);
        String token = jsonObject.getString("token");
        String password = jsonObject.getString("password");
        String repeatPassword = jsonObject.getString("repeatPassword");
        String result = "";

        Database db = new Database();
        PasswordResetToken passwordResetToken = getPasswordResetToken(token);
        if (passwordResetToken == null || passwordResetToken.isExpired()) {
            result = "{\"error\":\"invalid token\"}";
            return result;
        }

        String email = passwordResetToken.getEmail();
        if (email != null && !email.isEmpty()) {
            if (password.equals(repeatPassword) && db.updatePasswordByEmail(email, password) == 0) {
                result = "{\"result\":\"" + "1" + "\"}";
            } else {
                result = "{\"error\":\"badness occurred\"}";
            }
        }
        return result;
    }


    @GetMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String users(@RequestHeader(X_AUTH_TOKEN) String token) {

        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired() || !ROLE_ADMIN.equals(userToken.getRole())) {
            return "{\"error\":\"invalid token\"}";
        }

        Database db = new Database();
        String result = db.getUsers(userToken.getOrgId());
        return result;
    }

    @GetMapping(value = "/user", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String user(@RequestHeader(X_AUTH_TOKEN) String token, @RequestParam String userId) {
        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired() || !ROLE_ADMIN.equals(userToken.getRole())) {
            return "{\"error\":\"invalid token\"}";
        }
        Database db = new Database();
        String result = db.getUser(userId);
        return result;
    }

    @GetMapping(value = "/userbylogin", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String userbylogin(@RequestHeader(X_AUTH_TOKEN) String token, @RequestParam String login) {
        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired() || (!ROLE_ADMIN.equals(userToken.getRole()) && !login.equals(userToken.getUsername()))) {
            return "{\"error\":\"invalid token\"}";
        }

        Database db = new Database();
        String result = db.getUserByLogin(login);
        return result;
    }

    @GetMapping(value = "/userbytoken", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String userbytoken(@RequestHeader(X_AUTH_TOKEN) String token) {
        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired()) {
            return "{\"error\":\"invalid token\"}";
        }
        Database db = new Database();
        String result = db.getUserByLogin(userToken.getUsername());

        return result;
    }

    //not authenticated
    @GetMapping(value = "/attendance", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String attendance(@RequestParam String userId, @RequestParam(value = "startDate", defaultValue = "") String startDate,
    @RequestParam(value = "endDate", defaultValue = "") String endDate) {
        Database db = new Database();
        String result = db.getAttendance(userId, startDate, endDate);
        return result;
    }

    @GetMapping(value = "/attendancebytoken", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String attendanceByToken(@RequestHeader(X_AUTH_TOKEN) String token, 
            @RequestParam(value = "startDate", defaultValue = "") String startDate,
            @RequestParam(value = "endDate", defaultValue = "") String endDate) {

        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired()) {
            return "{\"error\":\"invalid token\"}";
        }

        Database db = new Database();
        String getId = db.getUserByLogin(userToken.getUsername());
        JSONObject jsonObject = new JSONObject(getId);
        String userId = jsonObject.getString("id");
        String result = db.getAttendance(userId, startDate, endDate);
        return result;
    }

    @GetMapping(value = "/attendancebytokenbetweendates", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String attendanceByTokenBetweenDates(@RequestHeader(X_AUTH_TOKEN) String token, 
            @RequestParam(value = "startDate", defaultValue = "") String startDate,
            @RequestParam(value = "endDate", defaultValue = "") String endDate) {

        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired()) {
            return "{\"error\":\"invalid token\"}";
        }

        Database db = new Database();
        String getId = db.getUserByLogin(userToken.getUsername());
        JSONObject jsonObject = new JSONObject(getId);
        String userId = jsonObject.getString("id");
        String result = db.getAttendance(userId, startDate, endDate);
        return result;
    }

    //not authenticated
    @GetMapping(value = "/percentages", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String percentages(@RequestParam String userId, @RequestParam(value = "startDate", defaultValue = "") String startDate,
    @RequestParam(value = "endDate", defaultValue = "") String endDate) {
        Database db = new Database();
        String result = db.getPercentages(userId, startDate, endDate);
        return result;
    }

    @GetMapping(value = "/percentagesbytokenbetweendates", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String percentagesByTokenBetweenDates(@RequestHeader(X_AUTH_TOKEN) String token, 
            @RequestParam(value = "startDate", defaultValue = "") String startDate,
            @RequestParam(value = "endDate", defaultValue = "") String endDate) {

        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired()) {
            return "{\"error\":\"invalid token\"}";
        }

        Database db = new Database();
        String getId = db.getUserByLogin(userToken.getUsername());
        JSONObject jsonObject = new JSONObject(getId);
        String userId = jsonObject.getString("id");
        String result = db.getPercentages(userId, startDate, endDate);
        return result;
    }

    @GetMapping(value = "/reportbydate", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String reportByDate(@RequestHeader(X_AUTH_TOKEN) String token, 
            @RequestParam(value = "startDate", defaultValue = "") String startDate,
            @RequestParam(value = "endDate", defaultValue = "") String endDate) {

        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired() || !ROLE_ADMIN.equals(userToken.getRole())) {
            return "{\"error\":\"invalid token\"}";
        }

        Database db = new Database();
        String result = db.getUsersBetweenDates(userToken.getOrgId(), startDate, endDate);
        return result;
    }

    @GetMapping(value = "/rawdatabydate", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String rawDataByDate(@RequestHeader(X_AUTH_TOKEN) String token, 
            @RequestParam(value = "startDate", defaultValue = "") String startDate,
            @RequestParam(value = "endDate", defaultValue = "") String endDate) {

        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired() || !ROLE_ADMIN.equals(userToken.getRole())) {
            return "{\"error\":\"invalid token\"}";
        }

        Database db = new Database();
        String result = db.getRawDataBetweenDates(userToken.getOrgId(), startDate, endDate);
        return result;
    }

    @GetMapping(value = "/recentmeetings", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String recentMeetings(@RequestHeader(X_AUTH_TOKEN) String token, @RequestParam String limit) {

        // check if token is valid
        Token userToken = getToken(token);
        System.out.println("role: " + userToken.getRole());
        if (userToken == null || userToken.isExpired() || !ROLE_ADMIN.equals(userToken.getRole())) {
            return "{\"error\":\"invalid token\"}";
        }

        Database db = new Database();
        int size = 10;
        if (limit != null) {
            size = Integer.parseInt(limit);
        }
        String result = db.getRecentMeetings(size, userToken);
        return result;
    }

    @GetMapping(value = "/topattendees", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String topAttendees(@RequestHeader(X_AUTH_TOKEN) String token, 
            @RequestParam(value = "startDate", defaultValue = "") String startDate,
            @RequestParam(value = "endDate", defaultValue = "") String endDate, 
            @RequestParam(value = "limit") String limit,
            @RequestParam(value = "type") String type) {

        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired() || !ROLE_ADMIN.equals(userToken.getRole())) {
            return "{\"error\":\"invalid token\"}";
        }
        Database db = new Database();
        int size = 5;
        if (limit != null) {
            size = Integer.parseInt(limit);
        }
        String result = db.getUsersBetweenDates(userToken.getOrgId(), startDate, endDate, size, type);
        return result;
    }

    @GetMapping(value = "/attendees", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String attendees(@RequestHeader(X_AUTH_TOKEN) String token, @RequestParam(value = "meetingId") String meetingId) {
        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired() || !ROLE_ADMIN.equals(userToken.getRole())) {
            return "{\"error\":\"invalid token\"}";
        }
        Database db = new Database();
        String result = db.getAttendees(meetingId);
        return result;
    }

    @GetMapping(value = "/meeting", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String meeting(@RequestHeader(X_AUTH_TOKEN) String token, @RequestParam(value = "meetingId") String meetingId) {
        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired() || !ROLE_ADMIN.equals(userToken.getRole())) {
            return "{\"error\":\"invalid token\"}";
        }
        Database db = new Database();
        String result = db.getMeeting(meetingId);
        System.out.println("result: " + result);
        return result;
    }

    @GetMapping(value = "/meetingtype", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String meetingType(@RequestHeader(X_AUTH_TOKEN) String token, @RequestParam(value = "meetingTypeId") String meetingTypeId) {
        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired() || !ROLE_ADMIN.equals(userToken.getRole())) {
            return "{\"error\":\"invalid token\"}";
        }
        Database db = new Database();
        String result = db.getMeetingType(meetingTypeId);
        System.out.println("result: " + result);
        return result;
    }

    @GetMapping(value = "/meetings", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String meetings(@RequestHeader(X_AUTH_TOKEN) String token) {
        System.out.println("token: " + token);
        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired() || !ROLE_ADMIN.equals(userToken.getRole())) {
            return "{\"error\":\"invalid token\"}";
        }
        Database db = new Database();
        String result = db.getMeetings(userToken.getOrgId());
        return result;
    }

    @GetMapping(value = "/meetingtypes", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String meetingTypes(@RequestHeader(X_AUTH_TOKEN) String token) {
        System.out.println("token: " + token);
        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired() || !ROLE_ADMIN.equals(userToken.getRole())) {
            return "{\"error\":\"invalid token\"}";
        }
        Database db = new Database();
        String result = db.getMeetingTypes(userToken.getOrgId());
        return result;
    }

    @GetMapping(value = "/meetingswithtype", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String meetingsWithType(@RequestHeader(X_AUTH_TOKEN) String token, @RequestParam(value = "meetingTypeId") String meetingTypeId) {
        System.out.println("token: " + token);
        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired() || !ROLE_ADMIN.equals(userToken.getRole())) {
            return "{\"error\":\"invalid token\"}";
        }
        Database db = new Database();
        String result = db.getMeetingsWithType(userToken.getOrgId() ,meetingTypeId);
        return result;
    }
    

    @PostMapping(value = "/startmeeting", consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String startmeeting(@RequestHeader(X_AUTH_TOKEN) String token) {
        String result = "";

        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired() || !ROLE_ADMIN.equals(userToken.getRole())) {
            result = "{\"error\":\"invalid token\"}";
            return result;
        }

        Database db = new Database();
        int meetingId = db.createMeeting(userToken.getUsername(), userToken);
        if (meetingId != 0) {
            result = "{\"meeting\":\"" + meetingId + "\"}";
        } else {
            result = "{\"error\":\"badness occurred\"}";
        }
        return result;
    }
    

    @PostMapping(value = "/closemeeting", consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String closemeeting(@RequestHeader(X_AUTH_TOKEN) String token, @RequestBody String json) {
        JSONObject jsonObject = new JSONObject(json);
        System.out.println("jsonObject: " + jsonObject); 
        String meetingId = jsonObject.getString("meetingId");
        String result = "";

        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired() || !ROLE_ADMIN.equals(userToken.getRole())) {
            result = "{\"error\":\"invalid token\"}";
            return result;
        }

        Database db = new Database();
        if (db.closeMeeting(meetingId) == 0) {
            result = "{\"meeting\":\"" + meetingId + "\"}";
        } else {
            result = "{\"error\":\"badness occurred\"}";
        }
        return result;
    }

    @PostMapping(value = "/addmeetingtype", consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String addMeetingType(@RequestHeader(X_AUTH_TOKEN) String token, @RequestBody String json) {
        JSONObject jsonObject = new JSONObject(json);
        String name = jsonObject.getString("name");
        String multiplier = jsonObject.getString("multiplier");

        String result = "";

        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired() || !ROLE_ADMIN.equals(userToken.getRole())) {
            result = "{\"error\":\"invalid token\"}";
            return result;
        }

        Database db = new Database();
        if (db.addMeetingType(name, multiplier, userToken) == 0) {
            result = "{\"meeting type\":\"" + name + "\"}";
        } else {
            result = "{\"error\":\"badness occurred\"}";
        }
        return result;
    }

    @PostMapping(value = "/updatemeetingtype", consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String updateMeetingType(@RequestHeader(X_AUTH_TOKEN) String token, @RequestBody String json) {
        JSONObject jsonObject = new JSONObject(json);
        System.out.println("jsonObject: " + jsonObject); 
        String meetingTypeId = jsonObject.getString("id");
        String name = jsonObject.getString("name");
        String multiplier = jsonObject.getString("multiplier");
        String result = "";

        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired() || !ROLE_ADMIN.equals(userToken.getRole())) {
            result = "{\"error\":\"invalid token\"}";
            return result;
        }

        Database db = new Database();
        if (db.updateMeetingType(meetingTypeId, name, multiplier) == 0) {
            result = "{\"meeting\":\"" + meetingTypeId + "\"}";
        } else {
            result = "{\"error\":\"badness occurred\"}";
        }
        return result;
    }

    @PostMapping(value = "/changemeetingtype", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String changeMeetingType(@RequestHeader(X_AUTH_TOKEN) String token, @RequestBody String json) {
        JSONObject jsonObject = new JSONObject(json);
        System.out.println("jsonObject: " + jsonObject); 
        String meetingId = jsonObject.getString("meetingId");
        String meetingTypeId = jsonObject.getString("meetingTypeId");

        String result = "";

        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired() || !ROLE_ADMIN.equals(userToken.getRole())) {
            result = "{\"error\":\"invalid token\"}";
            return result;
        }

        Database db = new Database();
        if (db.changeMeetingType(meetingId, meetingTypeId) == 0) {
            result = "{\"meeting\":\"" + meetingId + "\"}";
        } else {
            result = "{\"error\":\"badness occurred\"}";
        }
        return result;
    }



    @PostMapping(value = "/test", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> example() {
        return Collections.singletonMap("key", "value");
    }

    @ResponseBody
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public String handleHttpMediaTypeNotAcceptableException() {
        return "acceptable MIME type:" + MediaType.APPLICATION_JSON_VALUE;
    }

    private String generateTokenForUser(String username, String role) throws Exception {
        // check database for secret key
        Database db = new Database();
        String secretKey = db.getSecretKey();
        if (secretKey == null) {
            secretKey = AESUtil.generateKeyAndIv(256);
            db.setSecretKey(secretKey);
        }

        Date now = new Date();
        Date expiry = new Date(now.getTime() + (1000 * TOKEN_EXPIRY_SECONDS));

        //get user orgaization id
        String orgId = db.getOrgIdFromUsername(username);

        // generate token
        Token token = new Token(username, now, expiry, role, orgId);
        String encryptedToken = TokenUtil.encrypt(token, secretKey);
        return encryptedToken;

    }

    private String generatePasswordResetTokenForUser(String email) throws Exception {
        // check database for secret key
        Database db = new Database();
        String secretKey = db.getSecretKey();
        if (secretKey == null) {
            secretKey = AESUtil.generateKeyAndIv(256);
            db.setSecretKey(secretKey);
        }

        Date now = new Date();
        Date expiry = new Date(now.getTime() + (1000 * 1800));

        // generate token
        PasswordResetToken token = new PasswordResetToken(email, now, expiry);
        String encryptedToken = PasswordResetTokenUtil.encrypt(token, secretKey);
        return encryptedToken;        
    }

    private Token getToken(String token) {
        // check database for secret key
        Database db = new Database();
        String secretKey = db.getSecretKey();
        if (secretKey == null) {
            System.out.println("No secret key found in database");
            return null;
        }
        Token decryptedToken = null;
        try {
            decryptedToken = TokenUtil.decrypt(token, secretKey);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return decryptedToken;
    }

    private PasswordResetToken getPasswordResetToken(String token) {
        // check database for secret key
        Database db = new Database();
        String secretKey = db.getSecretKey();
        if (secretKey == null) {
            System.out.println("No secret key found in database");
            return null;
        }
        PasswordResetToken decryptedToken = null;
        try {
            decryptedToken = PasswordResetTokenUtil.decrypt(token, secretKey);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return decryptedToken;
    }


}
