package nus.iss.smartcart.backend.dto;

import lombok.Getter;
import lombok.Setter;

//Author: Junior

@Getter
@Setter
public class LoginRequest {

    private String email;
    private String password;
}