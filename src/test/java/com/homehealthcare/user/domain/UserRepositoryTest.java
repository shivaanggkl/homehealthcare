package com.homehealthcare.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesUserWithRequiredFieldsAndAuditing() {
        User user = userRepository.saveAndFlush(
                User.invite("Alicia", "Owner", "alicia.owner@northstar.example", "+1 312 555 0101"));

        assertThat(user.getId()).isNotNull();
        assertThat(user.getStatus()).isEqualTo(UserStatus.INVITED);
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(user.getLastLoginAt()).isNull();
        assertThat(user.getDeactivatedAt()).isNull();
    }

    @Test
    void enforcesGlobalUniqueEmail() {
        userRepository.saveAndFlush(
                User.invite("Alicia", "Owner", "alicia.owner@northstar.example", "+1 312 555 0101"));

        User duplicate = User.invite("Alicia", "Scheduler", "alicia.owner@northstar.example", "+1 312 555 0102");

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void normalizesEmailAndOptionalPhoneBeforePersist() {
        User user = userRepository.saveAndFlush(
                User.invite(" Alicia ", " Owner ", " ALICIA.OWNER@NORTHSTAR.EXAMPLE ", "   "));

        assertThat(user.getFirstName()).isEqualTo("Alicia");
        assertThat(user.getLastName()).isEqualTo("Owner");
        assertThat(user.getEmail()).isEqualTo("alicia.owner@northstar.example");
        assertThat(user.getPhone()).isNull();
    }

    @Test
    void supportsAllRequiredStatuses() {
        User user = userRepository.saveAndFlush(
                User.invite("Alicia", "Owner", "alicia.owner@northstar.example", "+1 312 555 0101"));

        user.activate();
        assertThat(userRepository.saveAndFlush(user).getStatus()).isEqualTo(UserStatus.ACTIVE);

        user.lock();
        assertThat(userRepository.saveAndFlush(user).getStatus()).isEqualTo(UserStatus.LOCKED);

        user.suspend();
        assertThat(userRepository.saveAndFlush(user).getStatus()).isEqualTo(UserStatus.SUSPENDED);

        user.deactivate();
        User deactivated = userRepository.saveAndFlush(user);
        assertThat(deactivated.getStatus()).isEqualTo(UserStatus.DEACTIVATED);
        assertThat(deactivated.isDeactivated()).isTrue();
        assertThat(deactivated.getDeactivatedAt()).isNotNull();
    }

    @Test
    void recordsLastLoginTimestamp() {
        User user = userRepository.saveAndFlush(
                User.invite("Alicia", "Owner", "alicia.owner@northstar.example", "+1 312 555 0101"));
        OffsetDateTime loginTime = OffsetDateTime.now().minusMinutes(5);

        user.recordLogin(loginTime);
        User updated = userRepository.saveAndFlush(user);

        assertThat(updated.getLastLoginAt()).isEqualTo(loginTime);
    }

    @Test
    void supportsSoftDeactivationAndReactivation() {
        User user = userRepository.saveAndFlush(
                User.invite("Alicia", "Owner", "alicia.owner@northstar.example", "+1 312 555 0101"));

        user.deactivate();
        User deactivated = userRepository.saveAndFlush(user);
        assertThat(deactivated.getStatus()).isEqualTo(UserStatus.DEACTIVATED);
        assertThat(deactivated.getDeactivatedAt()).isNotNull();

        deactivated.activate();
        User reactivated = userRepository.saveAndFlush(deactivated);
        assertThat(reactivated.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(reactivated.getDeactivatedAt()).isNull();
        assertThat(reactivated.isDeactivated()).isFalse();
    }

    @Test
    void findsUserByNormalizedEmail() {
        userRepository.saveAndFlush(
                User.invite("Alicia", "Owner", " ALICIA.OWNER@NORTHSTAR.EXAMPLE ", "+1 312 555 0101"));

        assertThat(userRepository.findByEmail("alicia.owner@northstar.example")).isPresent();
        assertThat(userRepository.existsByEmail("alicia.owner@northstar.example")).isTrue();
    }
}
