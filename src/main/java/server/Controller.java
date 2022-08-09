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

    @GetMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String users() {
        Database db = new Database();
        String result = db.getUsers();
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

    @GetMapping("start")
    public String start() {
        System.out.println("Start Called");
        Database db = new Database();
        int result = db.startMeeting_old();
        System.out.println("Result: " + result);
        if (result == 0) {
            Date date = new Date();  
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");  
            return "Meeting started at " + formatter.format(date);
        } else {
            return "Error";
        }    
    }

    @GetMapping("percentages")
    public String percentages(@RequestParam int percent) {
        System.out.println("Percentages Called");
        Database db = new Database();
        String result = db.getPercentages(percent);
        System.out.println("Result: " + result);
        return result;
    }

    @GetMapping("getstudents")
    public String listStudents() {
        System.out.println("List Students Called");
        Database db = new Database();
        String result = db.getStudents();
        System.out.println("Result: " + result);
        return result;
    }

    @GetMapping("checkmeeting")
    public int checkMeeting() {
        System.out.println("Check Meeting Called");
        Database db = new Database();
        int result = db.checkMeeting();
        return result;
    }

    @GetMapping("attendees")
    public String attendees() {
        System.out.println("Attendees Called");
        Database db = new Database();
        String result = db.getAttendees();
        System.out.println("Result: " + result);
        return result;
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
