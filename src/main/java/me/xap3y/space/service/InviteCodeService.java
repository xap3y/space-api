package me.xap3y.space.service;

import me.xap3y.space.entity.InviteCode;
import me.xap3y.space.repository.InviteCodeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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
        this.inviteCodeRepository.deleteByCode(code);
    }

    public void createInviteCode(InviteCode code) {
        if (this.inviteCodeRepository.existsByCode(code.getCode())) {
            throw new IllegalArgumentException("Invite code already exists");
        }
        this.inviteCodeRepository.save(code);
    }

    public List<InviteCode> findAllByUsed(boolean used) {
        return this.inviteCodeRepository.findAllByUsed(used);
    }

    public List<InviteCode> findAll() {
        return this.inviteCodeRepository.findAll();
    }

    public List<InviteCode> findAllByUsedAndCreatedAtAfter(boolean used, LocalDateTime createdAtAfter) {
        return this.inviteCodeRepository.findAllByUsedAndCreatedAtAfter(used, createdAtAfter);
    }


}
