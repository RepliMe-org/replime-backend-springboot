package com.example.demo.entities.utils;

public enum MessageStatus {
    SENT,           // USER message received, forwarded to FastAPI
    PROCESSING,     // FastAPI is generating AI response (bot-side placeholder)
    DELIVERED,      // BOT message returned and stored
    FAILED,         // AI call failed
    CLASSIFIED      // Background classification completed
}