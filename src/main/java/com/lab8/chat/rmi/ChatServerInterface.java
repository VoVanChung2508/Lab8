package com.lab8.chat.rmi;

import com.lab8.chat.model.ChatMessage;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ChatServerInterface extends Remote {

    /**
     * Đăng ký máy khách mới với máy chủ.
     * Trả về true nếu đăng ký thành công, false nếu tên đã tồn tại.
     */
    boolean registerClient(ChatClientInterface client, String username) throws RemoteException;

    /**
     * Hủy đăng ký khi máy khách rời phòng chat.
     */
    void unregisterClient(String username) throws RemoteException;

    /**
     * Phát tin nhắn tới tất cả người dùng trong phòng chat.
     */
    void broadcastMessage(ChatMessage message) throws RemoteException;

    /**
     * Gửi tin nhắn riêng (1-1) tới một người dùng cụ thể.
     */
    void sendPrivateMessage(ChatMessage message) throws RemoteException;

    /**
     * Lấy danh sách tên người dùng đang trực tuyến.
     */
    List<String> getOnlineUsers() throws RemoteException;

    /**
     * Liên kết với một máy chủ khác (Server 2 trên máy thực 2) để tạo cụm liên máy chủ (Cluster).
     */
    boolean linkPeerServer(String peerHost, int peerPort) throws RemoteException;

    /**
     * Nhận tin nhắn được chuyển tiếp từ máy chủ đối tác (chống gửi lặp lại).
     */
    void forwardFromPeerServer(ChatMessage message, String originServerId) throws RemoteException;

    /**
     * Lấy ID của server.
     */
    String getServerId() throws RemoteException;
}
