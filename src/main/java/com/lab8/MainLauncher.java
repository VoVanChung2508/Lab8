package com.lab8;

import com.lab8.chat.client.ChatClientApp;
import com.lab8.chat.server.ChatServerApp;
import com.lab8.common.UITheme;
import com.lab8.dbsync.client.SyncClientApp;
import com.lab8.dbsync.monitor.SyncDashboardApp;
import com.lab8.dbsync.server.SyncServerWindow;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MainLauncher extends JFrame {

    public MainLauncher() {
        super("LAB 8: LẬP TRÌNH MẠNG - RMI CHAT & ĐỒNG BỘ 3 CSDL");
        UITheme.applyTheme(true);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(780, 560);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(15, 20, 15, 20));

        // Header Title
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        JLabel titleLabel = new JLabel("LAB 8: RMI CHAT & ĐỒNG BỘ HÓA 3 CSDL", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(UITheme.getPrimaryColor());

        JLabel subtitleLabel = new JLabel("Hỗ trợ chạy phân tán trên Máy thực (Physical PC) và Máy ảo (VMware / VirtualBox)", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        subtitleLabel.setForeground(Color.LIGHT_GRAY);

        headerPanel.add(titleLabel);
        headerPanel.add(subtitleLabel);
        root.add(headerPanel, BorderLayout.NORTH);

        // Center: 2 Big sections: BÀI 1 & BÀI 2
        JPanel centerGrid = new JPanel(new GridLayout(1, 2, 16, 0));

        // === CỘT TRÁI: BÀI 1 - RMI CHAT ===
        JPanel chatBox = new JPanel();
        chatBox.setLayout(new BoxLayout(chatBox, BoxLayout.Y_AXIS));
        chatBox.setBackground(UITheme.getCardBackgroundColor());
        chatBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 70, 90), 1),
                new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel chatTitle = new JLabel("💬 BÀI 1: CHAT BẰNG RMI");
        chatTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        chatTitle.setForeground(new Color(100, 181, 246));
        chatTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        chatBox.add(chatTitle);
        chatBox.add(Box.createRigidArea(new Dimension(0, 8)));

        JLabel chatDesc = new JLabel("<html><center style='color:#b0bec5; font-size:11px;'>" +
                "• Mô hình RMI Callback thời gian thực<br/>" +
                "• Chat phòng (Public) & Chat riêng (1-1)<br/>" +
                "• Hỗ trợ kết nối cụm 2 Server trên 2 máy thực" +
                "</center></html>");
        chatDesc.setAlignmentX(Component.CENTER_ALIGNMENT);
        chatBox.add(chatDesc);
        chatBox.add(Box.createRigidArea(new Dimension(0, 16)));

        JButton runChatServerBtn = new JButton("🚀 Khởi Chạy Chat Server (Máy thực)");
        runChatServerBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        runChatServerBtn.setMaximumSize(new Dimension(280, 42));
        runChatServerBtn.setBackground(UITheme.getPrimaryColor());
        runChatServerBtn.setForeground(Color.WHITE);
        runChatServerBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        runChatServerBtn.addActionListener(e -> new Thread(() -> ChatServerApp.main(new String[0])).start());
        chatBox.add(runChatServerBtn);
        chatBox.add(Box.createRigidArea(new Dimension(0, 12)));

        JButton runChatClientBtn = new JButton("💬 Khởi Chạy Chat Client (Máy ảo)");
        runChatClientBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        runChatClientBtn.setMaximumSize(new Dimension(280, 42));
        runChatClientBtn.setBackground(new Color(76, 175, 80));
        runChatClientBtn.setForeground(Color.WHITE);
        runChatClientBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        runChatClientBtn.addActionListener(e -> new Thread(() -> ChatClientApp.main(new String[0])).start());
        chatBox.add(runChatClientBtn);

        centerGrid.add(chatBox);

        // === CỘT PHẢI: BÀI 2 - ĐỒNG BỘ HÓA 3 CSDL ===
        JPanel dbBox = new JPanel();
        dbBox.setLayout(new BoxLayout(dbBox, BoxLayout.Y_AXIS));
        dbBox.setBackground(UITheme.getCardBackgroundColor());
        dbBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 70, 90), 1),
                new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel dbTitle = new JLabel("🗄️ BÀI 2: ĐỒNG BỘ 3 CSDL RMI");
        dbTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        dbTitle.setForeground(new Color(255, 183, 77));
        dbTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        dbBox.add(dbTitle);
        dbBox.add(Box.createRigidArea(new Dimension(0, 8)));

        JLabel dbDesc = new JLabel("<html><center style='color:#b0bec5; font-size:11px;'>" +
                "• 3 CSDL SQLite độc lập trên 3 Server<br/>" +
                "• Đồng bộ phân tán tức thời (Replication)<br/>" +
                "• Chữa lành tự động khi có node phục hồi (Catch-up)" +
                "</center></html>");
        dbDesc.setAlignmentX(Component.CENTER_ALIGNMENT);
        dbBox.add(dbDesc);
        dbBox.add(Box.createRigidArea(new Dimension(0, 16)));

        JButton runDashboardBtn = new JButton("📊 Mở Bảng Giám Sát Đồng Bộ 3 CSDL");
        runDashboardBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        runDashboardBtn.setMaximumSize(new Dimension(280, 40));
        runDashboardBtn.setBackground(new Color(156, 39, 176));
        runDashboardBtn.setForeground(Color.WHITE);
        runDashboardBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        runDashboardBtn.addActionListener(e -> new Thread(() -> SyncDashboardApp.main(new String[0])).start());
        dbBox.add(runDashboardBtn);
        dbBox.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel nodeBtnsPanel = new JPanel(new GridLayout(1, 3, 4, 0));
        nodeBtnsPanel.setMaximumSize(new Dimension(280, 34));
        nodeBtnsPanel.setOpaque(false);

        JButton n1Btn = new JButton("Node 1");
        n1Btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        n1Btn.addActionListener(e -> new Thread(() -> SyncServerWindow.main(new String[]{"Server-1", "1099", "server1_db.sqlite"})).start());

        JButton n2Btn = new JButton("Node 2");
        n2Btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        n2Btn.addActionListener(e -> new Thread(() -> SyncServerWindow.main(new String[]{"Server-2", "1100", "server2_db.sqlite"})).start());

        JButton n3Btn = new JButton("Node 3");
        n3Btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        n3Btn.addActionListener(e -> new Thread(() -> SyncServerWindow.main(new String[]{"Server-3", "1101", "server3_db.sqlite"})).start());

        nodeBtnsPanel.add(n1Btn);
        nodeBtnsPanel.add(n2Btn);
        nodeBtnsPanel.add(n3Btn);
        dbBox.add(nodeBtnsPanel);
        dbBox.add(Box.createRigidArea(new Dimension(0, 10)));

        JButton runDbClientBtn = new JButton("💻 Khởi Chạy DB Client (Máy ảo)");
        runDbClientBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        runDbClientBtn.setMaximumSize(new Dimension(280, 36));
        runDbClientBtn.addActionListener(e -> new Thread(() -> SyncClientApp.main(new String[0])).start());
        dbBox.add(runDbClientBtn);

        centerGrid.add(dbBox);
        root.add(centerGrid, BorderLayout.CENTER);

        // Footer
        JLabel footerLabel = new JLabel("💡 Mẹo: Có thể chạy nhiều cửa sổ cùng lúc trên 1 máy để test, hoặc xuất JAR chạy qua LAN giữa Máy thực và Máy ảo.", SwingConstants.CENTER);
        footerLabel.setForeground(Color.GRAY);
        footerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        root.add(footerLabel, BorderLayout.SOUTH);

        add(root);
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            String command = args[0].toLowerCase();
            String[] subArgs = new String[args.length - 1];
            System.arraycopy(args, 1, subArgs, 0, subArgs.length);

            switch (command) {
                case "chat-server":
                    ChatServerApp.main(subArgs);
                    return;
                case "chat-client":
                    ChatClientApp.main(subArgs);
                    return;
                case "db-server":
                    com.lab8.dbsync.server.SyncServerApp.main(subArgs);
                    return;
                case "db-client":
                    SyncClientApp.main(subArgs);
                    return;
                case "db-dashboard":
                    SyncDashboardApp.main(subArgs);
                    return;
                default:
                    System.out.println("Tham số không hợp lệ. Khởi động giao diện Launcher mặc định.");
            }
        }

        SwingUtilities.invokeLater(() -> {
            MainLauncher launcher = new MainLauncher();
            launcher.setVisible(true);
        });
    }
}
