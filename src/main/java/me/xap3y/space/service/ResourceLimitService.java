package me.xap3y.space.service;

import lombok.RequiredArgsConstructor;
import me.xap3y.space.api.enums.ResourceLimitPeriod;
import me.xap3y.space.api.enums.ResourceLimitType;
import me.xap3y.space.api.enums.UserRole;
import me.xap3y.space.api.exception.BadRequestException;
import me.xap3y.space.api.exception.ResourceAccessForbiddenException;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.dto.ResourceLimitDtos.*;
import me.xap3y.space.entity.ResourceLimitProfile;
import me.xap3y.space.entity.ResourceLimitRule;
import me.xap3y.space.entity.ResourceUsageCounter;
import me.xap3y.space.entity.User;
import me.xap3y.space.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ResourceLimitService {

    private static final List<UserRole> MANAGED_ROLES = Arrays.stream(UserRole.values())
            .filter(role -> role != UserRole.ADMIN && role != UserRole.OWNER)
            .toList();

    private final ResourceLimitProfileRepository profileRepository;
    private final ResourceLimitRuleRepository ruleRepository;
    private final UserService userService;
    private final ImageRepository imageRepository;
    private final FileRepository fileRepository;
    private final PasteRepository pasteRepository;
    private final UrlRepository urlRepository;
    private final TempMailRepository tempMailRepository;
    private final ResourceUsageCounterRepository usageCounterRepository;

    public boolean isExempt(User user) {
        return user == null || user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.OWNER;
    }

    @Transactional(readOnly = true)
    public void assertMutationAllowed(User user) {
        if (isExempt(user)) return;
        ResourceLimitProfile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        if (profile == null) return;

        LocalDateTime now = LocalDateTime.now();
        if (profile.isPausedIndefinitely())
            throw new ResourceAccessForbiddenException("Your account is paused indefinitely. You can view resources, but cannot create, edit, or delete them.");
        if (profile.getPausedUntil() != null && profile.getPausedUntil().isAfter(now))
            throw new ResourceAccessForbiddenException("Your account is paused until " + profile.getPausedUntil() + ". You can view resources, but cannot create, edit, or delete them.");
    }

    @Transactional
    public void assertCanCreate(User user, ResourceLimitType type, long incomingCount, long incomingBytes) {
        if (isExempt(user)) return;
        assertMutationAllowed(user);

        Map<ResourceLimitType, RuleValues> effective = effectiveRules(user);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekStart = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();

        checkRule(type, ResourceLimitPeriod.DAY, effective.get(type), usage(user.getId(), type, dayStart, now), incomingCount, incomingBytes);
        checkRule(type, ResourceLimitPeriod.WEEK, effective.get(type), usage(user.getId(), type, weekStart, now), incomingCount, incomingBytes);

        if (type == ResourceLimitType.IMAGE || type == ResourceLimitType.FILE || type == ResourceLimitType.PASTE) {
            checkRule(ResourceLimitType.TOTAL, ResourceLimitPeriod.DAY, effective.get(ResourceLimitType.TOTAL),
                    usage(user.getId(), ResourceLimitType.TOTAL, dayStart, now), incomingCount, incomingBytes);
            checkRule(ResourceLimitType.TOTAL, ResourceLimitPeriod.WEEK, effective.get(ResourceLimitType.TOTAL),
                    usage(user.getId(), ResourceLimitType.TOTAL, weekStart, now), incomingCount, incomingBytes);
        }
    }

    @Transactional
    public void recordCreation(User user, ResourceLimitType type, long count, long bytes) {
        if (isExempt(user) || type == ResourceLimitType.TOTAL) return;
        LocalDate today = LocalDate.now();
        Optional<ResourceUsageCounter> existing = usageCounterRepository
                .findByUserIdAndResourceTypeAndUsageDate(user.getId(), type, today);
        if (existing.isEmpty()) {
            // A direct service caller may not have run assertCanCreate first. The persisted
            // resource is already included in this baseline, so do not add it twice.
            usageForDay(user, type, today);
            return;
        }
        ResourceUsageCounter counter = existing.get();
        counter.setUsedCount(Math.addExact(counter.getUsedCount(), Math.max(0, count)));
        counter.setUsedBytes(Math.addExact(counter.getUsedBytes(), Math.max(0, bytes)));
        usageCounterRepository.save(counter);
    }

    private void checkRule(ResourceLimitType type,
                           ResourceLimitPeriod period,
                           RuleValues limits,
                           WindowUsage usage,
                           long incomingCount,
                           long incomingBytes) {
        if (limits == null) return;
        Long maxCount = period == ResourceLimitPeriod.DAY ? limits.dailyCount() : limits.weeklyCount();
        Long maxBytes = period == ResourceLimitPeriod.DAY ? limits.dailyBytes() : limits.weeklyBytes();
        String window = period == ResourceLimitPeriod.DAY ? "Daily" : "Weekly";
        String label = type == ResourceLimitType.TOTAL ? "total" : type.name().toLowerCase(Locale.ROOT).replace('_', ' ');

        if (maxCount != null && usage.count + incomingCount > maxCount)
            throw new ResourceAccessForbiddenException(window + " " + label + " count limit exceeded (used " + usage.count + ", limit " + maxCount + ")");
        if (maxBytes != null && usage.bytes + incomingBytes > maxBytes)
            throw new ResourceAccessForbiddenException(window + " " + label + " byte limit exceeded (used " + usage.bytes + ", requested " + incomingBytes + ", limit " + maxBytes + ")");
    }

    @Transactional(readOnly = true)
    public List<RolePolicy> getRolePolicies() {
        return MANAGED_ROLES.stream()
                .map(role -> new RolePolicy(role, configuredRules(profileRepository.findByRole(role).orElse(null))))
                .toList();
    }

    @Transactional
    public RolePolicy updateRolePolicy(UserRole role, PolicyUpdate update) {
        requireManagedRole(role);
        ResourceLimitProfile profile = profileRepository.findByRole(role).orElseGet(() -> {
            ResourceLimitProfile created = new ResourceLimitProfile();
            created.setRole(role);
            return profileRepository.save(created);
        });
        saveRules(profile, update == null ? null : update.limits());
        return new RolePolicy(role, configuredRules(profile));
    }

    @Transactional
    public UserPolicy getUserPolicy(Long userId) {
        User user = requireManagedUser(userId);
        ResourceLimitProfile profile = profileRepository.findByUserId(userId).orElse(null);
        Map<ResourceLimitType, RuleValues> overrides = configuredRules(profile);
        LocalDateTime now = LocalDateTime.now();
        boolean paused = profile != null && (profile.isPausedIndefinitely()
                || profile.getPausedUntil() != null && profile.getPausedUntil().isAfter(now));
        return new UserPolicy(user.getId(), user.getUsername(), user.getRole(), overrides, effectiveRules(user), usageView(userId),
                paused, profile != null && profile.isPausedIndefinitely(), profile == null ? null : profile.getPausedUntil());
    }

    @Transactional
    public UserPolicy updateUserPolicy(Long userId, PolicyUpdate update) {
        User user = requireManagedUser(userId);
        ResourceLimitProfile profile = getOrCreateUserProfile(user);
        saveRules(profile, update == null ? null : update.limits());
        return getUserPolicy(userId);
    }

    @Transactional
    public UserPolicy clearUserOverrides(Long userId) {
        User user = requireManagedUser(userId);
        ResourceLimitProfile profile = profileRepository.findByUserId(userId).orElse(null);
        if (profile != null) ruleRepository.deleteByProfileId(profile.getId());
        return getUserPolicy(user.getId());
    }

    @Transactional
    public UserPolicy pauseUser(Long userId, PauseRequest request) {
        User user = requireManagedUser(userId);
        if (request == null) throw new BadRequestException("Pause duration is required");
        boolean indefinite = Boolean.TRUE.equals(request.indefinite());
        if (!indefinite && (request.durationMinutes() == null || request.durationMinutes() <= 0))
            throw new BadRequestException("durationMinutes must be greater than zero");

        ResourceLimitProfile profile = getOrCreateUserProfile(user);
        profile.setPausedIndefinitely(indefinite);
        profile.setPausedUntil(indefinite ? null : LocalDateTime.now().plusMinutes(request.durationMinutes()));
        profileRepository.save(profile);
        return getUserPolicy(userId);
    }

    @Transactional
    public UserPolicy unpauseUser(Long userId) {
        User user = requireManagedUser(userId);
        ResourceLimitProfile profile = profileRepository.findByUserId(userId).orElse(null);
        if (profile != null) {
            profile.setPausedIndefinitely(false);
            profile.setPausedUntil(null);
            profileRepository.save(profile);
        }
        return getUserPolicy(user.getId());
    }

    private ResourceLimitProfile getOrCreateUserProfile(User user) {
        return profileRepository.findByUserId(user.getId()).orElseGet(() -> {
            ResourceLimitProfile created = new ResourceLimitProfile();
            created.setUser(user);
            return profileRepository.save(created);
        });
    }

    private User requireManagedUser(Long userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (isExempt(user)) throw new BadRequestException("ADMIN and OWNER accounts cannot have resource limits or pauses");
        return user;
    }

    private void requireManagedRole(UserRole role) {
        if (role == null || !MANAGED_ROLES.contains(role))
            throw new BadRequestException("ADMIN and OWNER roles cannot have resource limits");
    }

    private Map<ResourceLimitType, RuleValues> effectiveRules(User user) {
        Map<ResourceLimitType, RuleValues> roleRules = configuredRules(profileRepository.findByRole(user.getRole()).orElse(null));
        Map<ResourceLimitType, RuleValues> userRules = configuredRules(profileRepository.findByUserId(user.getId()).orElse(null));
        Map<ResourceLimitType, RuleValues> result = new EnumMap<>(ResourceLimitType.class);
        for (ResourceLimitType type : ResourceLimitType.values())
            result.put(type, merge(roleRules.get(type), userRules.get(type)));
        return result;
    }

    private RuleValues merge(RuleValues base, RuleValues override) {
        if (base == null) base = new RuleValues(null, null, null, null);
        if (override == null) return base;
        return new RuleValues(
                override.dailyCount() != null ? override.dailyCount() : base.dailyCount(),
                override.weeklyCount() != null ? override.weeklyCount() : base.weeklyCount(),
                override.dailyBytes() != null ? override.dailyBytes() : base.dailyBytes(),
                override.weeklyBytes() != null ? override.weeklyBytes() : base.weeklyBytes());
    }

    private Map<ResourceLimitType, RuleValues> configuredRules(ResourceLimitProfile profile) {
        Map<ResourceLimitType, MutableRule> mutable = new EnumMap<>(ResourceLimitType.class);
        for (ResourceLimitType type : ResourceLimitType.values()) mutable.put(type, new MutableRule());
        if (profile != null) {
            for (ResourceLimitRule rule : ruleRepository.findByProfileId(profile.getId())) {
                MutableRule target = mutable.get(rule.getResourceType());
                if (rule.getPeriod() == ResourceLimitPeriod.DAY) {
                    target.dailyCount = rule.getMaxCount();
                    target.dailyBytes = rule.getMaxBytes();
                } else {
                    target.weeklyCount = rule.getMaxCount();
                    target.weeklyBytes = rule.getMaxBytes();
                }
            }
        }
        Map<ResourceLimitType, RuleValues> result = new EnumMap<>(ResourceLimitType.class);
        mutable.forEach((type, value) -> result.put(type, value.toValues()));
        return result;
    }

    private void saveRules(ResourceLimitProfile profile, Map<ResourceLimitType, RuleValues> limits) {
        ruleRepository.deleteByProfileId(profile.getId());
        if (limits == null) return;
        List<ResourceLimitRule> rules = new ArrayList<>();
        for (Map.Entry<ResourceLimitType, RuleValues> entry : limits.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            validate(entry.getValue());
            addRule(rules, profile, entry.getKey(), ResourceLimitPeriod.DAY, entry.getValue().dailyCount(), entry.getValue().dailyBytes());
            addRule(rules, profile, entry.getKey(), ResourceLimitPeriod.WEEK, entry.getValue().weeklyCount(), entry.getValue().weeklyBytes());
        }
        ruleRepository.saveAll(rules);
    }

    private void validate(RuleValues values) {
        for (Long value : List.of(
                valueOrZero(values.dailyCount()), valueOrZero(values.weeklyCount()),
                valueOrZero(values.dailyBytes()), valueOrZero(values.weeklyBytes()))) {
            if (value < 0) throw new BadRequestException("Limits cannot be negative");
        }
    }

    private long valueOrZero(Long value) {
        return value == null ? 0 : value;
    }

    private void addRule(List<ResourceLimitRule> rules, ResourceLimitProfile profile, ResourceLimitType type,
                         ResourceLimitPeriod period, Long count, Long bytes) {
        if (count == null && bytes == null) return;
        ResourceLimitRule rule = new ResourceLimitRule();
        rule.setProfile(profile);
        rule.setResourceType(type);
        rule.setPeriod(period);
        rule.setMaxCount(count);
        rule.setMaxBytes(bytes);
        rules.add(rule);
    }

    private Map<ResourceLimitType, UsageValues> usageView(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        Map<ResourceLimitType, UsageValues> result = new EnumMap<>(ResourceLimitType.class);
        for (ResourceLimitType type : ResourceLimitType.values()) {
            WindowUsage day = usage(userId, type, dayStart, now);
            WindowUsage week = usage(userId, type, weekStart, now);
            result.put(type, new UsageValues(day.count, week.count, day.bytes, week.bytes));
        }
        return result;
    }

    private WindowUsage usage(Long userId, ResourceLimitType type, LocalDateTime from, LocalDateTime to) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (type == ResourceLimitType.TOTAL) {
            WindowUsage images = usage(userId, ResourceLimitType.IMAGE, from, to);
            WindowUsage files = usage(userId, ResourceLimitType.FILE, from, to);
            WindowUsage pastes = usage(userId, ResourceLimitType.PASTE, from, to);
            return new WindowUsage(images.count + files.count + pastes.count, images.bytes + files.bytes + pastes.bytes);
        }

        WindowUsage total = new WindowUsage(0, 0);
        for (LocalDate day = from.toLocalDate(); !day.isAfter(to.toLocalDate()); day = day.plusDays(1)) {
            WindowUsage daily = usageForDay(user, type, day);
            total = new WindowUsage(total.count + daily.count, total.bytes + daily.bytes);
        }
        return total;
    }

    private WindowUsage usageForDay(User user, ResourceLimitType type, LocalDate day) {
        Optional<ResourceUsageCounter> existing = usageCounterRepository
                .findByUserIdAndResourceTypeAndUsageDate(user.getId(), type, day);
        if (existing.isPresent())
            return new WindowUsage(existing.get().getUsedCount(), existing.get().getUsedBytes());

        LocalDateTime from = day.atStartOfDay();
        LocalDateTime to = day.plusDays(1).atStartOfDay().minusNanos(1);
        WindowUsage baseline = databaseUsage(user.getId(), type, from, to);
        ResourceUsageCounter counter = new ResourceUsageCounter();
        counter.setUser(user);
        counter.setResourceType(type);
        counter.setUsageDate(day);
        counter.setUsedCount(baseline.count);
        counter.setUsedBytes(baseline.bytes);
        usageCounterRepository.save(counter);
        return baseline;
    }

    private WindowUsage databaseUsage(Long userId, ResourceLimitType type, LocalDateTime from, LocalDateTime to) {
        return switch (type) {
            case IMAGE -> new WindowUsage(
                    imageRepository.countByUploadTimeBetweenAndUploaderId(from, to, userId),
                    number(imageRepository.sumStorageByUploaderIdInRange(userId, from, to)));
            case FILE -> new WindowUsage(
                    fileRepository.countByUploaderIdInRange(userId, from, to),
                    number(fileRepository.sumStorageByUploaderIdInRange(userId, from, to)));
            case PASTE -> new WindowUsage(
                    pasteRepository.countByCreatedAtBetweenAndCreatedById(from, to, userId),
                    number(pasteRepository.sumContentBytesByUserInRange(userId, from, to)));
            case URL -> new WindowUsage(urlRepository.countByUserInRange(userId, from, to), 0);
            case TEMP_MAIL -> new WindowUsage(tempMailRepository.countByUserInRange(userId, from, to), 0);
            case TOTAL -> throw new IllegalArgumentException("TOTAL usage is derived from resource counters");
        };
    }

    private long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private record WindowUsage(long count, long bytes) {}

    private static final class MutableRule {
        private Long dailyCount;
        private Long weeklyCount;
        private Long dailyBytes;
        private Long weeklyBytes;

        private RuleValues toValues() {
            return new RuleValues(dailyCount, weeklyCount, dailyBytes, weeklyBytes);
        }
    }
}
