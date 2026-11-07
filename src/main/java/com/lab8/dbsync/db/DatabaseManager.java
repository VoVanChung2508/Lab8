package com.lab8.dbsync.db;

import com.lab8.dbsync.model.OperationType;
import com.lab8.dbsync.model.Student;
import com.lab8.dbsync.model.SyncOperation;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private final String dbUrl;
    private final String dbPath;

    public DatabaseManager(String dbPath) {
        this.dbPath = dbPath;
        // Đảm bảo thư mục cha tồn tại nếu có
        File file = new File(dbPath);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        this.dbUrl = "jdbc:sqlite:" + dbPath;
        initDatabase();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    private void initDatabase() {
        String createStudentTable = "CREATE TABLE IF NOT EXISTS students (" +
                "student_code TEXT PRIMARY KEY, " +
                "full_name TEXT NOT NULL, " +
                "class_name TEXT NOT NULL, " +
                "gpa REAL NOT NULL, " +
                "updated_at INTEGER NOT NULL, " +
                "version INTEGER NOT NULL" +
                ");";

        String createLogTable = "CREATE TABLE IF NOT EXISTS sync_log (" +
                "op_id TEXT PRIMARY KEY, " +
                "origin_node TEXT NOT NULL, " +
                "op_type TEXT NOT NULL, " +
                "student_code TEXT NOT NULL, " +
                "data_summary TEXT, " +
                "timestamp INTEGER NOT NULL" +
                ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createStudentTable);
            stmt.execute(createLogTable);
        } catch (SQLException e) {
            System.err.println("Lỗi khởi tạo CSDL SQLite (" + dbPath + "): " + e.getMessage());
        }
    }

    public synchronized boolean hasAppliedOp(String opId) {
        String sql = "SELECT 1 FROM sync_log WHERE op_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, opId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public synchronized void recordOp(SyncOperation op) {
        String sql = "INSERT OR IGNORE INTO sync_log (op_id, origin_node, op_type, student_code, data_summary, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, op.getOpId());
            pstmt.setString(2, op.getOriginNodeId());
            pstmt.setString(3, op.getOpType().name());
            pstmt.setString(4, op.getStudentCode());
            String summary = op.getStudent() != null ? op.getStudent().toString() : "DELETE " + op.getStudentCode();
            pstmt.setString(5, summary);
            pstmt.setLong(6, op.getTimestamp());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Lỗi ghi log đồng bộ: " + e.getMessage());
        }
    }

    public synchronized boolean applySyncOperation(SyncOperation op) {
        if (hasAppliedOp(op.getOpId())) {
            return true; // Đã áp dụng trước đó, tránh áp dụng lặp lại (Idempotent)
        }

        boolean success = false;
        if (op.getOpType() == OperationType.INSERT || op.getOpType() == OperationType.UPDATE) {
            Student s = op.getStudent();
            if (s != null) {
                String sql = "INSERT INTO students (student_code, full_name, class_name, gpa, updated_at, version) " +
                        "VALUES (?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT(student_code) DO UPDATE SET " +
                        "full_name = excluded.full_name, " +
                        "class_name = excluded.class_name, " +
                        "gpa = excluded.gpa, " +
                        "updated_at = excluded.updated_at, " +
                        "version = excluded.version;";
                try (Connection conn = getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, s.getStudentCode());
                    pstmt.setString(2, s.getFullName());
                    pstmt.setString(3, s.getClassName());
                    pstmt.setDouble(4, s.getGpa());
                    pstmt.setLong(5, s.getUpdatedAt());
                    pstmt.setInt(6, s.getVersion());
                    pstmt.executeUpdate();
                    success = true;
                } catch (SQLException e) {
                    System.err.println("Lỗi thực thi Insert/Update: " + e.getMessage());
                }
            }
        } else if (op.getOpType() == OperationType.DELETE) {
            String sql = "DELETE FROM students WHERE student_code = ?";
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, op.getStudentCode());
                pstmt.executeUpdate();
                success = true;
            } catch (SQLException e) {
                System.err.println("Lỗi thực thi Delete: " + e.getMessage());
            }
        }

        if (success) {
            recordOp(op);
        }
        return success;
    }

    public synchronized List<Student> getAllStudents() {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT student_code, full_name, class_name, gpa, updated_at, version FROM students ORDER BY student_code ASC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Student(
                        rs.getString("student_code"),
                        rs.getString("full_name"),
                        rs.getString("class_name"),
                        rs.getDouble("gpa"),
                        rs.getLong("updated_at"),
                        rs.getInt("version")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Lỗi đọc danh sách sinh viên: " + e.getMessage());
        }
        return list;
    }

    public synchronized Student getStudent(String code) {
        String sql = "SELECT student_code, full_name, class_name, gpa, updated_at, version FROM students WHERE student_code = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, code);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Student(
                            rs.getString("student_code"),
                            rs.getString("full_name"),
                            rs.getString("class_name"),
                            rs.getDouble("gpa"),
                            rs.getLong("updated_at"),
                            rs.getInt("version")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi tìm sinh viên: " + e.getMessage());
        }
        return null;
    }

    public synchronized int getRecordCount() {
        String sql = "SELECT COUNT(*) FROM students";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ignored) {}
        return 0;
    }

    public synchronized List<SyncOperation> getOperationsSince(long timestamp) {
        List<SyncOperation> list = new ArrayList<>();
        String sql = "SELECT op_id, origin_node, op_type, student_code, timestamp FROM sync_log WHERE timestamp > ? ORDER BY timestamp ASC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, timestamp);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String opId = rs.getString("op_id");
                    String origin = rs.getString("origin_node");
                    OperationType type = OperationType.valueOf(rs.getString("op_type"));
                    String code = rs.getString("student_code");
                    long ts = rs.getLong("timestamp");

                    Student student = null;
                    if (type != OperationType.DELETE) {
                        student = getStudent(code);
                    }
                    list.add(new SyncOperation(opId, origin, type, code, student, ts));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi đọc danh sách log theo thời gian: " + e.getMessage());
        }
        return list;
    }

    public synchronized void mergeStudents(List<Student> students) {
        if (students == null || students.isEmpty()) return;
        String sql = "INSERT INTO students (student_code, full_name, class_name, gpa, updated_at, version) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(student_code) DO UPDATE SET " +
                "full_name = excluded.full_name, " +
                "class_name = excluded.class_name, " +
                "gpa = excluded.gpa, " +
                "updated_at = excluded.updated_at, " +
                "version = CASE WHEN excluded.updated_at > students.updated_at THEN students.version + 1 ELSE students.version END " +
                "WHERE excluded.updated_at >= students.updated_at;";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (Student s : students) {
                pstmt.setString(1, s.getStudentCode());
                pstmt.setString(2, s.getFullName());
                pstmt.setString(3, s.getClassName());
                pstmt.setDouble(4, s.getGpa());
                pstmt.setLong(5, s.getUpdatedAt());
                pstmt.setInt(6, s.getVersion());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            System.err.println("Lỗi hợp nhất (merge) danh sách sinh viên: " + e.getMessage());
        }
    }

    public synchronized void replaceAllStudents(List<Student> students) {
        String deleteSql = "DELETE FROM students";
        String insertSql = "INSERT INTO students (student_code, full_name, class_name, gpa, updated_at, version) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(deleteSql);
            }
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                for (Student s : students) {
                    pstmt.setString(1, s.getStudentCode());
                    pstmt.setString(2, s.getFullName());
                    pstmt.setString(3, s.getClassName());
                    pstmt.setDouble(4, s.getGpa());
                    pstmt.setLong(5, s.getUpdatedAt());
                    pstmt.setInt(6, s.getVersion());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) {
            System.err.println("Lỗi nạp bản chụp toàn bộ (Full Snapshot): " + e.getMessage());
        }
    }
}
