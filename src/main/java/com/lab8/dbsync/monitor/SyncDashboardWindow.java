package com.lab8.dbsync.monitor;

import com.lab8.common.UITheme;
import com.lab8.dbsync.model.Student;
import com.lab8.dbsync.model.SyncOperation;
import com.lab8.dbsync.rmi.SyncNodeInterface;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

public class SyncDashboardWindow extends JFrame {

    // Thông tin kết nối 3 Server
    private final JTextField host1Field = new JTextField("127.0.0.1", 9);
    private final JTextField port1Field = new JTextField("1099", 4);

    private final JTextField host2Field = new JTextField("127.0.0.1", 9);
    private final JTextField port2Field = new JTextField("1100", 4);

    private final JTextField host3Field = new JTextField("127.0.0.1", 9);
    private final JTextField port3Field = new JTextField("1101", 4);

    private final JLabel status1Label = new JLabel("Chưa kết nối");
    private final JLabel status2Label = new JLabel("Chưa kết nối");
    private final JLabel status3Label = new JLabel("Chưa kết nối");

    private final DefaultTableModel model1;
    private final DefaultTableModel model2;
    private final DefaultTableModel model3;

    private final JTable table1;
    private final JTable table2;
    private final JTable table3;

    private final JLabel overallSyncStatusLabel;
    private final JCheckBox autoRefreshCheck;

    // Form thêm/sửa/xóa
    private final JComboBox<String> targetServerCombo;
    private final JTextField codeField = new JTextField(6);
    private final JTextField nameField = new JTextField(11);
    private final JTextField classField = new JTextField(7);
    private final JTextField gpaField = new JTextField(4);

    private SyncNodeInterface node1;
    private SyncNodeInterface node2;
    private SyncNodeInterface node3;

    private Timer refreshTimer;

    public SyncDashboardWindow() {
        super("Bảng Điều Khiển Giám Sát Đồng Bộ 3 CSDL (3-Server Database Synchronization Monitor)");
        UITheme.applyTheme(true);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1240, 750);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- TOP: Kết nối 3 Node & Trạng thái đồng bộ toàn cục ---
        JPanel topContainer = new JPanel(new BorderLayout(6, 6));

        JPanel connPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        connPanel.setBackground(UITheme.getCardBackgroundColor());
        connPanel.setBorder(BorderFactory.createTitledBorder("🌐 Cấu Hình Địa Chỉ 3 Server (Máy Thực 1, Máy Thực 2, Máy Ảo)"));

        connPanel.add(new JLabel("🖥 Server 1:"));
        connPanel.add(host1Field);
        connPanel.add(new JLabel(":"));
        connPanel.add(port1Field);

        connPanel.add(new JLabel(" | 🖥 Server 2:"));
        connPanel.add(host2Field);
        connPanel.add(new JLabel(":"));
        connPanel.add(port2Field);

        connPanel.add(new JLabel(" | 🖥 Server 3:"));
        connPanel.add(host3Field);
        connPanel.add(new JLabel(":"));
        connPanel.add(port3Field);

        JButton connectAllBtn = new JButton("🔌 Kết Nối 3 Server");
        connectAllBtn.setBackground(UITheme.getPrimaryColor());
        connectAllBtn.setForeground(Color.WHITE);
        connectAllBtn.addActionListener(e -> connectAllNodes());
        connPanel.add(connectAllBtn);

        JButton refreshAllBtn = new JButton("🔄 Làm Mới");
        refreshAllBtn.addActionListener(e -> refreshAllData());
        connPanel.add(refreshAllBtn);

        autoRefreshCheck = new JCheckBox("Tự động làm mới (2s)", true);
        autoRefreshCheck.addActionListener(e -> toggleAutoRefresh());
        connPanel.add(autoRefreshCheck);

        // Overall status bar
        JPanel overallPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        overallPanel.setBackground(new Color(25, 30, 40));
        overallSyncStatusLabel = new JLabel("⏳ Đang chuẩn bị kết nối...");
        overallSyncStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        overallPanel.add(overallSyncStatusLabel);

        topContainer.add(connPanel, BorderLayout.NORTH);
        topContainer.add(overallPanel, BorderLayout.SOUTH);
        root.add(topContainer, BorderLayout.NORTH);

        // --- CENTER: 3 Cột hiển thị 3 CSDL của 3 Server ---
        String[] cols = {"Mã SV", "Họ Tên", "Lớp", "GPA", "Ver"};

        model1 = createTableModel(cols);
        model2 = createTableModel(cols);
        model3 = createTableModel(cols);

        table1 = new JTable(model1);
        table2 = new JTable(model2);
        table3 = new JTable(model3);

        JPanel centerPanel = new JPanel(new GridLayout(1, 3, 8, 0));
        centerPanel.add(buildServerColumnPanel("🖥 Server 1 (Máy thực 1 - DB1)", host1Field, port1Field, status1Label, table1));
        centerPanel.add(buildServerColumnPanel("🖥 Server 2 (Máy thực 2 - DB2)", host2Field, port2Field, status2Label, table2));
        centerPanel.add(buildServerColumnPanel("🖥 Server 3 (Máy ảo - DB3)", host3Field, port3Field, status3Label, table3));
        root.add(centerPanel, BorderLayout.CENTER);

        // --- BOTTOM: Thanh điều khiển thao tác CRUD trực quan ---
        JPanel bottomPanel = new JPanel(new BorderLayout(6, 6));

        JPanel crudPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        crudPanel.setBorder(BorderFactory.createTitledBorder("⚡ Thử Nghiệm Đồng Bộ Hóa (Gửi lệnh tới 1 Server ➔ Kiểm tra 2 Server còn lại tức thời)"));

        crudPanel.add(new JLabel("Gửi lệnh vào:"));
        targetServerCombo = new JComboBox<>(new String[]{"Server 1 (Máy thực 1)", "Server 2 (Máy thực 2)", "Server 3 (Máy ảo)"});
        crudPanel.add(targetServerCombo);

        crudPanel.add(new JLabel("Mã SV:"));
        crudPanel.add(codeField);

        crudPanel.add(new JLabel("Họ Tên:"));
        crudPanel.add(nameField);

        crudPanel.add(new JLabel("Lớp:"));
        crudPanel.add(classField);

        crudPanel.add(new JLabel("GPA:"));
        crudPanel.add(gpaField);

        JButton addBtn = new JButton("➕ Thêm");
        addBtn.setBackground(UITheme.getSuccessColor());
        addBtn.setForeground(Color.WHITE);
        addBtn.addActionListener(e -> handleAdd());
        crudPanel.add(addBtn);

        JButton updateBtn = new JButton("✏ Sửa");
        updateBtn.addActionListener(e -> handleUpdate());
        crudPanel.add(updateBtn);

        JButton deleteBtn = new JButton("🗑 Xóa");
        deleteBtn.setBackground(UITheme.getDangerColor());
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.addActionListener(e -> handleDelete());
        crudPanel.add(deleteBtn);

        JButton reconcileBtn = new JButton("🔄 Kích Hoạt Đồng Bộ Bù (Catch-Up All)");
        reconcileBtn.setBackground(UITheme.getWarningColor());
        reconcileBtn.setForeground(Color.BLACK);
        reconcileBtn.addActionListener(e -> triggerReconciliation());
        crudPanel.add(reconcileBtn);

        bottomPanel.add(crudPanel, BorderLayout.CENTER);
        root.add(bottomPanel, BorderLayout.SOUTH);

        add(root);

        // Timer tự động làm mới mỗi 2 giây
        refreshTimer = new Timer(2000, e -> refreshAllData());
        refreshTimer.start();

        // Kết nối thử ban đầu
        SwingUtilities.invokeLater(this::connectAllNodes);
    }

    private DefaultTableModel createTableModel(String[] cols) {
        return new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
    }

    private JPanel buildServerColumnPanel(String title, JTextField hostF, JTextField portF, JLabel statusLbl, JTable table) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder(title));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        header.add(statusLbl);
        panel.add(header, BorderLayout.NORTH);

        table.setRowHeight(22);
        JScrollPane scroll = new JScrollPane(table);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private void toggleAutoRefresh() {
        if (autoRefreshCheck.isSelected()) {
            refreshTimer.start();
        } else {
            refreshTimer.stop();
        }
    }

    private void connectAllNodes() {
        connectNode(1, host1Field.getText().trim(), Integer.parseInt(port1Field.getText().trim()), "Server-1", status1Label);
        connectNode(2, host2Field.getText().trim(), Integer.parseInt(port2Field.getText().trim()), "Server-2", status2Label);
        connectNode(3, host3Field.getText().trim(), Integer.parseInt(port3Field.getText().trim()), "Server-3", status3Label);
        refreshAllData();
    }

    private void connectNode(int idx, String host, int port, String nodeId, JLabel statusLbl) {
        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            SyncNodeInterface stub = (SyncNodeInterface) registry.lookup("SyncNode_" + nodeId);
            stub.ping();

            if (idx == 1) node1 = stub;
            else if (idx == 2) node2 = stub;
            else if (idx == 3) node3 = stub;

            statusLbl.setText("🟢 Online (" + host + ":" + port + ")");
            statusLbl.setForeground(UITheme.getSuccessColor());
        } catch (Exception ex) {
            if (idx == 1) node1 = null;
            else if (idx == 2) node2 = null;
            else if (idx == 3) node3 = null;

            statusLbl.setText("🔴 Offline (" + host + ":" + port + ")");
            statusLbl.setForeground(UITheme.getDangerColor());
        }
    }

    public void refreshAllData() {
        new Thread(() -> {
            List<Student> list1 = fetchStudents(node1);
            List<Student> list2 = fetchStudents(node2);
            List<Student> list3 = fetchStudents(node3);

            SwingUtilities.invokeLater(() -> {
                populateTable(model1, list1);
                populateTable(model2, list2);
                populateTable(model3, list3);

                updateOverallStatus(list1, list2, list3);
            });
        }).start();
    }

    private List<Student> fetchStudents(SyncNodeInterface node) {
        if (node == null) return null;
        try {
            return node.getAllStudents();
        } catch (Exception e) {
            return null;
        }
    }

    private void populateTable(DefaultTableModel model, List<Student> list) {
        model.setRowCount(0);
        if (list != null) {
            for (Student s : list) {
                model.addRow(new Object[]{
                        s.getStudentCode(),
                        s.getFullName(),
                        s.getClassName(),
                        s.getGpa(),
                        s.getVersion()
                });
            }
        }
    }

    private void updateOverallStatus(List<Student> l1, List<Student> l2, List<Student> l3) {
        int onlineCount = (node1 != null ? 1 : 0) + (node2 != null ? 1 : 0) + (node3 != null ? 1 : 0);

        if (onlineCount == 0) {
            overallSyncStatusLabel.setText("🔴 Cả 3 Server đều đang Offline.");
            overallSyncStatusLabel.setForeground(UITheme.getDangerColor());
            return;
        }

        // So sánh tính nhất quán giữa các server đang online
        List<List<Student>> activeLists = new ArrayList<>();
        if (l1 != null) activeLists.add(l1);
        if (l2 != null) activeLists.add(l2);
        if (l3 != null) activeLists.add(l3);

        boolean allMatch = true;
        if (activeLists.size() > 1) {
            List<Student> first = activeLists.get(0);
            for (int i = 1; i < activeLists.size(); i++) {
                if (!first.equals(activeLists.get(i))) {
                    allMatch = false;
                    break;
                }
            }
        }

        if (allMatch && onlineCount == 3) {
            int count = l1 != null ? l1.size() : 0;
            overallSyncStatusLabel.setText("🟢 ĐỒNG BỘ HOÀN HẢO 100% — Cả 3 CSDL đều có " + count + " bản ghi trùng khớp tuyệt đối!");
            overallSyncStatusLabel.setForeground(UITheme.getSuccessColor());
        } else if (allMatch) {
            overallSyncStatusLabel.setText("🟡 ĐỒNG BỘ TẠM THỜI (" + onlineCount + "/3 Server Online) — Các server đang hoạt động có dữ liệu khớp nhau.");
            overallSyncStatusLabel.setForeground(UITheme.getWarningColor());
        } else {
            overallSyncStatusLabel.setText("⚠ CẢNH BÁO: DỮ LIỆU ĐANG LỆCH GIỮA CÁC SERVER — Bấm 'Kích Hoạt Đồng Bộ Bù' để sửa.");
            overallSyncStatusLabel.setForeground(UITheme.getDangerColor());
        }
    }

    private SyncNodeInterface getSelectedTargetNode() {
        int idx = targetServerCombo.getSelectedIndex();
        if (idx == 0) return node1;
        if (idx == 1) return node2;
        return node3;
    }

    private void handleAdd() {
        SyncNodeInterface target = getSelectedTargetNode();
        if (target == null) {
            JOptionPane.showMessageDialog(this, "Server được chọn hiện đang Offline!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            String code = codeField.getText().trim();
            String name = nameField.getText().trim();
            String className = classField.getText().trim();
            double gpa = Double.parseDouble(gpaField.getText().trim());

            if (code.isEmpty() || name.isEmpty() || className.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đủ thông tin!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Student s = new Student(code, name, className, gpa);
            SyncOperation op = SyncOperation.insertOp(target.getNodeId(), s);
            target.executeLocalOperation(op);

            codeField.setText("");
            nameField.setText("");
            classField.setText("");
            gpaField.setText("");

            Thread.sleep(100);
            refreshAllData();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Điểm GPA phải là số thực!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi thêm: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleUpdate() {
        SyncNodeInterface target = getSelectedTargetNode();
        if (target == null) {
            JOptionPane.showMessageDialog(this, "Server được chọn hiện đang Offline!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            String code = codeField.getText().trim();
            String name = nameField.getText().trim();
            String className = classField.getText().trim();
            double gpa = Double.parseDouble(gpaField.getText().trim());

            Student existing = target.getStudent(code);
            int ver = existing != null ? existing.getVersion() + 1 : 1;
            Student s = new Student(code, name, className, gpa, System.currentTimeMillis(), ver);

            SyncOperation op = SyncOperation.updateOp(target.getNodeId(), s);
            target.executeLocalOperation(op);

            Thread.sleep(100);
            refreshAllData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi sửa: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDelete() {
        SyncNodeInterface target = getSelectedTargetNode();
        if (target == null) {
            JOptionPane.showMessageDialog(this, "Server được chọn hiện đang Offline!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String code = codeField.getText().trim();
        if (code.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nhập Mã SV cần xóa!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            SyncOperation op = SyncOperation.deleteOp(target.getNodeId(), code);
            target.executeLocalOperation(op);

            Thread.sleep(100);
            refreshAllData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi xóa: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void triggerReconciliation() {
        new Thread(() -> {
            try {
                if (node1 != null) node1.syncMissedFromPeers();
                if (node2 != null) node2.syncMissedFromPeers();
                if (node3 != null) node3.syncMissedFromPeers();
                Thread.sleep(300);
                refreshAllData();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Đã gửi yêu cầu đồng bộ bù tới tất cả các server đang sống!", "Hoàn tất", JOptionPane.INFORMATION_MESSAGE));
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi đồng bộ bù: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SyncDashboardWindow win = new SyncDashboardWindow();
            win.setVisible(true);
        });
    }
}
