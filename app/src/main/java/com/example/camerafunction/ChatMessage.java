package com.example.camerafunction;

// A simple data model class for a chat message
public class ChatMessage {
    private String messageText;
    private boolean isSentByUser;

    public ChatMessage(String messageText, boolean isSentByUser) {
        this.messageText = messageText;
        this.isSentByUser = isSentByUser;
    }

    public String getMessageText() {
        return messageText;
    }

    public boolean isSentByUser() {
        return isSentByUser;
    }
}