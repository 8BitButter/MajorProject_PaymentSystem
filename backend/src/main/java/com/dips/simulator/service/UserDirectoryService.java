package com.dips.simulator.service;

import com.dips.simulator.domain.UserEntity;
import com.dips.simulator.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserDirectoryService {

    private final UserRepository userRepository;

    public UserDirectoryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<UserEntity> findByVpa(String vpa) {
        return userRepository.findByVpa(vpa);
    }

    public UserEntity requireByVpa(String vpa) {
        return userRepository.findByVpa(vpa)
                .orElseThrow(() -> new DomainException("User not found for VPA: " + vpa));
    }

    public boolean verifyMpin(String vpa, String mpin) {
        return userRepository.findByVpa(vpa)
                .map(user -> user.getMpin().equals(mpin))
                .orElse(false);
    }
}

