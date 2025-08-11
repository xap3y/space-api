package me.xap3y.space.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.entity.EmailVerifyCodes;
import me.xap3y.space.entity.User;
import me.xap3y.space.repository.EmailVerifyCodeRepository;
import me.xap3y.space.util.Utils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class EmailVerifyCodeService {

    private final EmailVerifyCodeRepository emailVerifyCodeRepository;

    public EmailVerifyCodes save(User user, String code) {
        return emailVerifyCodeRepository.save(new EmailVerifyCodes(code, user));
    }

    public void deleteByEmail(String email) {
        emailVerifyCodeRepository.deleteByEmail(email);
    }

    public boolean existsByEmail(String email) {
        return emailVerifyCodeRepository.existsByEmail(email);
    }

    public boolean existsByTelCode(String code) {
        return emailVerifyCodeRepository.existsByTelCode(code);
    }

    public void deleteAll() {
        emailVerifyCodeRepository.deleteAll();
    }

    public void deleteById(Long id) {
        emailVerifyCodeRepository.deleteById(id);
    }

    public void deleteAllByEmail(String email) {
        emailVerifyCodeRepository.deleteByEmail(email);
    }

    public Optional<EmailVerifyCodes> findByCode(String code) {
        return emailVerifyCodeRepository.findByCode(code);
    }

    public EmailVerifyCodes findByCodeStrict(String code) {
        return emailVerifyCodeRepository.findByCode(code).orElseThrow(() -> new ResourceNotFoundException("Email verification code not found: " + code));
    }

    public EmailVerifyCodes findByUrlCodeStrict(String code) {
        return emailVerifyCodeRepository.findByUrlCode(code).orElseThrow(() -> new ResourceNotFoundException("Email verification code not found: " + code));
    }

    public EmailVerifyCodes findByTelCodeStrict(String code) {
        return emailVerifyCodeRepository.findByTelCode(code).orElseThrow(() -> new ResourceNotFoundException("Email verification code not found: " + code));
    }

    public EmailVerifyCodes findTopByUserStrict(User user) {
        return emailVerifyCodeRepository.findTopByUserOrderByCreatedAtDesc(user).orElseThrow(() -> new ResourceNotFoundException("Email verification code not found: " + user.getUsername()));
    }

    public EmailVerifyCodes findTopByEmailStrict(String mail) {
        return emailVerifyCodeRepository.findTopByEmailOrderByCreatedAtDesc(mail).orElseThrow(() -> new ResourceNotFoundException("Email verification code not found: " + mail));
    }

    public void setCodeUsed(EmailVerifyCodes code) {
        code.setUsed(true);
        emailVerifyCodeRepository.save(code);
    }

    public EmailVerifyCodes generateAndSaveCode(User user) {
        String code = Utils.generateRandom6DigitNumber();
        return save(user, code);
    }
}
