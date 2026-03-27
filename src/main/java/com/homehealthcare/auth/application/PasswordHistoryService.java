package com.homehealthcare.auth.application;

import com.homehealthcare.auth.domain.UserPasswordHistory;
import com.homehealthcare.auth.domain.UserPasswordHistoryRepository;
import com.homehealthcare.user.domain.User;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordHistoryService {

    private final UserPasswordHistoryRepository userPasswordHistoryRepository;
    private final PasswordPolicyProperties passwordPolicyProperties;

    public List<String> recentPasswordHashes(User user) {
        List<String> hashes = new ArrayList<>();
        if (user.hasPasswordHash()) {
            hashes.add(user.getPasswordHash());
        }

        if (passwordPolicyProperties.getPreventReuseCount() <= 0) {
            return hashes;
        }

        userPasswordHistoryRepository.findByUser_IdOrderByRecordedAtDesc(
                        user.getId(),
                        PageRequest.of(0, passwordPolicyProperties.getPreventReuseCount()))
                .stream()
                .map(UserPasswordHistory::getPasswordHash)
                .filter(hash -> !hashes.contains(hash))
                .forEach(hashes::add);
        return hashes;
    }

    public void recordPassword(User user, String passwordHash) {
        userPasswordHistoryRepository.save(UserPasswordHistory.record(user, passwordHash, OffsetDateTime.now()));
    }
}
