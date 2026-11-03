package com.lab8.dbsync.client;

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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SyncClientWindow extends JFrame {
    private final JTextField hostField;
    private final JTextField portField;
    private final JTextField serviceNodeIdField;
    private final JButton connectBtn;
    private final JLabel connectionStatusLabel;

    private final DefaultTableModel tableModel;
    private final JTable studentTable;

    private final JTextField codeField;
    private final JTextField nameField;
    private final JTextField classField;
    private final JTextField gpaField;
    private final JButton addBtn;
    private final JButton updateBtn;
    private final JButton deleteBtn;
    private final JButton refreshBtn;

    private SyncNodeInterface currentNode;

    public SyncClientWindow() {
        super("Client Quản Lý Sinh Viên (Đồng Bộ 3 CSDL RMI)");
        UITheme.applyTheme(true);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 580);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- TOP: Kết nối tới Server ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        topPanel.setBackground(UITheme.getCardBackgroundColor());
        topPanel.setBorder(BorderFactory.createTitledBorder("🔌 Kết Nối Tới Server CSDL"));

        topPanel.add(new JLabel("Node ID:"));
        serviceNodeIdField = new JTextField("Server-1", 7);
        topPanel.add(serviceNodeIdField);

        topPanel.add(new JLabel("IP Server:"));
        hostField = new JTextField("127.0.0.1", 10);
        topPanel.add(hostField);

        topPanel.add(new JLabel("Port:"));
        portField = new JTextField("1099", 5);
        topPanel.add(portField);

        connectBtn = new JButton("Kết Nối");
        connectBtn.setBackground(UITheme.getPrimaryColor());
        connectBtn.setForeground(Color.WHITE);
        topPanel.add(connectBtn);

        connectionStatusLabel = new JLabel("Chưa kết nối");
        connectionStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        topPanel.add(connectionStatusLabel);

        root.add(topPanel, BorderLayout.NORTH);

        // --- CENTER: Bảng dữ liệu sinh viên ---
        String[] cols = {"Mã SV", "Họ và Tên", "Lớp", "Điểm GPA", "Phiên Bản"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        studentTable = new JTable(tableModel);
        studentTable.setRowHeight(24);
        studentTable.getSelectionModel().addListSelectionListener(e -> fillFormFromSelectedRow());

        JScrollPane scrollPane = new JScrollPane(studentTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("📋 Danh Sách Sinh Viên"));
        root.add(scrollPane, BorderLayout.CENTER);

        // --- BOTTOM: Form Thao tác ---
        JPanel bottomContainer = new JPanel(new BorderLayout(5, 5));

        JPanel formPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        formPanel.add(new JLabel("Mã SV:"));
        codeField = new JTextField(6);
        formPanel.add(codeField);

        formPanel.add(new JLabel("Họ Tên:"));
        nameField = new JTextField(12);
        formPanel.add(nameField);

        formPanel.add(new JLabel("Lớp:"));
        classField = new JTextField(8);
        formPanel.add(classField);

        formPanel.add(new JLabel("GPA:"));
        gpaField = new JTextField(4);
        formPanel.add(gpaField);

        addBtn = new JButton("➕ Thêm");
        addBtn.setBackground(UITheme.getSuccessColor());
        addBtn.setForeground(Color.WHITE);
        formPanel.add(addBtn);

        updateBtn = new JButton("✏ Sửa");
        formPanel.add(updateBtn);

        deleteBtn = new JButton("🗑 Xóa");
        deleteBtn.setBackground(UITheme.getDangerColor());
        deleteBtn.setForeground(Color.WHITE);
        formPanel.add(deleteBtn);

        refreshBtn = new JButton("🔄 Làm Mới");
        formPanel.add(refreshBtn);

        bottomContainer.add(formPanel, BorderLayout.CENTER);
        root.add(bottomContainer, BorderLayout.SOUTH);

        add(root);

        // Listeners
        connectBtn.addActionListener(e -> connectToServer());
        addBtn.addActionListener(e -> handleAdd());
        updateBtn.addActionListener(e -> handleUpdate());
        deleteBtn.addActionListener(e -> handleDelete());
        refreshBtn.addActionListener(e -> loadData());
    }

    private void connectToServer() {
        String host = hostField.getText().trim();
        int port = Integer.parseInt(portField.getText().trim());
        String nodeId = serviceNodeIdField.getText().trim();
        String serviceName = "SyncNode_" + nodeId;

        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            currentNode = (SyncNodeInterface) registry.lookup(serviceName);

            connectionStatusLabel.setText("🟢 Đã kết nối tới " + nodeId + " (" + host + ":" + port + ")");
            connectionStatusLabel.setForeground(UITheme.getSuccessColor());
            setTitle("Client Quản Lý Sinh Viên - Đang kết nối " + nodeId);

            loadData();
            JOptionPane.showMessageDialog(this, "Kết nối thành công tới " + nodeId + "!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            connectionStatusLabel.setText("🔴 Lỗi kết nối: " + ex.getMessage());
            connectionStatusLabel.setForeground(UITheme.getDangerColor());
            JOptionPane.showMessageDialog(this, "Không thể kết nối tới " + serviceName + " tại " + host + ":" + port + "\n" + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadData() {
        if (currentNode == null) return;
        new Thread(() -> {
            try {
                List<Student> list = currentNode.getAllStudents();
                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0);
                    for (Student s : list) {
                        tableModel.addRow(new Object[]{
                                s.getStudentCode(),
                                s.getFullName(),
                                s.getClassName(),
                                s.getGpa(),
                                s.getVersion()
                        });
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi khi nạp dữ liệu: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    private void handleAdd() {
        if (currentNode == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng kết nối tới Server trước!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            String code = codeField.getText().trim();
            String name = nameField.getText().trim();
            String className = classField.getText().trim();
            double gpa = Double.parseDouble(gpaField.getText().trim());

            if (code.isEmpty() || name.isEmpty() || className.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng điền đủ thông tin!", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Student s = new Student(code, name, className, gpa);
            SyncOperation op = SyncOperation.insertOp(currentNode.getNodeId(), s);
            boolean ok = currentNode.executeLocalOperation(op);
            if (ok) {
                JOptionPane.showMessageDialog(this, "Đã thêm sinh viên và tự động đồng bộ sang 2 server còn lại!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                loadData();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi thêm: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleUpdate() {
        if (currentNode == null) return;
        try {
            String code = codeField.getText().trim();
            String name = nameField.getText().trim();
            String className = classField.getText().trim();
            double gpa = Double.parseDouble(gpaField.getText().trim());

            Student existing = currentNode.getStudent(code);
            int nextVersion = existing != null ? existing.getVersion() + 1 : 1;
            Student s = new Student(code, name, className, gpa, System.currentTimeMillis(), nextVersion);

            SyncOperation op = SyncOperation.updateOp(currentNode.getNodeId(), s);
            boolean ok = currentNode.executeLocalOperation(op);
            if (ok) {
                JOptionPane.showMessageDialog(this, "Đã cập nhật sinh viên và tự động đồng bộ sang 2 server còn lại!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                loadData();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi sửa: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDelete() {
        if (currentNode == null) return;
        String code = codeField.getText().trim();
        if (code.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn Mã SV cần xóa!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có muốn xóa sinh viên " + code + "? (Sẽ đồng bộ xóa trên cả 3 server)", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                SyncOperation op = SyncOperation.deleteOp(currentNode.getNodeId(), code);
                boolean ok = currentNode.executeLocalOperation(op);
                if (ok) {
                    JOptionPane.showMessageDialog(this, "Đã xóa sinh viên trên cả 3 server!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    clearForm();
                    loadData();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi xóa: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
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
        SwingUtilities.invokeLater(() -> {
            SyncClientWindow win = new SyncClientWindow();
            win.setVisible(true);
        });
    }
}
