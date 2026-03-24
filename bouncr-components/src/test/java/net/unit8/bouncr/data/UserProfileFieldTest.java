package net.unit8.bouncr.data;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileFieldTest {

    @Test
    void construction_allFieldsAccessible() {
        UserProfileField field = new UserProfileField(
                1L, "email", "email",
                true, true,
                "^[^@]+@[^@]+$", 255, 1,
                true, 0);

        assertThat(field.id()).isEqualTo(1L);
        assertThat(field.name()).isEqualTo("email");
        assertThat(field.jsonName()).isEqualTo("email");
        assertThat(field.isRequired()).isTrue();
        assertThat(field.isIdentity()).isTrue();
        assertThat(field.regularExpression()).isEqualTo("^[^@]+@[^@]+$");
        assertThat(field.maxLength()).isEqualTo(255);
        assertThat(field.minLength()).isEqualTo(1);
        assertThat(field.needsVerification()).isTrue();
        assertThat(field.position()).isEqualTo(0);
    }

    @Test
    void optionalFieldsCanBeNull() {
        UserProfileField field = new UserProfileField(
                null, "nickname", "nickname",
                false, false,
                null, null, null,
                false, 5);

        assertThat(field.id()).isNull();
        assertThat(field.regularExpression()).isNull();
        assertThat(field.maxLength()).isNull();
        assertThat(field.minLength()).isNull();
    }

    @Test
    void isRequired_falseByDefault() {
        UserProfileField field = new UserProfileField(
                2L, "bio", "bio",
                false, false,
                null, 500, null,
                false, 10);

        assertThat(field.isRequired()).isFalse();
        assertThat(field.isIdentity()).isFalse();
        assertThat(field.needsVerification()).isFalse();
    }

    @Test
    void equality_sameValuesMeansEqual() {
        UserProfileField a = new UserProfileField(
                1L, "email", "email",
                true, true,
                null, 255, 1,
                true, 0);
        UserProfileField b = new UserProfileField(
                1L, "email", "email",
                true, true,
                null, 255, 1,
                true, 0);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equality_differentValuesMeansNotEqual() {
        UserProfileField a = new UserProfileField(
                1L, "email", "email",
                true, true,
                null, 255, 1,
                true, 0);
        UserProfileField b = new UserProfileField(
                2L, "phone", "phone",
                false, false,
                null, 20, null,
                false, 1);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void regularExpression_canBeUsedForValidation() {
        UserProfileField field = new UserProfileField(
                3L, "zipcode", "zip_code",
                true, false,
                "^\\d{3}-\\d{4}$", 8, 8,
                false, 2);

        assertThat("123-4567".matches(field.regularExpression())).isTrue();
        assertThat("abcdefgh".matches(field.regularExpression())).isFalse();
    }
}
