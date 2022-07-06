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
