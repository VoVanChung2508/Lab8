package com.lab8.chat.rmi;

import com.lab8.chat.model.ChatMessage;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.function.Consumer;

public class ChatClientImpl extends UnicastRemoteObject implements ChatClientInterface {
    private static final long serialVersionUID = 1L;

    private final String username;
    private Consumer<ChatMessage> messageListener;
    private Consumer<List<String>> userListListener;

    public ChatClientImpl(String username, int callbackPort) throws RemoteException {
        super(callbackPort);
        this.username = username;
    }

    public void setMessageListener(Consumer<ChatMessage> listener) {
        this.messageListener = listener;
    }

    public void setUserListListener(Consumer<List<String>> listener) {
        this.userListListener = listener;
    }

    @Override
    public void receiveMessage(ChatMessage message) throws RemoteException {
        if (messageListener != null) {
            messageListener.accept(message);
        }
    }

    @Override
    public void updateUserList(List<String> onlineUsers) throws RemoteException {
        if (userListListener != null) {
            userListListener.accept(onlineUsers);
        }
    }

    @Override
    public String getUsername() throws RemoteException {
        return this.username;
    }

    @Override
    public void ping() throws RemoteException {
        // Heartbeat probe from server
    }
}
