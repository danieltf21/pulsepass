package com.pulsepass.domain;

import com.pulsepass.domain.enums.RoleName;
import jakarta.persistence.*;

@Entity
@Table(name = "user_roles")
@IdClass(UserRoleId.class)
public class UserRole {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20)
    private RoleName role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    public UserRole() {
    }

    public UserRole(Long userId, RoleName role) {
        this.userId = userId;
        this.role = role;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public RoleName getRole() { return role; }
    public void setRole(RoleName role) { this.role = role; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}