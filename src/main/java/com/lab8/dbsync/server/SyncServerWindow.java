package com.lab8.dbsync.server;

import com.lab8.common.NetworkUtils;
import com.lab8.common.UITheme;
import com.lab8.dbsync.db.DatabaseManager;
import com.lab8.dbsync.model.NodeInfo;
import com.lab8.dbsync.model.Student;
import com.lab8.dbsync.model.SyncOperation;
import com.lab8.dbsync.rmi.SyncNodeImpl;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SyncServerWindow extends JFrame {
    private final JTextField nodeIdField;
    private final JComboBox<String> ipComboBox;
    private final JTextField portField;
    private final JTextField dbPathField;
    private final JButton startBtn;
    private final JButton stopBtn;
    private final JLabel statusLabel;

    // Cấu hình 2 Peer Servers còn lại
    private final JTextField peer1HostField;
    private final JTextField peer1PortField;
    private final JTextField peer1IdField;

    private final JTextField peer2HostField;
    private final JTextField peer2PortField;
    private final JTextField peer2IdField;

    // Bảng dữ liệu Sinh viên cục bộ
    private final DefaultTableModel tableModel;
    private final JTable studentTable;

    // Form nhập liệu cục bộ
    private final JTextField codeField;
    private final JTextField nameField;
    private final JTextField classField;
    private final JTextField gpaField;
    private final JButton addBtn;
    private final JButton updateBtn;
    private final JButton deleteBtn;
    private final JButton catchUpBtn;

    // Nhật ký sự kiện
    private final JTextArea logArea;

    private Registry registry;
    private SyncNodeImpl syncNode;
    private DatabaseManager dbManager;
    private boolean isRunning = false;

    public SyncServerWindow(String defaultNodeId, int defaultPort, String defaultDbPath) {
        super("Hệ Thống Đồng Bộ 3 CSDL - " + defaultNodeId);
        UITheme.applyTheme(true);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 680);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- TOP: Server Node Configuration & Peer Setup ---
        JPanel topContainer = new JPanel(new GridLayout(2, 1, 6, 6));

        // Hàng 1: Cấu hình Server này
        JPanel selfConfigRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        selfConfigRow.add(new JLabel("Node ID:"));
        nodeIdField = new JTextField(defaultNodeId, 8);
        selfConfigRow.add(nodeIdField);

        selfConfigRow.add(new JLabel("IP:"));
        List<String> ips = NetworkUtils.getAvailableIpv4Addresses();
        ipComboBox = new JComboBox<>(ips.toArray(new String[0]));
        ipComboBox.setSelectedItem(NetworkUtils.getPreferredLocalIp());
        ipComboBox.setEditable(true);
        selfConfigRow.add(ipComboBox);

        selfConfigRow.add(new JLabel("Port:"));
        portField = new JTextField(String.valueOf(defaultPort), 5);
        selfConfigRow.add(portField);

        selfConfigRow.add(new JLabel("DB File:"));
        dbPathField = new JTextField(defaultDbPath, 10);
        selfConfigRow.add(dbPathField);

        startBtn = new JButton("▶ Chạy Node");
        startBtn.setBackground(UITheme.getSuccessColor());
        startBtn.setForeground(Color.WHITE);
        selfConfigRow.add(startBtn);

        stopBtn = new JButton("⏹ Dừng");
        stopBtn.setBackground(UITheme.getDangerColor());
        stopBtn.setForeground(Color.WHITE);
        stopBtn.setEnabled(false);
        selfConfigRow.add(stopBtn);

        statusLabel = new JLabel("🔴 Đang tắt");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        selfConfigRow.add(statusLabel);

        // Hàng 2: Cấu hình 2 Peer Server
        JPanel peersRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        peersRow.setBackground(UITheme.getCardBackgroundColor());
        peersRow.setBorder(BorderFactory.createTitledBorder("🌐 Kết Nối 2 Server Đối Tác (Replication Peers)"));

        // Xác định 2 Peer Server mặc định tùy theo Node ID hiện tại
        String defP1Id = "Server-2", defP1Port = "1100";
        String defP2Id = "Server-3", defP2Port = "1101";
        if ("Server-2".equalsIgnoreCase(defaultNodeId)) {
            defP1Id = "Server-1"; defP1Port = "1099";
            defP2Id = "Server-3"; defP2Port = "1101";
        } else if ("Server-3".equalsIgnoreCase(defaultNodeId)) {
            defP1Id = "Server-1"; defP1Port = "1099";
            defP2Id = "Server-2"; defP2Port = "1100";
        }

        peersRow.add(new JLabel("Peer 1 ID:"));
        peer1IdField = new JTextField(defP1Id, 7);
        peersRow.add(peer1IdField);
        peersRow.add(new JLabel("IP:"));
        peer1HostField = new JTextField("127.0.0.1", 10);
        peersRow.add(peer1HostField);
        peersRow.add(new JLabel("Port:"));
        peer1PortField = new JTextField(defP1Port, 5);
        peersRow.add(peer1PortField);

        peersRow.add(new JLabel(" | Peer 2 ID:"));
        peer2IdField = new JTextField(defP2Id, 7);
        peersRow.add(peer2IdField);
        peersRow.add(new JLabel("IP:"));
        peer2HostField = new JTextField("127.0.0.1", 10);
        peersRow.add(peer2HostField);
        peersRow.add(new JLabel("Port:"));
        peer2PortField = new JTextField(defP2Port, 5);
        peersRow.add(peer2PortField);

        topContainer.add(selfConfigRow);
        topContainer.add(peersRow);
        root.add(topContainer, BorderLayout.NORTH);

        // --- CENTER: SplitPane (Table Sinh Viên CSDL Cục Bộ + Log Đồng Bộ) ---
        String[] columns = {"Mã SV", "Họ và Tên", "Lớp", "Điểm TB", "Cập nhật lúc", "Ver"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        studentTable = new JTable(tableModel);
        studentTable.setRowHeight(22);
        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        studentTable.getSelectionModel().addListSelectionListener(e -> fillFormFromSelectedRow());

        JScrollPane tableScroll = new JScrollPane(studentTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("📋 Dữ Liệu CSDL Cục Bộ Của Server (0 bản ghi)"));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(new Color(25, 27, 32));
        logArea.setForeground(new Color(220, 220, 220));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("📜 Nhật Ký Đồng Bộ & Nhân Bản (Sync Log)"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScroll, logScroll);
        splitPane.setDividerLocation(520);
        root.add(splitPane, BorderLayout.CENTER);

        // --- BOTTOM: Form Thao Tác CSDL Cục Bộ ---
        JPanel bottomContainer = new JPanel(new BorderLayout(6, 6));

        JPanel formPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        formPanel.setBorder(BorderFactory.createTitledBorder("📝 Thao Tác Trực Tiếp CSDL (Tự động đồng bộ sang 2 server còn lại)"));

        formPanel.add(new JLabel("Mã SV:"));
        codeField = new JTextField(6);
        formPanel.add(codeField);

        formPanel.add(new JLabel("Họ Tên:"));
        nameField = new JTextField(12);
        formPanel.add(nameField);

        formPanel.add(new JLabel("Lớp:"));
        classField = new JTextField(8);
        formPanel.add(classField);

        formPanel.add(new JLabel("Điểm TB:"));
        gpaField = new JTextField(4);
        formPanel.add(gpaField);

        addBtn = new JButton("➕ Thêm");
        addBtn.setBackground(UITheme.getPrimaryColor());
        addBtn.setForeground(Color.WHITE);
        formPanel.add(addBtn);

        updateBtn = new JButton("✏ Sửa");
        formPanel.add(updateBtn);

        deleteBtn = new JButton("🗑 Xóa");
        deleteBtn.setBackground(UITheme.getDangerColor());
        deleteBtn.setForeground(Color.WHITE);
        formPanel.add(deleteBtn);

        catchUpBtn = new JButton("🔄 Bắt Kịp Dữ Liệu (Catch-up Sync)");
        catchUpBtn.setBackground(UITheme.getWarningColor());
        catchUpBtn.setForeground(Color.BLACK);
        formPanel.add(catchUpBtn);

        JButton clearBtn = new JButton("Làm mới Form");
        clearBtn.addActionListener(e -> clearForm());
        formPanel.add(clearBtn);

        bottomContainer.add(formPanel, BorderLayout.CENTER);
        root.add(bottomContainer, BorderLayout.SOUTH);

        add(root);

        // Action Listeners
        startBtn.addActionListener(e -> startNode());
        stopBtn.addActionListener(e -> stopNode());
        addBtn.addActionListener(e -> handleAdd());
        updateBtn.addActionListener(e -> handleUpdate());
        deleteBtn.addActionListener(e -> handleDelete());
        catchUpBtn.addActionListener(e -> handleCatchUp());
    }

    private void logToWindow(String msg) {
        SwingUtilities.invokeLater(() -> {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            logArea.append("[" + time + "] " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void startNode() {
        try {
            String nodeId = nodeIdField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());
            String chosenIp = (String) ipComboBox.getSelectedItem();
            String dbPath = dbPathField.getText().trim();

            NetworkUtils.setRmiHostname(chosenIp);

            try {
                registry = LocateRegistry.createRegistry(port);
                logToWindow("Đã tạo mới RMI Registry tại cổng " + port);
            } catch (Exception ex) {
                registry = LocateRegistry.getRegistry(port);
                logToWindow("Sử dụng RMI Registry hiện có tại cổng " + port);
            }

            dbManager = new DatabaseManager(dbPath);
            syncNode = new SyncNodeImpl(nodeId, dbManager, 0);
            syncNode.addLogListener(this::logToWindow);
            syncNode.addDataChangeListener(this::refreshTable);

            // Thêm cấu hình 2 peer
            NodeInfo peer1 = new NodeInfo(
                    peer1IdField.getText().trim(),
                    peer1HostField.getText().trim(),
                    Integer.parseInt(peer1PortField.getText().trim()),
                    ""
            );
            NodeInfo peer2 = new NodeInfo(
                    peer2IdField.getText().trim(),
                    peer2HostField.getText().trim(),
                    Integer.parseInt(peer2PortField.getText().trim()),
                    ""
            );
            syncNode.addPeer(peer1);
            syncNode.addPeer(peer2);

            String serviceName = "SyncNode_" + nodeId;
            registry.rebind(serviceName, syncNode);

            isRunning = true;
            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);
            nodeIdField.setEnabled(false);
            portField.setEnabled(false);
            ipComboBox.setEnabled(false);
            dbPathField.setEnabled(false);

            statusLabel.setText("🟢 Đang chạy trên " + chosenIp + ":" + port);
            statusLabel.setForeground(UITheme.getSuccessColor());
            logToWindow("✅ Node [" + nodeId + "] đã sẵn sàng phục vụ tại rmi://" + chosenIp + ":" + port + "/" + serviceName);

            refreshTable();

            // Tự động kéo dữ liệu nếu 2 server kia đã có dữ liệu trước
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    syncNode.syncMissedFromPeers();
                } catch (Exception ignored) {}
            }).start();

        } catch (Exception ex) {
            logToWindow("❌ Lỗi khởi động Node: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Không thể khởi động Node: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopNode() {
        try {
            if (registry != null && syncNode != null) {
                registry.unbind("SyncNode_" + nodeIdField.getText().trim());
                UnicastRemoteObject.unexportObject(syncNode, true);
            }
            isRunning = false;
            startBtn.setEnabled(true);
            stopBtn.setEnabled(false);
            nodeIdField.setEnabled(true);
            portField.setEnabled(true);
            ipComboBox.setEnabled(true);
            dbPathField.setEnabled(true);

            statusLabel.setText("🔴 Đang tắt");
            statusLabel.setForeground(Color.LIGHT_GRAY);
            logToWindow("🛑 Đã dừng Node Server.");
        } catch (Exception ex) {
            logToWindow("Lỗi khi dừng server: " + ex.getMessage());
        }
    }

    private void refreshTable() {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            if (dbManager != null) {
                List<Student> list = dbManager.getAllStudents();
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
                for (Student s : list) {
                    String timeStr = LocalTime.now().format(dtf);
                    tableModel.addRow(new Object[]{
                            s.getStudentCode(),
                            s.getFullName(),
                            s.getClassName(),
                            s.getGpa(),
                            timeStr,
                            s.getVersion()
                    });
                }
                ((JComponent) studentTable.getParent().getParent()).setBorder(
                        BorderFactory.createTitledBorder("📋 Dữ Liệu CSDL Cục Bộ (" + list.size() + " bản ghi)")
                );
            }
        });
    }

    private void handleAdd() {
        if (!isRunning || syncNode == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng khởi động Node trước khi thao tác CSDL!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            String code = codeField.getText().trim();
            String name = nameField.getText().trim();
            String className = classField.getText().trim();
            double gpa = Double.parseDouble(gpaField.getText().trim());

            if (code.isEmpty() || name.isEmpty() || className.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng điền đầy đủ thông tin!", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Student s = new Student(code, name, className, gpa);
            SyncOperation op = SyncOperation.insertOp(syncNode.getNodeId(), s);
            syncNode.executeLocalOperation(op);
            clearForm();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Điểm GPA phải là số thực hợp lệ (ví dụ: 3.5)", "Lỗi", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            logToWindow("Lỗi thêm sinh viên: " + ex.getMessage());
        }
    }

    private void handleUpdate() {
        if (!isRunning || syncNode == null) return;
        try {
            String code = codeField.getText().trim();
            String name = nameField.getText().trim();
            String className = classField.getText().trim();
            double gpa = Double.parseDouble(gpaField.getText().trim());

            Student existing = dbManager.getStudent(code);
            int nextVersion = existing != null ? existing.getVersion() + 1 : 1;
            Student s = new Student(code, name, className, gpa, System.currentTimeMillis(), nextVersion);

            SyncOperation op = SyncOperation.updateOp(syncNode.getNodeId(), s);
            syncNode.executeLocalOperation(op);
            clearForm();
        } catch (Exception ex) {
            logToWindow("Lỗi cập nhật sinh viên: " + ex.getMessage());
        }
    }

    private void handleDelete() {
        if (!isRunning || syncNode == null) return;
        String code = codeField.getText().trim();
        if (code.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn hoặc nhập Mã SV cần xóa!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn xóa sinh viên " + code + "?\nThao tác này sẽ đồng bộ xóa trên cả 3 server!", "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                SyncOperation op = SyncOperation.deleteOp(syncNode.getNodeId(), code);
                syncNode.executeLocalOperation(op);
                clearForm();
            } catch (Exception ex) {
                logToWindow("Lỗi xóa sinh viên: " + ex.getMessage());
            }
        }
    }

    private void handleCatchUp() {
        if (!isRunning || syncNode == null) return;
        new Thread(() -> {
            try {
                syncNode.syncMissedFromPeers();
                JOptionPane.showMessageDialog(this, "Đã thực hiện xong quá trình kiểm tra và bắt kịp dữ liệu từ các Peer!", "Hoàn tất", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                logToWindow("Lỗi khi kéo dữ liệu bù: " + ex.getMessage());
            }
        }).start();
    }

    private void fillFormFromSelectedRow() {
        int row = studentTable.getSelectedRow();
        if (row >= 0) {
            codeField.setText(tableModel.getValueAt(row, 0).toString());
            nameField.setText(tableModel.getValueAt(row, 1).toString());
            classField.setText(tableModel.getValueAt(row, 2).toString());
            gpaField.setText(tableModel.getValueAt(row, 3).toString());
        }
    }

    private void clearForm() {
        codeField.setText("");
        nameField.setText("");
        classField.setText("");
        gpaField.setText("");
        studentTable.clearSelection();
    }

    public static void main(String[] args) {
        String nodeId = args.length > 0 ? args[0] : "Server-1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 1099;
        String dbPath = args.length > 2 ? args[2] : "server1_db.sqlite";

        SwingUtilities.invokeLater(() -> {
            SyncServerWindow win = new SyncServerWindow(nodeId, port, dbPath);
            win.setVisible(true);
        });
    }
}
