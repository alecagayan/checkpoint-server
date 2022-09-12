package server;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.HttpMediaTypeNotAcceptableException;

import java.util.Collections;
import java.util.Map;
import java.util.Date;

import org.json.*;

@RestController
@RequestMapping("/rbapi")
@CrossOrigin(origins = "*")
public class Controller {

    public static final int TOKEN_EXPIRY_SECONDS = 60 * 60 * 2;

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
            try {
                tokenValue = generateTokenForUser(login);
            } catch (Exception e) {
                e.printStackTrace();
                result = "{\"error\":\"cannot generate token\"}";
                return result;
            }
            result = "{\"token\":\"" + tokenValue + "\"}";
        } else {
            result = "{\"error\":\"invalid credentials\"}";
        }
        return result;
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String register(@RequestBody String json) {
        JSONObject jsonObject = new JSONObject(json);
        String id = jsonObject.getString("id");
        String name = jsonObject.getString("name");
        String email = jsonObject.getString("email");
        String registrationCode = jsonObject.getString("registrationcode");

        String result = "";
        Database db = new Database();
        if (db.register(id, name, email, registrationCode) == 0) {
            result = "{\"result\":\"" + "1" + "\"}";
        } else {
            result = "{\"error\":\"badness occurred\"}";
        }
        return result;
    }

    @PostMapping(value = "/adduser", consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String adduser(@RequestBody String json) {
        JSONObject jsonObject = new JSONObject(json);
        String name = jsonObject.getString("name");
        String email = jsonObject.getString("email");
        String username = jsonObject.getString("username");
        String password = jsonObject.getString("password");
        int role = jsonObject.getInt("role");

        String token = jsonObject.getString("token");
        String result = "";

        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired()) {
            result = "{\"error\":\"invalid token\"}";
            return result;
        }

        if (role == 1 && (password == null || password.isEmpty())) {
            result = "{\"error\":\"password is required\"}";
            return result;
        }

        Database db = new Database();
        if (db.addUser(name, email, username, role, 1, password) == 0) {
            result = "{\"user\":\"" + username + "\"}";
        } else {
            result = "{\"error\":\"badness occurred\"}";
        }
        return result;
    }

    @PostMapping(value = "/updateuser", consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String updateuser(@RequestBody String json) {
        JSONObject jsonObject = new JSONObject(json);
        System.out.println(jsonObject.toString());
        String name = jsonObject.getString("name");
        int id = jsonObject.getInt("id");
        String login = jsonObject.getString("login");
        String email = jsonObject.getString("email");
        int role = jsonObject.getInt("role");
        int status = jsonObject.getInt("status");
        String token = jsonObject.getString("token");
        String result = "";

        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired()) {
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
    public @ResponseBody String checkin(@RequestBody String json) {
        JSONObject jsonObject = new JSONObject(json);
        System.out.println("jsonObject: " + jsonObject); 
        String username = jsonObject.getString("login");
        String token = jsonObject.getString("token");
        String meetingId = jsonObject.getString("meetingId");
        String result = "";

        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired()) {
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

        int returnCode = db.checkIn(username, meetingId);
        result = "{\"result\":\"" + returnCode + "\"}";

        return result;
    }

    @PostMapping(value = "/checkout", consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String checkout(@RequestBody String json) {
        JSONObject jsonObject = new JSONObject(json);
        String username = jsonObject.getString("login");
        String token = jsonObject.getString("token");
        String meetingId = jsonObject.getString("meetingId");
        String result = "";

        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired()) {
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


    @GetMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String users() {
        Database db = new Database();
        String result = db.getUsers();
        return result;
    }

    @GetMapping(value = "/user", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String user(@RequestParam String userId) {
        Database db = new Database();
        String result = db.getUser(userId);
        return result;
    }

    @GetMapping(value = "/attendance", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String attendance(@RequestParam String userId) {
        Database db = new Database();
        String result = db.getAttendance(userId);
        return result;
    }

    @GetMapping(value = "/reportbydate", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String reportByDate(@RequestParam(value = "startDate", defaultValue = "") String startDate,
            @RequestParam(value = "endDate", defaultValue = "") String endDate) {
        Database db = new Database();
        String result = db.getUsersBetweenDates(startDate, endDate);
        return result;
    }

    @GetMapping(value = "/rawdatabydate", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String rawDataByDate(@RequestParam(value = "startDate", defaultValue = "") String startDate,
            @RequestParam(value = "endDate", defaultValue = "") String endDate) {
        Database db = new Database();
        String result = db.getRawDataBetweenDates(startDate, endDate);
        return result;
    }

    @GetMapping(value = "/recentmeetings", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String recentMeetings(@RequestParam(value = "limit") String limit) {
        Database db = new Database();
        int size = 10;
        if (limit != null) {
            size = Integer.parseInt(limit);
        }
        String result = db.getRecentMeetings(size);
        return result;
    }

    @GetMapping(value = "/topattendees", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String topAttendees(@RequestParam(value = "startDate", defaultValue = "") String startDate,
            @RequestParam(value = "endDate", defaultValue = "") String endDate, 
            @RequestParam(value = "limit") String limit) {
        Database db = new Database();
        int size = 5;
        if (limit != null) {
            size = Integer.parseInt(limit);
        }
        String result = db.getUsersBetweenDates(startDate, endDate, size);
        return result;
    }

    @GetMapping(value = "/attendees", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String attendees(@RequestParam(value = "meetingId") String meetingId) {
        Database db = new Database();
        String result = db.getAttendees(meetingId);
        return result;
    }

    @GetMapping(value = "/meeting", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String meeting(@RequestParam(value = "meetingId") String meetingId) {
        Database db = new Database();
        String result = db.getMeeting(meetingId);
        System.out.println("result: " + result);
        return result;
    }

    @GetMapping(value = "/meetings", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String meetings() {
        Database db = new Database();
        String result = db.getMeetings();
        return result;
    }

    @PostMapping(value = "/startmeeting", consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String startmeeting(@RequestBody String json) {
        JSONObject jsonObject = new JSONObject(json);
        System.out.println("jsonObject: " + jsonObject); 
        String token = jsonObject.getString("token");
        String result = "";

        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired()) {
            result = "{\"error\":\"invalid token\"}";
            return result;
        }

        Database db = new Database();
        int meetingId = db.createMeeting(userToken.getUsername());
        if (meetingId != 0) {
            result = "{\"meeting\":\"" + meetingId + "\"}";
        } else {
            result = "{\"error\":\"badness occurred\"}";
        }
        return result;
    }
    

    @PostMapping(value = "/closemeeting", consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String closemeeting(@RequestBody String json) {
        JSONObject jsonObject = new JSONObject(json);
        System.out.println("jsonObject: " + jsonObject); 
        String token = jsonObject.getString("token");
        String meetingId = jsonObject.getString("meetingId");
        String result = "";

        // check if token is valid
        Token userToken = getToken(token);
        if (userToken == null || userToken.isExpired()) {
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

    @GetMapping("signin")
    public String signin(@RequestParam String student_id, @RequestParam String account) {
        System.out.println("Signin Called with for student_id : " + student_id);
        Database db = new Database();
        if (db.checkIn(student_id, account) == 0) {
            return "Signed in " + student_id + " for " + account;
        } else {
            return "Error";
        }
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

    private String generateTokenForUser(String username) throws Exception {
        // check database for secret key
        Database db = new Database();
        String secretKey = db.getSecretKey();
        if (secretKey == null) {
            secretKey = AESUtil.generateKeyAndIv(256);
            db.setSecretKey(secretKey);
        }

        Date now = new Date();
        Date expiry = new Date(now.getTime() + (1000 * TOKEN_EXPIRY_SECONDS));

        // generate token
        Token token = new Token(username, now, expiry, "admin");
        String encryptedToken = TokenUtil.encrypt(token, secretKey);
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

}
