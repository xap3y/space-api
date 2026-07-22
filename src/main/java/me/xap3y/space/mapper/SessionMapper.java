package me.xap3y.space.mapper;

import me.xap3y.space.dto.SessionDto;
import me.xap3y.space.entity.Session;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class SessionMapper implements Function<Session, SessionDto> {

    private final ShortUserMapper shortUserMapper;

    public SessionMapper(ShortUserMapper shortUserMapper) {
        this.shortUserMapper = shortUserMapper;
    }

    public SessionDto apply(Session session, boolean includeUser, String currentToken) {
        return new SessionDto(
                session.getId(),
                includeUser ? shortUserMapper.apply(session.getUser()) : null,
                session.getCreatedAt(),
                session.getLastUsedAt(),
                session.getExpiresAt(),
                session.getIsValid(),
                session.getUserAgent(),
                session.getIpAddress(),
                currentToken != null && currentToken.equals(session.getToken())
        );
    }

    public SessionDto apply(Session session, boolean includeUser) {
        return apply(session, includeUser, null);
    }

    @Override
    public SessionDto apply(Session session) {
        return apply(session, false, null);
    }
}
