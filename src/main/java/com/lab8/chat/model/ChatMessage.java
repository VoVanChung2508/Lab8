package com.lab8.chat.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final String sender;
    private final String recipient; // null hoặc rỗng nếu là tin nhắn PUBLIC / SYSTEM
    private final String content;
    private final MessageType type;
    private final String timeString;

    public ChatMessage(String sender, String recipient, String content, MessageType type) {
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
        this.type = type;
        this.timeString = LocalDateTime.now().format(TIME_FORMATTER);
    }

    public static ChatMessage publicMessage(String sender, String content) {
        return new ChatMessage(sender, null, content, MessageType.PUBLIC);
    }

    public static ChatMessage privateMessage(String sender, String recipient, String content) {
        return new ChatMessage(sender, recipient, content, MessageType.PRIVATE);
    }

    public static ChatMessage systemMessage(String content) {
        return new ChatMessage("HỆ THỐNG", null, content, MessageType.SYSTEM);
    }

    public String getSender() {
        return sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getContent() {
        return content;
    }

    public MessageType getType() {
        return type;
    }

    public String getTimeString() {
        return timeString;
    }

    public boolean isPrivate() {
        return type == MessageType.PRIVATE;
    }

    @Override
    public String toString() {
        if (type == MessageType.SYSTEM) {
            return String.format("[%s] 📢 %s", timeString, content);
        } else if (type == MessageType.PRIVATE) {
            return String.format("[%s] 🔒 (Riêng tư từ %s tới %s): %s", timeString, sender, recipient, content);
        } else {
            return String.format("[%s] %s: %s", timeString, sender, content);
        }
    }
}
