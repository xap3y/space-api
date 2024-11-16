package me.xap3y.space.controller;

import lombok.Getter;
import me.xap3y.space.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/user")
@Getter
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createUser(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String role
    ) {

        return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);

        /*User newUser = userService.createUser(username, password, role);

        HashMap<String, String> response = new HashMap<>();
        response.put("username", newUser.getUsername());
        response.put("role", newUser.getRole());
        return new ResponseEntity<>(response, HttpStatus.CREATED);*/
    }

    @GetMapping("/{username}")
    public ResponseEntity<Map<String, String>> getUserByUsername(
            @PathVariable String username
    ) {

        return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);

        /*User user = userService.findByUsername(username);
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(user, HttpStatus.OK);*/
    }

}
