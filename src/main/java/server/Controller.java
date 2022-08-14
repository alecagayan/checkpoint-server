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

import javax.annotation.processing.Generated;

import java.text.SimpleDateFormat;  
import java.util.Date;

import org.json.*;

@RestController
@RequestMapping("/rbapi")
@CrossOrigin(origins = "*")
public class Controller {

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
            result = "{\"token\":\"" + login + "\"}";
        } else {
            result = "{\"error\":\"badness occurred\"}";
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
        String result = "";
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
        String username = jsonObject.getString("login");
        String token = jsonObject.getString("storedToken");
        String meetingId = jsonObject.getString("meetingId");
        String result = "";
        Database db = new Database();
        System.out.println("checking in user " + username + " with token " + token + " for meeting " + meetingId);
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
    public @ResponseBody String reportByDate(@RequestParam(value = "startDate", defaultValue = "") String startDate, @RequestParam(value = "endDate", defaultValue = "") String endDate) {
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
        String result = "";
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

    @PostMapping(
    value = "/test", 
    consumes = MediaType.APPLICATION_JSON_VALUE, 
    produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> example() {
        return Collections.singletonMap("key", "value");
    }

    @ResponseBody
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public String handleHttpMediaTypeNotAcceptableException() {
        return "acceptable MIME type:" + MediaType.APPLICATION_JSON_VALUE;
    }

}
