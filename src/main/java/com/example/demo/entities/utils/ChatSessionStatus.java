package com.example.demo.entities.utils;

public enum ChatSessionStatus {
    ACTIVE,   // Session is ongoing
    CLOSED,   // User or system explicitly closed it
    EXPIRED   // Closed by TTL/inactivity (scheduled job)
}