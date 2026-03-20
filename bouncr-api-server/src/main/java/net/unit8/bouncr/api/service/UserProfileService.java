package net.unit8.bouncr.api.service;

import kotowari.restful.data.Problem;
import net.unit8.bouncr.api.repository.UserProfileFieldRepository;
import net.unit8.bouncr.data.UserProfileField;
import org.jooq.DSLContext;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jooq.impl.DSL.*;

public class UserProfileService {
    private final DSLContext dsl;

    public UserProfileService(DSLContext dsl) {
        this.dsl = dsl;
    }

    private List<UserProfileField> findUserProfileFields() {
        return new UserProfileFieldRepository(dsl).findAll();
    }

    public Set<Problem.Violation> validateUserProfile(Map<String, Object> userProfiles) {
        List<UserProfileField> userProfileFields = findUserProfileFields();
        return userProfileFields.stream().flatMap(f -> {
            Object value = userProfiles.get(f.jsonName());
            if (value == null) {
                return f.isRequired()
                        ? Stream.of(new Problem.Violation(f.jsonName(), "is required"))
                        : Stream.of();
            }

            List<Problem.Violation> fieldViolations = new ArrayList<>();

            Optional.ofNullable(f.maxLength())
                    .filter(maxLength -> Objects.toString(value).length() > maxLength)
                    .map(maxLength -> new Problem.Violation(f.jsonName(), "" + maxLength))
                    .ifPresent(fieldViolations::add);

            Optional.ofNullable(f.minLength())
                    .filter(minLength -> Objects.toString(value).length() < minLength)
                    .map(minLength -> new Problem.Violation(f.jsonName(), "" + minLength))
                    .ifPresent(fieldViolations::add);

            Optional.ofNullable(f.regularExpression())
                    .filter(pattern -> !Pattern.compile(pattern).matcher(Objects.toString(value)).matches())
                    .map(pattern -> new Problem.Violation(f.jsonName(), "" + pattern))
                    .ifPresent(fieldViolations::add);

            return fieldViolations.stream();
        }).collect(Collectors.toSet());
    }

    public Set<Problem.Violation> validateProfileUniqueness(Map<String, Object> userProfiles) {
        return validateProfileUniqueness(userProfiles, null);
    }

    public Set<Problem.Violation> validateProfileUniqueness(Map<String, Object> userProfiles, Long excludeUserId) {
        List<UserProfileField> userProfileFields = findUserProfileFields();
        return userProfileFields.stream()
                .filter(f -> {
                    if (!f.isIdentity()) return false;
                    Object profileValue = userProfiles.get(f.jsonName());
                    if (profileValue == null) return false;

                    var condition = field("upv.\"value\"").eq(profileValue.toString())
                            .and(field("upv.user_profile_field_id").eq(f.id()));
                    if (excludeUserId != null) {
                        condition = condition.and(field("upv.user_id").ne(excludeUserId));
                    }

                    int cnt = dsl.selectCount()
                            .from(table("user_profile_values").as("upv"))
                            .where(condition)
                            .fetchOne(0, int.class);
                    return cnt > 0;
                })
                .map(f -> new Problem.Violation(f.jsonName(), "conflicts"))
                .collect(Collectors.toSet());
    }

    public Set<Problem.Violation> validateAccountUniqueness(String account) {
        int cnt = dsl.selectCount()
                .from(table("users"))
                .where(field("account_lower").eq(account.toLowerCase(Locale.US)))
                .fetchOne(0, int.class);
        if (cnt > 0) {
            return new HashSet<>(Collections.singletonList(new Problem.Violation("account", "conflicts")));
        }
        return new HashSet<>();
    }
}
