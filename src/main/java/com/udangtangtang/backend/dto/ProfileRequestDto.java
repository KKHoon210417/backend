package com.udangtangtang.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileRequestDto {
    private String nowPassword;
    private String newPassword;
    private String userProfileIntro;
}
