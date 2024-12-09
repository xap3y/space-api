package me.xap3y.space.mapper;

import me.xap3y.space.dto.InviteCodeDto;
import me.xap3y.space.entity.InviteCode;

import java.util.function.Function;

public class InviteCodeMapper implements Function<InviteCode, InviteCodeDto> {

    @Override
    public InviteCodeDto apply(InviteCode inviteCode) {
        return new InviteCodeDto(
                inviteCode.getCode(),
                inviteCode.isUsed(),
                inviteCode.getCreatedAt(),
                inviteCode.getUsedAt(),
                inviteCode.getCreatedBy() != null ? inviteCode.getCreatedBy().getId() : null,
                inviteCode.getUsedBy() != null ? inviteCode.getUsedBy().getId() : null
        );
    }
}
