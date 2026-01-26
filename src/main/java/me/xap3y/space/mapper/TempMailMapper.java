package me.xap3y.space.mapper;

import me.xap3y.space.dto.TempMailDto;
import me.xap3y.space.entity.TempMail;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class TempMailMapper implements Function<TempMail, TempMailDto> {

    private final ShortUserMapper shortUserMapper;

    public TempMailMapper(ShortUserMapper shortUserMapper) {
        this.shortUserMapper = shortUserMapper;
    }

    @Override
    public TempMailDto apply(TempMail email) {
        return new TempMailDto(
                email.getId(),
                email.getEmail(),
                email.getStatus(),
                email.getCreatedAt(),
                email.getExpireAt(),
                shortUserMapper.apply(email.getCreatedBy(), false)
        );
    }
}
