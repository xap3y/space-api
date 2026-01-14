package me.xap3y.space.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.iface.RequiresSpecialApiKey;
import me.xap3y.space.dto.InviteCodeDto;
import me.xap3y.space.entity.InviteCode;
import me.xap3y.space.mapper.InviteCodeMapper;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.service.InviteCodeService;
import me.xap3y.space.util.Utils;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/admin/invite")
public class InviteCodeController {


    private final InviteCodeService inviteCodeService;
    private final InviteCodeMapper inviteCodeMapper;

    public InviteCodeController(InviteCodeService inviteCodeService, InviteCodeMapper inviteCodeMapper) {
        this.inviteCodeService = inviteCodeService;
        this.inviteCodeMapper = inviteCodeMapper;
    }

    @PostMapping(
            value = "/create",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @RequiresSpecialApiKey
    public ResponseEntity<?> createInviteCode(
            @RequestParam(value = "code", required = false) @Nullable String customCode,
            @RequestParam(value = "prefix", required = false) @Nullable String customPrefix,
            @RequestParam(value = "amount", required = false) @Nullable Integer amount
    ) {
        if (customCode != null && customCode.length() < 3) {
            return new ResponseEntity<>(new DefaultResponse(true, "Custom code must be at least 3 characters long"), HttpStatus.BAD_REQUEST);
        }

        else if (amount != null && amount < 1) {
            return new ResponseEntity<>(new DefaultResponse(true, "Amount must be at least 1"), HttpStatus.BAD_REQUEST);
        }

        else if (customPrefix != null && customPrefix.length() < 3) {
            return new ResponseEntity<>(new DefaultResponse(true, "Custom prefix must be at least 3 characters long"), HttpStatus.BAD_REQUEST);
        }

        if (amount != null && amount > 1) {
            List<InviteCodeDto> inviteCodeDtos = new ArrayList<>();
            for (int i = 0; i < amount; i++) {
                InviteCode code = new InviteCode(Utils.generateRandomId());
                if (customPrefix != null) {
                    code.setCode(customPrefix + "-" + code.getCode());
                }
                inviteCodeService.createInviteCode(code);
                InviteCodeDto codeDto = inviteCodeMapper.apply(code);
                inviteCodeDtos.add(codeDto);
            }
            List<String> inviteCodes = inviteCodeDtos.stream().map(InviteCodeDto::code).toList();
            return new ResponseEntity<>(new DefaultResponse(false, inviteCodes), HttpStatus.ACCEPTED);
        }

        String codeStr = customCode != null ? customCode : Utils.generateRandomId();
        if (customPrefix != null) {
            codeStr = customPrefix + "-" + codeStr;
        }
        InviteCode code = new InviteCode(codeStr);
        inviteCodeService.createInviteCode(code);

        InviteCodeDto codeDto = inviteCodeMapper.apply(code);

        return new ResponseEntity<>(new DefaultResponse(false, codeDto), HttpStatus.ACCEPTED);
    }

    @GetMapping(
            value = "/get",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @RequiresSpecialApiKey
    public ResponseEntity<?> getInviteCode(
            HttpServletRequest request,
            @RequestParam(value = "used", required = false) Boolean used,
            @RequestParam(value = "after", required = false) String afterDate
    ) {
        List<InviteCode> codes;
        if (used == null ) {
            codes = inviteCodeService.findAll();
        } else {
            codes = inviteCodeService.findAllByUsed(used);
        }

        if (codes == null) {
            return new ResponseEntity<>(new DefaultResponse(true, List.of()), HttpStatus.OK);
        }

        List<InviteCodeDto> codeDtos = codes.stream().map(inviteCodeMapper).toList();

        return new ResponseEntity<>(new DefaultResponse(false, codeDtos, codeDtos.size()), HttpStatus.OK);
    }
}
