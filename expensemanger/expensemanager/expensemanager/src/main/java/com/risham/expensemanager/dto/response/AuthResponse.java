package com.risham.expensemanager.dto.response;

import com.risham.expensemanager.enums.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;
    private Long id;
    private String name;
    private String email;
    private Role role;
}