package com.lab8.chat.rmi;

import com.lab8.chat.model.ChatMessage;
import com.lab8.chat.model.MessageType;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChatServerImpl extends UnicastRemoteObject implements ChatServerInterface {
    private static final long serialVersionUID = 1L;

    private final String serverId;
    private final Map<String, ChatClientInterface> clients = new ConcurrentHashMap<>();
    private final Map<String, ChatServerInterface> peerServers = new ConcurrentHashMap<>();
    private final List<Consumer<String>> logListeners = new ArrayList<>();
    private final List<Runnable> userListChangeListeners = new ArrayList<>();

    public ChatServerImpl(String serverId, int port) throws RemoteException {
        super(port);
        this.serverId = serverId;
    }

    public void addLogListener(Consumer<String> listener) {
        this.logListeners.add(listener);
    }

    public void addUserListChangeListener(Runnable listener) {
        this.userListChangeListeners.add(listener);
    }

    private void log(String message) {
        System.out.println("[" + serverId + "] " + message);
        for (Consumer<String> l : logListeners) {
            try {
                l.accept(message);
            } catch (Exception ignored) {}
        }
    }

    private void notifyUserListChanged() {
        for (Runnable r : userListChangeListeners) {
            try {
                r.run();
            } catch (Exception ignored) {}
        }
    }

    @Override
    public synchronized boolean registerClient(ChatClientInterface client, String username) throws RemoteException {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        String cleanUsername = username.trim();
        if (clients.containsKey(cleanUsername)) {
            log("Từ chối đăng ký: Tên người dùng '" + cleanUsername + "' đã tồn tại.");
            return false;
        }

        clients.put(cleanUsername, client);
        log("Người dùng mới tham gia: " + cleanUsername + " (Tổng số: " + clients.size() + ")");
        notifyUserListChanged();

        // Gửi thông báo tới các client
        ChatMessage joinMsg = new ChatMessage("HỆ THỐNG", null, cleanUsername + " đã tham gia phòng chat.", MessageType.SYSTEM);
        broadcastMessageInternal(joinMsg, true);
        pushOnlineUserListToAll();

        return true;
    }

    @Override
    public synchronized void unregisterClient(String username) throws RemoteException {
        if (username == null) return;
        ChatClientInterface removed = clients.remove(username);
        if (removed != null) {
            log("Người dùng đã rời phòng: " + username + " (Còn lại: " + clients.size() + ")");
            notifyUserListChanged();

            ChatMessage leaveMsg = new ChatMessage("HỆ THỐNG", null, username + " đã rời phòng chat.", MessageType.SYSTEM);
            broadcastMessageInternal(leaveMsg, true);
            pushOnlineUserListToAll();
        }
    }

    @Override
    public void broadcastMessage(ChatMessage message) throws RemoteException {
        log("Broadcast: " + message);
        broadcastMessageInternal(message, true);
    }

    private void broadcastMessageInternal(ChatMessage message, boolean forwardToPeers) {
        List<String> deadClients = new ArrayList<>();

        for (Map.Entry<String, ChatClientInterface> entry : clients.entrySet()) {
            String uname = entry.getKey();
            ChatClientInterface client = entry.getValue();
            try {
                client.receiveMessage(message);
            } catch (RemoteException e) {
                log("Không thể liên lạc tới client " + uname + ", đánh dấu ngắt kết nối.");
                deadClients.add(uname);
            }
        }

        // Dọn dẹp các client đã mất kết nối
        for (String dead : deadClients) {
            clients.remove(dead);
        }
        if (!deadClients.isEmpty()) {
            notifyUserListChanged();
            pushOnlineUserListToAll();
        }

        // Chuyển tiếp tới server bạn bè (Máy thực 2)
        if (forwardToPeers && !peerServers.isEmpty()) {
            for (Map.Entry<String, ChatServerInterface> peerEntry : peerServers.entrySet()) {
                try {
                    peerEntry.getValue().forwardFromPeerServer(message, this.serverId);
                } catch (RemoteException e) {
                    log("Lỗi chuyển tiếp tới Peer Server " + peerEntry.getKey() + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void sendPrivateMessage(ChatMessage message) throws RemoteException {
        String recipient = message.getRecipient();
        String sender = message.getSender();
        log("Private Message: " + sender + " -> " + recipient + ": " + message.getContent());

        ChatClientInterface destClient = clients.get(recipient);
        boolean deliveredLocally = false;

        if (destClient != null) {
            try {
                destClient.receiveMessage(message);
                deliveredLocally = true;
            } catch (RemoteException e) {
                log("Lỗi gửi tin riêng tới " + recipient + ", hủy đăng ký.");
                clients.remove(recipient);
                notifyUserListChanged();
                pushOnlineUserListToAll();
            }
        }

        // Gửi lại cho chính người gửi để hiển thị trên giao diện người gửi
        ChatClientInterface senderClient = clients.get(sender);
        if (senderClient != null) {
            try {
                senderClient.receiveMessage(message);
            } catch (RemoteException ignored) {}
        }

        // Nếu người nhận không nằm ở server này, chuyển tiếp sang Peer Server
        if (!deliveredLocally && !peerServers.isEmpty()) {
            for (Map.Entry<String, ChatServerInterface> peerEntry : peerServers.entrySet()) {
                try {
                    peerEntry.getValue().forwardFromPeerServer(message, this.serverId);
                } catch (RemoteException e) {
                    log("Lỗi chuyển tiếp tin riêng tới Peer: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void forwardFromPeerServer(ChatMessage message, String originServerId) throws RemoteException {
        // Tránh vòng lặp
        if (this.serverId.equals(originServerId)) {
            return;
        }

        log("Nhận tin chuyển tiếp từ [" + originServerId + "]: " + message.getContent());

        if (message.getType() == MessageType.PRIVATE) {
            ChatClientInterface dest = clients.get(message.getRecipient());
            if (dest != null) {
                try {
                    dest.receiveMessage(message);
                } catch (RemoteException e) {
                    clients.remove(message.getRecipient());
                }
            }
        } else {
            // Tin nhắn chung -> gửi tới tất cả client cục bộ
            broadcastMessageInternal(message, false);
        }
    }

    @Override
    public boolean linkPeerServer(String peerHost, int peerPort) throws RemoteException {
        try {
            log("Đang kết nối tới Peer Server tại " + peerHost + ":" + peerPort + "...");
            Registry registry = LocateRegistry.getRegistry(peerHost, peerPort);
            ChatServerInterface peer = (ChatServerInterface) registry.lookup("ChatServer");
            String peerId = peer.getServerId();
            peerServers.put(peerId, peer);
            log("Đã kết nối thành công với Peer Server: " + peerId);
            return true;
        } catch (Exception e) {
            log("Thất bại khi kết nối tới Peer Server (" + peerHost + ":" + peerPort + "): " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<String> getOnlineUsers() throws RemoteException {
        return new ArrayList<>(clients.keySet());
    }

    @Override
    public String getServerId() throws RemoteException {
        return this.serverId;
    }

    public void pushOnlineUserListToAll() {
        List<String> userList = new ArrayList<>(clients.keySet());
        for (Map.Entry<String, ChatClientInterface> entry : clients.entrySet()) {
            try {
                entry.getValue().updateUserList(userList);
            } catch (RemoteException ignored) {}
        }
    }

    public int getClientCount() {
        return clients.size();
    }
}
