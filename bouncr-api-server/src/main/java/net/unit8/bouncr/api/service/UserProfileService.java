package net.unit8.bouncr.api.service;

import kotowari.restful.data.Problem;
import net.unit8.bouncr.entity.User;
import net.unit8.bouncr.entity.UserProfileField;
import net.unit8.bouncr.entity.UserProfileValue;
import net.unit8.bouncr.entity.UserProfileVerification;
import net.unit8.bouncr.util.RandomUtils;

import javax.persistence.EntityManager;
import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static enkan.util.BeanBuilder.builder;

public class UserProfileService {
    private EntityManager em;

    public UserProfileService(EntityManager em) {
        this.em = em;
    }

    private List<UserProfileField> findUserProfileFields() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UserProfileField> query = cb.createQuery(UserProfileField.class);
        Root<UserProfileField> userProfileFieldRoot = query.from(UserProfileField.class);
        return em.createQuery(query).getResultList();
    }

    public List<UserProfileVerification> findUserProfileVerifications(String account) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UserProfileVerification> query = cb.createQuery(UserProfileVerification.class);
        Root<UserProfileVerification> root = query.from(UserProfileVerification.class);
        Join<UserProfileVerification, User> userRoot = root.join("user");
        Join<UserProfileField, User> userProfileFieldRoot = root.join("userProfileField");
        query.where(cb.equal(userRoot.get("account"), account));

        return em.createQuery(query)
                .getResultList();
    }

    public Set<Problem.Violation> validateUserProfile(Map<String, Object> userProfiles) {
        List<UserProfileField> userProfileFields = findUserProfileFields();
        return userProfileFields.stream().flatMap(field -> {
            Object value = userProfiles.get(field.getJsonName());
            if (value == null) {
                return field.isRequired()
                        ? Stream.of(new Problem.Violation(field.getJsonName(), "is required"))
                        : Stream.of();
            }

            List<Problem.Violation> fieldViolations = new ArrayList<>();

            Optional.ofNullable(field.getMaxLength())
                    .filter(maxLength -> Objects.toString(value).length() > maxLength)
                    .map(maxLength -> new Problem.Violation(field.getJsonName(), "" + maxLength))
                    .ifPresent(fieldViolations::add);

            Optional.ofNullable(field.getMinLength())
                    .filter(minLength -> Objects.toString(value).length() < minLength)
                    .map(minLength -> new Problem.Violation(field.getJsonName(), "" + minLength))
                    .ifPresent(fieldViolations::add);

            Optional.ofNullable(field.getRegularExpression())
                    .filter(pattern -> !Pattern.compile(pattern).matcher(Objects.toString(value)).matches())
                    .map(pattern -> new Problem.Violation(field.getJsonName(), "" + pattern))
                    .ifPresent(fieldViolations::add);

            return fieldViolations.stream();
        }).collect(Collectors.toSet());
    }
    public Set<Problem.Violation> validateProfileUniqueness(Map<String, Object> userProfiles) {
        return validateProfileUniqueness(userProfiles, null);
    }

    public Set<Problem.Violation> validateProfileUniqueness(Map<String, Object> userProfiles, User user) {
        List<UserProfileField> userProfileFields = findUserProfileFields();
        return userProfileFields.stream()
                .filter(field -> {
                    if (!field.isIdentity()) return false;
                    CriteriaBuilder cb = em.getCriteriaBuilder();
                    CriteriaQuery<Long> query = cb.createQuery(Long.class);
                    query.select(cb.count(cb.literal("1")));
                    Root<UserProfileValue> root = query.from(UserProfileValue.class);
                    List<Predicate> predicates = new ArrayList<>();
                    predicates.add(cb.equal(root.get("value"), userProfiles.get(field.getJsonName())));
                    if (user != null) {
                        predicates.add(cb.notEqual(root.get("user"), user));
                    }
                    query.where(predicates.toArray(Predicate[]::new));
                    Long cnt = em.createQuery(query).getSingleResult();
                    return (cnt > 0);
                })
                .map(f -> new Problem.Violation(f.getJsonName(), "conflicts"))
                .collect(Collectors.toSet());
    }

    public Set<Problem.Violation> validateAccountUniqueness(String account) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        query.select(cb.count(cb.literal("1")));
        Root<User> root = query.from(User.class);
        query.where(cb.equal(root.get("accountLower"), account.toLowerCase(Locale.US)));
        Long cnt = em.createQuery(query).getSingleResult();
        if (cnt > 0) {
            return new HashSet<>(Collections.singletonList(new Problem.Violation<>("account", "conflicts")));
        }
        return new HashSet<>();
    }

    public List<UserProfileValue> convertToUserProfileValues(Map<String, Object> userProfiles) {
        List<UserProfileField> userProfileFields = findUserProfileFields();
        return userProfiles
                .entrySet()
                .stream()
                .map(e -> {
                    UserProfileField field =  userProfileFields.stream()
                            .filter(f -> f.getJsonName().equals(e.getKey()))
                            .findAny()
                            .orElse(null);
                    if (field == null) return null;
                    return builder(new UserProfileValue())
                            .set(UserProfileValue::setUserProfileField, field)
                            .set(UserProfileValue::setValue, Objects.toString(e.getValue(), null))
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<UserProfileVerification> createProfileVerification(Stream<UserProfileValue> userProfileValueStream) {
        return userProfileValueStream.filter(value -> value.getUserProfileField().isNeedsVerification())
                .map(value -> builder(new UserProfileVerification())
                        .set(UserProfileVerification::setUserProfileField, value.getUserProfileField())
                        .set(UserProfileVerification::setCode, RandomUtils.generateRandomString(6))
                        .set(UserProfileVerification::setExpiresAt, LocalDateTime.now().plusDays(1))
                        .build())
                .collect(Collectors.toList());
    }

    public List<UserProfileVerification> createProfileVerification(List<UserProfileValue> userProfileValues) {
        return createProfileVerification(userProfileValues.stream());
    }
}
