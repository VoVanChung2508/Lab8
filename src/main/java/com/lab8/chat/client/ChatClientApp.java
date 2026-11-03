package com.lab8.chat.client;

import com.lab8.chat.model.ChatMessage;
import com.lab8.chat.rmi.ChatClientImpl;
import com.lab8.chat.rmi.ChatServerInterface;
import com.lab8.common.NetworkUtils;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class ChatClientApp {

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("--cli")) {
            // Chế độ CLI
            String serverHost = args.length > 1 ? args[1] : "127.0.0.1";
            int serverPort = args.length > 2 ? Integer.parseInt(args[2]) : 1099;
            String username = args.length > 3 ? args[3] : "CLI_User_" + (int)(Math.random() * 1000);
            String localIp = args.length > 4 ? args[4] : NetworkUtils.getPreferredLocalIp();

            System.out.println("=== RMI CHAT CLIENT (CLI MODE) ===");
            System.out.println("Kết nối tới: " + serverHost + ":" + serverPort + " với tên: " + username);

            NetworkUtils.setRmiHostname(localIp);

            try {
                Registry registry = LocateRegistry.getRegistry(serverHost, serverPort);
                ChatServerInterface server = (ChatServerInterface) registry.lookup("ChatServer");

                ChatClientImpl clientCallback = new ChatClientImpl(username, 0);
                clientCallback.setMessageListener(msg -> System.out.println(msg.toString()));
                clientCallback.setUserListListener(users -> System.out.println("👥 Người dùng online: " + users));

                boolean ok = server.registerClient(clientCallback, username);
                if (!ok) {
                    System.err.println("Tên người dùng đã tồn tại. Thoát.");
                    System.exit(1);
                }

                System.out.println("Đăng ký thành công! Nhập tin nhắn và Enter để gửi. Cú pháp chat riêng: /w <user> <noidung>. Gõ /exit để thoát.");

                Scanner scanner = new Scanner(System.in);
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    if (line.equalsIgnoreCase("/exit")) {
                        server.unregisterClient(username);
                        break;
                    }
                    if (line.startsWith("/w ")) {
                        String[] parts = line.split(" ", 3);
                        if (parts.length >= 3) {
                            server.sendPrivateMessage(ChatMessage.privateMessage(username, parts[1], parts[2]));
                        } else {
                            System.out.println("Cú pháp: /w <user> <noidung>");
                        }
                    } else if (!line.isEmpty()) {
                        server.broadcastMessage(ChatMessage.publicMessage(username, line));
                    }
                }

                System.exit(0);

            } catch (Exception e) {
                System.err.println("Lỗi Chat Client: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Chế độ đồ họa GUI
            ChatClientWindow.main(args);
        }
    }
}
