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
        String token = jsonObject.getString("token");
        String result = "";

        // check if token is valid
        if (!validateToken(token)) {
            result = "{\"error\":\"invalid token\"}";
            return result;
        }

        Database db = new Database();
        if (db.addUser(name, email, username) == 0) {
            result = "{\"user\":\"" + username + "\"}";
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
        if (!validateToken(token)) {
            result = "{\"error\":\"invalid token\"}";
            return result;
        }

        System.out.println("checking in user " + username + " with token " + token + " for meeting " + meetingId);
        Database db = new Database();
        if (db.checkIn(username, meetingId) == 0) {
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

    @GetMapping(value = "/reportbydate", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String reportByDate(@RequestParam(value = "startDate", defaultValue = "") String startDate,
            @RequestParam(value = "endDate", defaultValue = "") String endDate) {
        Database db = new Database();
        String result = db.getUsersBetweenDates(startDate, endDate);
        return result;
    }

    @GetMapping(value = "/attendees", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String attendees(@RequestParam(value = "meetingId") String meetingId) {
        Database db = new Database();
        String result = db.getAttendees(meetingId);
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
        // System.out.println("jsonObject: " + jsonObject); 
        // String token = jsonObject.getString("token");
        // String meetingId = jsonObject.getString("meetingId");
        String result = "";


        // // check if token is valid
        // if (!validateToken(token)) {
        //     result = "{\"error\":\"invalid token\"}";
        //     return result;
        // }

        Database db = new Database();
        if (db.createMeeting() == 0) {
            result = "{\"meeting\":\"" + "1" + "\"}";
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

    private boolean validateToken(String token) {
        // check database for secret key
        Database db = new Database();
        String secretKey = db.getSecretKey();
        if (secretKey == null) {
            return false;
        }
        try {
            Token decryptedToken = TokenUtil.decrypt(token, secretKey);
            if (decryptedToken.isExpired()) {
                System.out.println("Token expired");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

}
