package server;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
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
public class Controller {

    @GetMapping("/hello")
    public String hello() {
        return "Hello World!";
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
        int result = db.startMeeting();
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
