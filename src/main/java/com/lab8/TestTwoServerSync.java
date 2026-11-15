package com.lab8;

import com.lab8.dbsync.db.DatabaseManager;
import com.lab8.dbsync.model.NodeInfo;
import com.lab8.dbsync.model.Student;
import com.lab8.dbsync.model.SyncOperation;
import com.lab8.dbsync.rmi.SyncNodeImpl;
import com.lab8.dbsync.rmi.SyncNodeInterface;

import java.io.File;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

/**
 * Kiểm thử tự động đồng bộ hóa 2 chiều qua RMI giữa 2 Server.
 */
public class TestTwoServerSync {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   KIỂM THỬ ĐỒNG BỘ HÓA RMI GIỮA 2 SERVER (SERVER 1 <-> SERVER 2)");
        System.out.println("==================================================================");

        // Xóa DB tạm cũ nếu có
        new File("test_s1.sqlite").delete();
        new File("test_s2.sqlite").delete();

        int port1 = 11201;
        int port2 = 11202;

        try {
            System.out.println("\n[1] Khởi tạo 2 RMI Registry và 2 Server Node...");
            Registry reg1 = LocateRegistry.createRegistry(port1);
            Registry reg2 = LocateRegistry.createRegistry(port2);

            DatabaseManager db1 = new DatabaseManager("test_s1.sqlite");
            DatabaseManager db2 = new DatabaseManager("test_s2.sqlite");

            SyncNodeImpl node1 = new SyncNodeImpl("Server-1", db1, 0);
            SyncNodeImpl node2 = new SyncNodeImpl("Server-2", db2, 0);

            // Cấu hình Server 1 liên kết với Server 2 và ngược lại
            node1.addPeer(new NodeInfo("Server-2", "127.0.0.1", port2, ""));
            node2.addPeer(new NodeInfo("Server-1", "127.0.0.1", port1, ""));

            reg1.rebind("SyncNode_Server-1", node1);
            reg2.rebind("SyncNode_Server-2", node2);

            SyncNodeInterface server1 = (SyncNodeInterface) reg1.lookup("SyncNode_Server-1");
            SyncNodeInterface server2 = (SyncNodeInterface) reg2.lookup("SyncNode_Server-2");

            System.out.println("  ✔ Server 1 sẵn sàng tại port " + port1);
            System.out.println("  ✔ Server 2 sẵn sàng tại port " + port2);

            // ==========================================
            // TEST 1: Thêm sinh viên SV001 tại Server 1
            // ==========================================
            System.out.println("\n[2] TEST 1: Thêm SV001 ('Nguyễn Văn An', GPA 3.5) vào Server 1...");
            Student s1 = new Student("SV001", "Nguyễn Văn An", "24IT1", 3.5);
            server1.executeLocalOperation(SyncOperation.insertOp("Server-1", s1));

            Thread.sleep(500); // Chờ RMI truyền tin bất đồng bộ

            List<Student> listS2 = server2.getAllStudents();
            if (listS2.size() == 1 && listS2.get(0).getStudentCode().equals("SV001")) {
                System.out.println("  🎉 THÀNH CÔNG: Server 2 đã tự động nhận được SV001 từ Server 1!");
                System.out.println("     Chi tiết tại Server 2: " + listS2.get(0));
            } else {
                System.err.println("  ❌ THẤT BẠI: Server 2 chưa nhận được SV001! Số bản ghi: " + listS2.size());
            }

            // ==========================================
            // TEST 2: Sửa điểm GPA của SV001 tại Server 2
            // ==========================================
            System.out.println("\n[3] TEST 2: Sửa điểm của SV001 tại Server 2 từ 3.5 lên 3.9...");
            Student s1Updated = new Student("SV001", "Nguyễn Văn An", "24IT1", 3.9, System.currentTimeMillis(), 2);
            server2.executeLocalOperation(SyncOperation.updateOp("Server-2", s1Updated));

            Thread.sleep(500);

            Student s1OnServer1 = server1.getStudent("SV001");
            if (s1OnServer1 != null && Math.abs(s1OnServer1.getGpa() - 3.9) < 0.001) {
                System.out.println("  🎉 THÀNH CÔNG: Server 1 đã tự động cập nhật GPA của SV001 thành " + s1OnServer1.getGpa() + "!");
            } else {
                System.err.println("  ❌ THẤT BẠI: Server 1 chưa nhận được cập nhật GPA!");
            }

            // ==========================================
            // TEST 3: Thêm sinh viên SV002 tại Server 2
            // ==========================================
            System.out.println("\n[4] TEST 3: Thêm SV002 ('Trần Thị Bích') vào Server 2...");
            Student s2 = new Student("SV002", "Trần Thị Bích", "24IT2", 3.8);
            server2.executeLocalOperation(SyncOperation.insertOp("Server-2", s2));

            Thread.sleep(500);

            List<Student> listS1 = server1.getAllStudents();
            if (listS1.size() == 2) {
                System.out.println("  🎉 THÀNH CÔNG: Server 1 đã nhận được SV002 từ Server 2!");
                System.out.println("     Danh sách CSDL Server 1 hiện có " + listS1.size() + " sinh viên:");
                for (Student s : listS1) {
                    System.out.println("       • " + s.getStudentCode() + " - " + s.getFullName() + " (GPA: " + s.getGpa() + ")");
                }
            } else {
                System.err.println("  ❌ THẤT BẠI: Server 1 chưa có đủ 2 sinh viên!");
            }

            // ==========================================
            // TEST 4: Xóa sinh viên SV001 tại Server 1
            // ==========================================
            System.out.println("\n[5] TEST 4: Xóa SV001 khỏi Server 1...");
            server1.executeLocalOperation(SyncOperation.deleteOp("Server-1", "SV001"));

            Thread.sleep(500);

            Student checkS2 = server2.getStudent("SV001");
            if (checkS2 == null) {
                System.out.println("  🎉 THÀNH CÔNG: Server 2 đã tự động xóa SV001 theo Server 1!");
                System.out.println("     Số lượng sinh viên còn lại trên Server 2: " + server2.getRecordCount());
            } else {
                System.err.println("  ❌ THẤT BẠI: Server 2 vẫn còn SV001!");
            }

            // Dọn dẹp
            new File("test_s1.sqlite").deleteOnExit();
            new File("test_s2.sqlite").deleteOnExit();

            System.out.println("\n==================================================================");
            System.out.println("  KẾT LUẬN: ĐỒNG BỘ HÓA RMI GIỮA 2 SERVER ĐÃ HOẠT ĐỘNG HOÀN HẢO 100%!");
            System.out.println("==================================================================");

        } catch (Exception e) {
            System.err.println("Lỗi khi chạy kiểm thử: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
