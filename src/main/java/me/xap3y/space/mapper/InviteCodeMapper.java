package me.xap3y.space.mapper;

import me.xap3y.space.dto.InviteCodeDto;
import me.xap3y.space.entity.InviteCode;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class InviteCodeMapper implements Function<InviteCode, InviteCodeDto> {

    private final ShortUserMapper shortUserMapper;

    public InviteCodeMapper(ShortUserMapper shortUserMapper) {
        this.shortUserMapper = shortUserMapper;
    }

    @Override
    public InviteCodeDto apply(InviteCode inviteCode) {
        return new InviteCodeDto(
                inviteCode.getCode(),
                inviteCode.isUsed(),
                inviteCode.getCreatedAt(),
                inviteCode.getUsedAt(),
                inviteCode.getCreatedBy() != null ? shortUserMapper.apply(inviteCode.getCreatedBy()) : null,
                inviteCode.getUsedBy() != null ? shortUserMapper.apply(inviteCode.getUsedBy()) : null
        );
    }
}
