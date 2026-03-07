package com.dips.simulator.service;

import com.dips.simulator.domain.enums.LoadProfile;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

@Service
public class LoadProfileService {

    private final AtomicReference<LoadProfile> profile = new AtomicReference<>(LoadProfile.NORMAL);

    public LoadProfile getProfile() {
        return profile.get();
    }

    public void setProfile(LoadProfile value) {
        profile.set(value);
    }
}

