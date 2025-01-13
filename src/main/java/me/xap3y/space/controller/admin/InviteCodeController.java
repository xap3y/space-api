package me.xap3y.space.controller.admin;

import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.iface.RequiresSpecialApiKey;
import me.xap3y.space.entity.InviteCode;
import me.xap3y.space.model.AuthLoginRequest;
import me.xap3y.space.service.InviteCodeService;
import me.xap3y.space.util.Utils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/admin/invite")
public class InviteCodeController {


    private final InviteCodeService inviteCodeService;

    public InviteCodeController(InviteCodeService inviteCodeService) {
        this.inviteCodeService = inviteCodeService;
    }

    @PostMapping(
            value = "/create",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @RequiresSpecialApiKey
    public ResponseEntity<?> createInviteCode() {

        InviteCode code = new InviteCode(Utils.generateRandomId());
        inviteCodeService.createInviteCode(code);

        Map<String, Object> map = new HashMap<>(){{

        }};
        return new ResponseEntity<Map<String, Object>>(map, HttpStatus.ACCEPTED);
    }
}
