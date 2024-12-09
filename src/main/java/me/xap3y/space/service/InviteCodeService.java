package me.xap3y.space.service;

import me.xap3y.space.repository.InviteCodeRepository;
import org.springframework.stereotype.Service;

@Service
public class InviteCodeService {

    private final InviteCodeRepository inviteCodeRepository;

    public InviteCodeService(InviteCodeRepository inviteCodeRepository) {
        this.inviteCodeRepository = inviteCodeRepository;
    }

    public boolean isValidInviteCode(String code) {
        return inviteCodeRepository.existsByCode(code);
    }

    public void deleteInviteCode(String code) {
        inviteCodeRepository.deleteByCode(code);
    }
}
