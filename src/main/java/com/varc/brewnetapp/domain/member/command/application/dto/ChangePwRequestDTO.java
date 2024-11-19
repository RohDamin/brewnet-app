package com.varc.brewnetapp.domain.member.command.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ChangePwRequestDTO {
    private String loginId;
    private String password;
}