package me.xap3y.space.controller;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.UserDto;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.mapper.UserMapper;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.service.ImageService;
import me.xap3y.space.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/v1/user")
@Getter
@Slf4j
public class UserController {

    private final UserService userService;
    private final ImageService imageService;
    private final UserMapper userMapper;
    private final ServerInfo serverInfo;

    public UserController(UserService userService, ImageService imageService, UserMapper userMapper, ServerInfo serverInfo) {
        this.userService = userService;
        this.imageService = imageService;
        this.userMapper = userMapper;
        this.serverInfo = serverInfo;
    }

    @PostMapping("/create")
    public ResponseEntity<Object> createUser(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String email,
            @RequestParam(required = false, defaultValue = "false") Boolean test
    ) {

        //return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);

        if (!serverInfo.getEnv().equals("dev")) {
            return new ResponseEntity<>(new DefaultResponse(true, "Registration is currently disabled"), HttpStatus.FORBIDDEN);
        }

        userService.createUser(username, password, email, test);

        return new ResponseEntity<>(new DefaultResponse(false, "OK"), HttpStatus.CREATED);
    }

    @GetMapping(
            value = "/get/{username}",
            produces = "application/json"
    )
    public ResponseEntity<?> getUserByUsername(
            @PathVariable String username
    ) {

        log.info("Getting user by username: {}", username);
        UserDto userDto;
        try {
            userDto = userService.findByUsername(username)
                    .map(userMapper)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new DefaultResponse(true, e.getMessage(), LocalDateTime.now()));
        }

        Map<String, ?> map = imageService.getUserStats(userDto.uid());

        if (map != null) {
            try {
                userDto.stats().setStorageUsed((Long) map.get("total_size"));
                userDto.stats().setTotalUploads(Integer.parseInt(((Long) map.get("uploads")).toString()));
            } catch (Exception e) {
                // IGNORE
            }
        }




        return ResponseEntity.ok()
                .body(new DefaultResponse(false, userDto));

        /*User user = userService.findByUsername(username);
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(user, HttpStatus.OK);*/
    }

}
