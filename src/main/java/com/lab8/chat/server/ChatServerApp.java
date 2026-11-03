package com.lab8.chat.server;

import com.lab8.chat.rmi.ChatServerImpl;
import com.lab8.common.NetworkUtils;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ChatServerApp {

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("--cli")) {
            // Chạy chế độ dòng lệnh Console (thích hợp cho máy chủ Linux / máy ảo không có màn hình)
            String serverId = args.length > 1 ? args[1] : "Server-1";
            int port = args.length > 2 ? Integer.parseInt(args[2]) : 1099;
            String hostIp = args.length > 3 ? args[3] : NetworkUtils.getPreferredLocalIp();

            System.out.println("=== KHỞI ĐỘNG CHAT SERVER RMI (CLI MODE) ===");
            NetworkUtils.setRmiHostname(hostIp);

            try {
                Registry registry;
                try {
                    registry = LocateRegistry.createRegistry(port);
                } catch (Exception e) {
                    registry = LocateRegistry.getRegistry(port);
                }

                ChatServerImpl server = new ChatServerImpl(serverId, 0);
                registry.rebind("ChatServer", server);

                System.out.println("✅ Server [" + serverId + "] đã chạy tại rmi://" + hostIp + ":" + port + "/ChatServer");
                System.out.println("Nhấn Ctrl+C để dừng server.");

                // Kết nối Peer Server nếu được truyền qua tham số
                if (args.length > 5 && args[4].equalsIgnoreCase("--peer")) {
                    String[] peerParts = args[5].split(":");
                    server.linkPeerServer(peerParts[0], Integer.parseInt(peerParts[1]));
                }

            } catch (Exception e) {
                System.err.println("Lỗi khởi động Server: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Chạy chế độ đồ họa GUI
            ChatServerWindow.main(args);
        }
    }
}
