package me.xap3y.space.service;

import me.xap3y.space.api.enums.ResourceLimitPeriod;
import me.xap3y.space.api.enums.ResourceLimitType;
import me.xap3y.space.api.enums.UserRole;
import me.xap3y.space.api.exception.ResourceAccessForbiddenException;
import me.xap3y.space.entity.ResourceLimitProfile;
import me.xap3y.space.entity.ResourceLimitRule;
import me.xap3y.space.entity.User;
import me.xap3y.space.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceLimitServiceTest {

    @Mock ResourceLimitProfileRepository profileRepository;
    @Mock ResourceLimitRuleRepository ruleRepository;
    @Mock UserService userService;
    @Mock ImageRepository imageRepository;
    @Mock FileRepository fileRepository;
    @Mock PasteRepository pasteRepository;
    @Mock UrlRepository urlRepository;
    @Mock TempMailRepository tempMailRepository;
    @Mock ResourceUsageCounterRepository usageCounterRepository;

    @InjectMocks ResourceLimitService service;

    private User user;
    private ResourceLimitProfile roleProfile;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(42L);
        user.setRole(UserRole.USER);
        user.setUsername("limited-user");

        roleProfile = new ResourceLimitProfile();
        roleProfile.setId(10L);
        roleProfile.setRole(UserRole.USER);
        lenient().when(profileRepository.findByRole(UserRole.USER)).thenReturn(Optional.of(roleProfile));
        lenient().when(profileRepository.findByUserId(42L)).thenReturn(Optional.empty());
        lenient().when(userService.findById(42L)).thenReturn(Optional.of(user));
    }

    @Test
    void blocksCreationWhenDailyCountWouldBeExceeded() {
        ResourceLimitRule rule = rule(ResourceLimitType.IMAGE, ResourceLimitPeriod.DAY, 5L, null);
        when(ruleRepository.findByProfileId(10L)).thenReturn(List.of(rule));
        when(imageRepository.countByUploadTimeBetweenAndUploaderId(any(), any(), eq(42L))).thenReturn(5L);
        when(imageRepository.sumStorageByUploaderIdInRange(eq(42L), any(), any())).thenReturn(0L);

        assertThatThrownBy(() -> service.assertCanCreate(user, ResourceLimitType.IMAGE, 1, 100))
                .isInstanceOf(ResourceAccessForbiddenException.class)
                .hasMessageContaining("Daily image count limit exceeded");
    }

    @Test
    void blocksCreationWhenCombinedDailyBytesWouldBeExceeded() {
        ResourceLimitRule rule = rule(ResourceLimitType.TOTAL, ResourceLimitPeriod.DAY, null, 1_000L);
        when(ruleRepository.findByProfileId(10L)).thenReturn(List.of(rule));
        when(imageRepository.countByUploadTimeBetweenAndUploaderId(any(), any(), eq(42L))).thenReturn(1L);
        when(imageRepository.sumStorageByUploaderIdInRange(eq(42L), any(), any())).thenReturn(700L);
        when(fileRepository.sumStorageByUploaderIdInRange(eq(42L), any(), any())).thenReturn(0L);
        when(pasteRepository.sumContentBytesByUserInRange(eq(42L), any(), any())).thenReturn(0L);

        assertThatThrownBy(() -> service.assertCanCreate(user, ResourceLimitType.FILE, 1, 400))
                .isInstanceOf(ResourceAccessForbiddenException.class)
                .hasMessageContaining("Daily total byte limit exceeded");
    }

    @Test
    void blocksAllResourceMutationsWhilePaused() {
        ResourceLimitProfile userProfile = new ResourceLimitProfile();
        userProfile.setId(11L);
        userProfile.setUser(user);
        userProfile.setPausedUntil(LocalDateTime.now().plusHours(1));
        when(profileRepository.findByUserId(42L)).thenReturn(Optional.of(userProfile));

        assertThatThrownBy(() -> service.assertMutationAllowed(user))
                .isInstanceOf(ResourceAccessForbiddenException.class)
                .hasMessageContaining("paused until");
    }

    @Test
    void adminAccountsBypassLimitsAndPauses() {
        user.setRole(UserRole.ADMIN);
        assertThatCode(() -> service.assertCanCreate(user, ResourceLimitType.IMAGE, 1, Long.MAX_VALUE))
                .doesNotThrowAnyException();
        verifyNoInteractions(imageRepository, fileRepository, pasteRepository, urlRepository, tempMailRepository);
    }

    private ResourceLimitRule rule(ResourceLimitType type, ResourceLimitPeriod period, Long count, Long bytes) {
        ResourceLimitRule rule = new ResourceLimitRule();
        rule.setProfile(roleProfile);
        rule.setResourceType(type);
        rule.setPeriod(period);
        rule.setMaxCount(count);
        rule.setMaxBytes(bytes);
        return rule;
    }
}
