package com.dips.simulator.dto;

import com.dips.simulator.domain.enums.LoadProfile;
import jakarta.validation.constraints.NotNull;

public class LoadProfileRequest {

    @NotNull
    private LoadProfile profile;

    public LoadProfile getProfile() {
        return profile;
    }

    public void setProfile(LoadProfile profile) {
        this.profile = profile;
    }
}

