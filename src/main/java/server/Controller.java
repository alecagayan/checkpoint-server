package server;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.json.*;

@RestController
@RequestMapping("/rbapi")
public class Controller {

    @GetMapping("/hello")
    public String hello() {
        return "Hello World!";
    }

    @PostMapping(value = "/signin", consumes = "application/json", produces = "application/json")
    public String signin(@RequestBody String body) {
        System.out.println("POST Called with body: " + body);
        Database db = new Database();
        JSONObject json = new JSONObject(body);
        String student_id = json.getString("student_id");
        String account = json.getString("account");

        db.checkIn(student_id, account);

        return json.toString();
    }

}
