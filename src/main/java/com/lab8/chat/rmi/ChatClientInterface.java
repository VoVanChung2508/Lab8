package com.lab8.chat.rmi;

import com.lab8.chat.model.ChatMessage;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ChatClientInterface extends Remote {

    /**
     * Máy chủ gọi hàm này để gửi tin nhắn đến máy khách (Callback).
     */
    void receiveMessage(ChatMessage message) throws RemoteException;

    /**
     * Máy chủ gọi hàm này để cập nhật danh sách người dùng đang trực tuyến.
     */
    void updateUserList(List<String> onlineUsers) throws RemoteException;

    /**
     * Lấy tên đăng nhập của máy khách.
     */
    String getUsername() throws RemoteException;

    /**
     * Kiểm tra kết nối từ máy chủ.
     */
    void ping() throws RemoteException;
}
