package com.lab8.chat.server;

import com.lab8.chat.rmi.ChatServerImpl;
import com.lab8.common.NetworkUtils;
import com.lab8.common.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ChatServerWindow extends JFrame {
    private final JComboBox<String> ipComboBox;
    private final JTextField portField;
    private final JTextField serverIdField;
    private final JButton startButton;
    private final JButton stopButton;
    private final JLabel statusLabel;

    // Liên kết Peer Server (Máy thực 2)
    private final JTextField peerHostField;
    private final JTextField peerPortField;
    private final JButton linkPeerButton;

    private final DefaultListModel<String> userListModel;
    private final JList<String> userList;
    private final JTextArea logArea;

    private Registry registry;
    private ChatServerImpl serverImpl;
    private boolean isRunning = false;

    public ChatServerWindow() {
        super("RMI Chat Server - Lab 8");
        UITheme.applyTheme(true);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 600);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // --- TOP PANEL: Cấu hình Server ---
        JPanel topConfigPanel = new JPanel(new GridLayout(2, 1, 6, 6));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row1.add(new JLabel("Server ID:"));
        serverIdField = new JTextField("Server-1", 8);
        row1.add(serverIdField);

        row1.add(new JLabel("IP Server:"));
        List<String> ips = NetworkUtils.getAvailableIpv4Addresses();
        ipComboBox = new JComboBox<>(ips.toArray(new String[0]));
        ipComboBox.setSelectedItem(NetworkUtils.getPreferredLocalIp());
        ipComboBox.setEditable(true);
        row1.add(ipComboBox);

        row1.add(new JLabel("Port:"));
        portField = new JTextField("1099", 5);
        row1.add(portField);

        startButton = new JButton("▶ Khởi Động Server");
        startButton.setBackground(UITheme.getSuccessColor());
        startButton.setForeground(Color.WHITE);
        row1.add(startButton);

        stopButton = new JButton("⏹ Dừng Server");
        stopButton.setBackground(UITheme.getDangerColor());
        stopButton.setForeground(Color.WHITE);
        stopButton.setEnabled(false);
        row1.add(stopButton);

        statusLabel = new JLabel("🔴 Server đang tắt");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        row1.add(statusLabel);

        // Row 2: Liên kết cụm 2 máy thực (Cluster Federation)
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row2.add(new JLabel("🌐 Liên kết Server Máy thực 2:"));
        row2.add(new JLabel("IP Máy 2:"));
        peerHostField = new JTextField("192.168.1.100", 12);
        row2.add(peerHostField);
        row2.add(new JLabel("Port:"));
        peerPortField = new JTextField("1099", 5);
        row2.add(peerPortField);

        linkPeerButton = new JButton("🔗 Kết Nối Liên Server");
        linkPeerButton.setEnabled(false);
        row2.add(linkPeerButton);

        topConfigPanel.add(row1);
        topConfigPanel.add(row2);
        mainPanel.add(topConfigPanel, BorderLayout.NORTH);

        // --- CENTER: SplitPane Danh sách Users & Nhật ký Logs ---
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setBorder(BorderFactory.createTitledBorder("👤 Online Clients (0)"));
        userScrollPane.setPreferredSize(new Dimension(200, 0));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(new Color(25, 25, 28));
        logArea.setForeground(new Color(220, 220, 220));
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("📜 Nhật Ký Hoạt Động (Server Logs)"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, userScrollPane, logScrollPane);
        splitPane.setDividerLocation(220);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // --- BOTTOM: Nút Clear Log và thông tin hướng dẫn ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JLabel guideLabel = new JLabel("💡 Mẹo: Chạy trên 2 máy thực bằng cách đổi Server ID (Server-1, Server-2) và bấm 'Kết Nối Liên Server'.");
        guideLabel.setForeground(Color.GRAY);
        bottomPanel.add(guideLabel, BorderLayout.WEST);

        JButton clearLogBtn = new JButton("Xóa Log");
        clearLogBtn.addActionListener(e -> logArea.setText(""));
        bottomPanel.add(clearLogBtn, BorderLayout.EAST);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Action listeners
        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());
        linkPeerButton.addActionListener(e -> linkPeerServer());
    }

    private void logToWindow(String message) {
        SwingUtilities.invokeLater(() -> {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            logArea.append("[" + time + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void startServer() {
        try {
            String chosenIp = (String) ipComboBox.getSelectedItem();
            if (chosenIp == null || chosenIp.trim().isEmpty()) {
                chosenIp = "127.0.0.1";
            }
            int port = Integer.parseInt(portField.getText().trim());
            String serverId = serverIdField.getText().trim();

            // Rất quan trọng cho giao tiếp giữa máy thực và máy ảo
            NetworkUtils.setRmiHostname(chosenIp);

            try {
                registry = LocateRegistry.createRegistry(port);
                logToWindow("Đã tạo mới RMI Registry tại cổng " + port);
            } catch (Exception ex) {
                registry = LocateRegistry.getRegistry(port);
                logToWindow("Sử dụng RMI Registry hiện có tại cổng " + port);
            }

            serverImpl = new ChatServerImpl(serverId, 0);
            serverImpl.addLogListener(this::logToWindow);
            serverImpl.addUserListChangeListener(this::refreshUserList);

            registry.rebind("ChatServer", serverImpl);

            isRunning = true;
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            linkPeerButton.setEnabled(true);
            ipComboBox.setEnabled(false);
            portField.setEnabled(false);
            serverIdField.setEnabled(false);

            statusLabel.setText("🟢 Đang chạy trên " + chosenIp + ":" + port);
            statusLabel.setForeground(UITheme.getSuccessColor());
            logToWindow("✅ Chat Server [" + serverId + "] đã sẵn sàng phục vụ tại rmi://" + chosenIp + ":" + port + "/ChatServer");

        } catch (Exception ex) {
            logToWindow("❌ Lỗi khi khởi động Server: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Không thể khởi động Server: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopServer() {
        try {
            if (registry != null && serverImpl != null) {
                registry.unbind("ChatServer");
                UnicastRemoteObject.unexportObject(serverImpl, true);
            }
            isRunning = false;
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            linkPeerButton.setEnabled(false);
            ipComboBox.setEnabled(true);
            portField.setEnabled(true);
            serverIdField.setEnabled(true);

            userListModel.clear();
            statusLabel.setText("🔴 Server đang tắt");
            statusLabel.setForeground(Color.LIGHT_GRAY);
            logToWindow("🛑 Đã dừng Chat Server.");
        } catch (Exception ex) {
            logToWindow("Lỗi khi dừng server: " + ex.getMessage());
        }
    }

    private void linkPeerServer() {
        if (!isRunning || serverImpl == null) return;
        String peerHost = peerHostField.getText().trim();
        int peerPort = Integer.parseInt(peerPortField.getText().trim());

        new Thread(() -> {
            try {
                logToWindow("Đang kết nối tới Peer Server " + peerHost + ":" + peerPort + "...");
                boolean ok = serverImpl.linkPeerServer(peerHost, peerPort);
                if (ok) {
                    JOptionPane.showMessageDialog(this, "Kết nối thành công với Peer Server!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Không thể kết nối tới Peer Server tại " + peerHost + ":" + peerPort, "Thất bại", JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception ex) {
                logToWindow("Lỗi kết nối Peer: " + ex.getMessage());
            }
        }).start();
    }

    private void refreshUserList() {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            if (serverImpl != null) {
                try {
                    List<String> users = serverImpl.getOnlineUsers();
                    for (String u : users) {
                        userListModel.addElement(u);
                    }
                    ((JComponent) userList.getParent().getParent()).setBorder(
                            BorderFactory.createTitledBorder("👤 Online Clients (" + users.size() + ")")
                    );
                } catch (Exception ignored) {}
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChatServerWindow window = new ChatServerWindow();
            window.setVisible(true);
        });
    }
}
