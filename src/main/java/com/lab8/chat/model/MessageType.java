package com.lab8.chat.model;

import java.io.Serializable;

public enum MessageType implements Serializable {
    PUBLIC,      // Tin nhắn gửi chung tới tất cả mọi người
    PRIVATE,     // Tin nhắn riêng tư giữa 2 người (1-1)
    SYSTEM,      // Thông báo từ hệ thống
    USER_JOIN,   // Người dùng mới tham gia
    USER_LEAVE   // Người dùng rời phòng
}
