package com.pulsepass.domain;

import com.pulsepass.domain.enums.RoleName;

import java.io.Serializable;
import java.util.Objects;

public class UserRoleId implements Serializable {

    private Long userId;
    private RoleName role;

    public UserRoleId() {
    }

    public UserRoleId(Long userId, RoleName role) {
        this.userId = userId;
        this.role = role;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public RoleName getRole() { return role; }
    public void setRole(RoleName role) { this.role = role; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserRoleId that)) return false;
        return Objects.equals(userId, that.userId) && role == that.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, role);
    }
}
