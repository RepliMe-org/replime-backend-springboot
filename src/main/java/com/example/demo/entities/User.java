package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = true)
    private String password; // nullable for OAuth users

    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    private String providerId; // only for OAuth users

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
}

