package com.lab8.chat.client;

import com.lab8.chat.model.ChatMessage;
import com.lab8.chat.model.MessageType;
import com.lab8.chat.rmi.ChatClientImpl;
import com.lab8.chat.rmi.ChatServerInterface;
import com.lab8.common.NetworkUtils;
import com.lab8.common.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.StringWriter;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class ChatClientWindow extends JFrame {
    private String username;
    private String serverHost;
    private int serverPort;

    private ChatServerInterface server;
    private ChatClientImpl clientCallback;

    private JTextPane messagePane;
    private HTMLEditorKit htmlKit;
    private HTMLDocument htmlDoc;

    private DefaultListModel<String> userListModel;
    private JList<String> userList;

    private JTextField messageInput;
    private JButton sendButton;
    private JLabel targetLabel;
    private String selectedRecipient = null; // null = gửi tới mọi người

    private JLabel statusLabel;
    private JButton disconnectBtn;

    public ChatClientWindow() {
        super("RMI Chat Client - Lab 8");
        UITheme.applyTheme(true);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(780, 560);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnectAndExit();
            }
        });

        initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- TOP BAR: Header thông tin người dùng và kết nối ---
        JPanel topBar = new JPanel(new BorderLayout(10, 5));
        topBar.setBackground(UITheme.getCardBackgroundColor());
        topBar.setBorder(new EmptyBorder(8, 12, 8, 12));

        statusLabel = new JLabel("Chưa kết nối");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        topBar.add(statusLabel, BorderLayout.WEST);

        disconnectBtn = new JButton("🚪 Rời Phòng");
        disconnectBtn.setBackground(UITheme.getDangerColor());
        disconnectBtn.setForeground(Color.WHITE);
        disconnectBtn.addActionListener(e -> disconnectAndExit());
        topBar.add(disconnectBtn, BorderLayout.EAST);

        root.add(topBar, BorderLayout.NORTH);

        // --- CENTER: Chat Area & User List SplitPane ---
        messagePane = new JTextPane();
        messagePane.setEditable(false);
        htmlKit = new HTMLEditorKit();
        htmlDoc = new HTMLDocument();
        messagePane.setEditorKit(htmlKit);
        messagePane.setDocument(htmlDoc);
        messagePane.setBackground(new Color(30, 32, 40));

        JScrollPane messageScrollPane = new JScrollPane(messagePane);
        messageScrollPane.setBorder(BorderFactory.createTitledBorder("💬 Tin Nhắn"));

        // User list
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String selected = userList.getSelectedValue();
                if (selected != null) {
                    if (selected.equals(username)) {
                        setRecipient(null); // Không chat riêng với chính mình
                    } else {
                        setRecipient(selected);
                    }
                }
            }
        });

        JPanel userPanel = new JPanel(new BorderLayout(5, 5));
        userPanel.setBorder(BorderFactory.createTitledBorder("👥 Online Users"));
        userPanel.setPreferredSize(new Dimension(180, 0));
        userPanel.add(new JScrollPane(userList), BorderLayout.CENTER);

        JButton toAllBtn = new JButton("📢 Chat Chung (Tất cả)");
        toAllBtn.addActionListener(e -> {
            userList.clearSelection();
            setRecipient(null);
        });
        userPanel.add(toAllBtn, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, userPanel, messageScrollPane);
        splitPane.setDividerLocation(190);
        root.add(splitPane, BorderLayout.CENTER);

        // --- BOTTOM: Input Bar ---
        JPanel bottomPanel = new JPanel(new BorderLayout(6, 6));

        JPanel targetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        targetLabel = new JLabel("Gửi tới: 🌐 TẤT CẢ MỌI NGƯỜI (Public)");
        targetLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        targetLabel.setForeground(UITheme.getPrimaryColor());
        targetPanel.add(targetLabel);
        bottomPanel.add(targetPanel, BorderLayout.NORTH);

        JPanel inputRow = new JPanel(new BorderLayout(6, 0));
        messageInput = new JTextField();
        messageInput.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageInput.addActionListener(e -> sendMessage());
        inputRow.add(messageInput, BorderLayout.CENTER);

        sendButton = new JButton("Gửi (Enter)");
        sendButton.setBackground(UITheme.getPrimaryColor());
        sendButton.setForeground(Color.WHITE);
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sendButton.addActionListener(e -> sendMessage());
        inputRow.add(sendButton, BorderLayout.EAST);

        bottomPanel.add(inputRow, BorderLayout.SOUTH);
        root.add(bottomPanel, BorderLayout.SOUTH);

        add(root);
    }

    private void setRecipient(String recipient) {
        this.selectedRecipient = recipient;
        if (recipient == null) {
            targetLabel.setText("Gửi tới: 🌐 TẤT CẢ MỌI NGƯỜI (Public)");
            targetLabel.setForeground(UITheme.getPrimaryColor());
        } else {
            targetLabel.setText("Gửi tới: 🔒 " + recipient + " (Tin Nhắn Riêng Tư)");
            targetLabel.setForeground(UITheme.getWarningColor());
        }
        messageInput.requestFocusInWindow();
    }

    public boolean showLoginDialog() {
        JPanel panel = new JPanel(new GridLayout(5, 2, 8, 8));

        JTextField hostField = new JTextField("127.0.0.1");
        JTextField portField = new JTextField("1099");
        JTextField userField = new JTextField("User" + (int)(Math.random() * 900 + 100));

        List<String> localIps = NetworkUtils.getAvailableIpv4Addresses();
        JComboBox<String> localIpCombo = new JComboBox<>(localIps.toArray(new String[0]));
        localIpCombo.setSelectedItem(NetworkUtils.getPreferredLocalIp());
        localIpCombo.setEditable(true);

        panel.add(new JLabel("IP Server (Máy thực):"));
        panel.add(hostField);

        panel.add(new JLabel("Port Server:"));
        panel.add(portField);

        panel.add(new JLabel("Tên Người Dùng:"));
        panel.add(userField);

        panel.add(new JLabel("IP Máy Khách (Callback):"));
        panel.add(localIpCombo);

        panel.add(new JLabel(""));
        JLabel tipLabel = new JLabel("<html><small style='color:gray'>Điền đúng IP Server nếu chạy khác máy</small></html>");
        panel.add(tipLabel);

        int result = JOptionPane.showConfirmDialog(
                this, panel, "Kết Nối RMI Chat Room",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return false;
        }

        this.serverHost = hostField.getText().trim();
        this.serverPort = Integer.parseInt(portField.getText().trim());
        this.username = userField.getText().trim();
        String localCallbackIp = (String) localIpCombo.getSelectedItem();

        if (this.username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tên người dùng không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return showLoginDialog();
        }

        // Cấu hình Callback hostname cho máy khách
        NetworkUtils.setRmiHostname(localCallbackIp);

        return connectToServer();
    }

    private boolean connectToServer() {
        try {
            Registry registry = LocateRegistry.getRegistry(serverHost, serverPort);
            server = (ChatServerInterface) registry.lookup("ChatServer");

            clientCallback = new ChatClientImpl(this.username, 0);
            clientCallback.setMessageListener(this::appendMessage);
            clientCallback.setUserListListener(this::updateUsersList);

            boolean success = server.registerClient(clientCallback, this.username);
            if (!success) {
                JOptionPane.showMessageDialog(this, "Tên '" + username + "' đã được sử dụng. Vui lòng chọn tên khác!", "Lỗi Đăng Ký", JOptionPane.WARNING_MESSAGE);
                return showLoginDialog();
            }

            statusLabel.setText("👤 " + username + "  |  🟢 Đã kết nối tới Server: " + serverHost + ":" + serverPort);
            statusLabel.setForeground(UITheme.getSuccessColor());
            setTitle("RMI Chat Client - " + username + " (" + serverHost + ")");

            appendRawHtml("<div style='color:#757575; font-style:italic; padding:4px;'>Chào mừng bạn <b>" + username + "</b> đến với phòng chat!</div>");
            return true;

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Không thể kết nối tới Chat Server tại " + serverHost + ":" + serverPort + "\nChi tiết lỗi: " + ex.getMessage(),
                    "Lỗi Kết Nối",
                    JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
    }

    private void sendMessage() {
        String text = messageInput.getText().trim();
        if (text.isEmpty() || server == null) return;

        try {
            if (selectedRecipient == null) {
                // Public chat
                ChatMessage msg = ChatMessage.publicMessage(username, text);
                server.broadcastMessage(msg);
            } else {
                // Private chat
                ChatMessage msg = ChatMessage.privateMessage(username, selectedRecipient, text);
                server.sendPrivateMessage(msg);
            }
            messageInput.setText("");
        } catch (Exception ex) {
            appendRawHtml("<div style='color:red;'>[Lỗi gửi tin]: " + ex.getMessage() + "</div>");
        }
    }

    public void appendMessage(ChatMessage msg) {
        SwingUtilities.invokeLater(() -> {
            String time = msg.getTimeString();
            String sender = msg.getSender();
            String content = escapeHtml(msg.getContent());

            String html;
            if (msg.getType() == MessageType.SYSTEM) {
                html = String.format(
                        "<div style='color:#9e9e9e; font-size:11px; margin:3px 0;'>[%s] 📢 <i>%s</i></div>",
                        time, content
                );
            } else if (msg.isPrivate()) {
                boolean isMe = sender.equals(username);
                String tag = isMe ? ("Bạn ➔ " + msg.getRecipient()) : (sender + " ➔ Bạn");
                html = String.format(
                        "<div style='background-color:#332940; border-left:3px solid #ff9800; padding:4px 8px; margin:4px 0; border-radius:4px;'>" +
                                "<span style='color:#ffb74d; font-weight:bold;'>🔒 [Riêng tư] %s</span> " +
                                "<span style='color:#757575; font-size:10px;'>(%s)</span><br/>" +
                                "<span style='color:#f5f5f5; font-size:13px;'>%s</span></div>",
                        tag, time, content
                );
            } else {
                boolean isMe = sender.equals(username);
                String color = isMe ? "#4caf50" : "#2196f3";
                String align = isMe ? "right" : "left";
                String bubbleColor = isMe ? "#1e3a29" : "#1e2e3d";

                html = String.format(
                        "<div style='text-align:%s; margin:4px 0;'>" +
                                "<div style='display:inline-block; max-width:80%%; text-align:left; background-color:%s; padding:5px 10px; border-radius:8px;'>" +
                                "<span style='color:%s; font-weight:bold; font-size:12px;'>%s</span> " +
                                "<span style='color:#757575; font-size:10px;'>%s</span><br/>" +
                                "<span style='color:#ffffff; font-size:13px;'>%s</span>" +
                                "</div></div>",
                        align, bubbleColor, color, (isMe ? "Bạn" : sender), time, content
                );
            }

            appendRawHtml(html);
        });
    }

    private void appendRawHtml(String html) {
        try {
            htmlKit.insertHTML(htmlDoc, htmlDoc.getLength(), html, 0, 0, null);
            messagePane.setCaretPosition(htmlDoc.getLength());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUsersList(List<String> users) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            for (String u : users) {
                if (u.equals(username)) {
                    userListModel.addElement(u + " (Bạn)");
                } else {
                    userListModel.addElement(u);
                }
            }
        });
    }

    private void disconnectAndExit() {
        if (server != null && username != null) {
            try {
                server.unregisterClient(username);
            } catch (Exception ignored) {}
        }
        if (clientCallback != null) {
            try {
                UnicastRemoteObject.unexportObject(clientCallback, true);
            } catch (Exception ignored) {}
        }
        dispose();
        System.exit(0);
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("\n", "<br/>");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChatClientWindow window = new ChatClientWindow();
            if (window.showLoginDialog()) {
                window.setVisible(true);
            } else {
                System.exit(0);
            }
        });
    }
}
