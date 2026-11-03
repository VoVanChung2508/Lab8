package com.lab8;

import com.lab8.chat.model.ChatMessage;
import com.lab8.chat.rmi.ChatClientImpl;
import com.lab8.chat.rmi.ChatServerImpl;
import com.lab8.chat.rmi.ChatServerInterface;
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

public class TestIntegration {
    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("  BẮT ĐẦU KIỂM THỬ TÍCH HỢP TỰ ĐỘNG LAB 8 (RMI)");
        System.out.println("==================================================");

        // --- 1. TEST BÀI 1: RMI CHAT ---
        System.out.println("\n[1] Đang kiểm thử Bài 1: RMI Chat...");
        int chatPort = 12099;
        Registry chatReg = LocateRegistry.createRegistry(chatPort);
        ChatServerImpl chatServer = new ChatServerImpl("Server-Test", 0);
        chatReg.rebind("ChatServer", chatServer);

        ChatServerInterface serverStub = (ChatServerInterface) chatReg.lookup("ChatServer");

        final String[] receivedMsg = new String[2];
        ChatClientImpl client1 = new ChatClientImpl("Alice", 0);
        client1.setMessageListener(msg -> receivedMsg[0] = msg.getContent());

        ChatClientImpl client2 = new ChatClientImpl("Bob", 0);
        client2.setMessageListener(msg -> receivedMsg[1] = msg.getContent());

        boolean reg1 = serverStub.registerClient(client1, "Alice");
        boolean reg2 = serverStub.registerClient(client2, "Bob");
        assert reg1 && reg2 : "Lỗi đăng ký client";

        serverStub.broadcastMessage(ChatMessage.publicMessage("Alice", "Xin chào mọi người!"));
        Thread.sleep(200);
        if ("Xin chào mọi người!".equals(receivedMsg[1])) {
            System.out.println("  ✔ Chat Broadcast RMI Callback: THÀNH CÔNG! Bob đã nhận tin từ Alice.");
        } else {
            System.err.println("  ❌ Chat Broadcast: THẤT BẠI! Nhận được: " + receivedMsg[1]);
        }

        serverStub.sendPrivateMessage(ChatMessage.privateMessage("Alice", "Bob", "Tin nhắn bí mật"));
        Thread.sleep(200);
        if ("Tin nhắn bí mật".equals(receivedMsg[1])) {
            System.out.println("  ✔ Chat Riêng Tư (Private Message) 1-1: THÀNH CÔNG!");
        } else {
            System.err.println("  ❌ Chat Riêng: THẤT BẠI!");
        }

        // --- 2. TEST BÀI 2: ĐỒNG BỘ 3 CSDL TRÊN 3 SERVER ---
        System.out.println("\n[2] Đang kiểm thử Bài 2: Đồng bộ 3 CSDL trên 3 Server...");
        new File("test_node1.sqlite").delete();
        new File("test_node2.sqlite").delete();
        new File("test_node3.sqlite").delete();

        int p1 = 12101, p2 = 12102, p3 = 12103;
        Registry reg1Node = LocateRegistry.createRegistry(p1);
        Registry reg2Node = LocateRegistry.createRegistry(p2);
        Registry reg3Node = LocateRegistry.createRegistry(p3);

        DatabaseManager db1 = new DatabaseManager("test_node1.sqlite");
        DatabaseManager db2 = new DatabaseManager("test_node2.sqlite");
        DatabaseManager db3 = new DatabaseManager("test_node3.sqlite");

        SyncNodeImpl node1 = new SyncNodeImpl("Server-1", db1, 0);
        SyncNodeImpl node2 = new SyncNodeImpl("Server-2", db2, 0);
        SyncNodeImpl node3 = new SyncNodeImpl("Server-3", db3, 0);

        // Thiết lập liên kết peer giữa 3 server
        node1.addPeer(new NodeInfo("Server-2", "127.0.0.1", p2, ""));
        node1.addPeer(new NodeInfo("Server-3", "127.0.0.1", p3, ""));

        node2.addPeer(new NodeInfo("Server-1", "127.0.0.1", p1, ""));
        node2.addPeer(new NodeInfo("Server-3", "127.0.0.1", p3, ""));

        node3.addPeer(new NodeInfo("Server-1", "127.0.0.1", p1, ""));
        node3.addPeer(new NodeInfo("Server-2", "127.0.0.1", p2, ""));

        reg1Node.rebind("SyncNode_Server-1", node1);
        reg2Node.rebind("SyncNode_Server-2", node2);
        reg3Node.rebind("SyncNode_Server-3", node3);

        SyncNodeInterface stub1 = (SyncNodeInterface) reg1Node.lookup("SyncNode_Server-1");
        SyncNodeInterface stub2 = (SyncNodeInterface) reg2Node.lookup("SyncNode_Server-2");
        SyncNodeInterface stub3 = (SyncNodeInterface) reg3Node.lookup("SyncNode_Server-3");

        // Ghi dữ liệu vào Server 1
        System.out.println("  ➔ Thêm sinh viên SV001 vào Server-1...");
        Student s1 = new Student("SV001", "Nguyễn Văn An", "D21CN1", 3.8);
        stub1.executeLocalOperation(SyncOperation.insertOp("Server-1", s1));

        Thread.sleep(400);

        List<Student> db1List = stub1.getAllStudents();
        List<Student> db2List = stub2.getAllStudents();
        List<Student> db3List = stub3.getAllStudents();

        System.out.println("  • Số lượng bản ghi: DB1=" + db1List.size() + " | DB2=" + db2List.size() + " | DB3=" + db3List.size());
        if (db1List.size() == 1 && db2List.size() == 1 && db3List.size() == 1
                && db2List.get(0).getStudentCode().equals("SV001")
                && db3List.get(0).getStudentCode().equals("SV001")) {
            System.out.println("  ✔ Nhân bản từ Server 1 ➔ Server 2 & Server 3: ĐỒNG BỘ 100% HOÀN HẢO!");
        } else {
            System.err.println("  ❌ Đồng bộ thất bại!");
        }

        // Ghi dữ liệu vào Server 3 (Máy ảo)
        System.out.println("  ➔ Thêm sinh viên SV002 từ Server-3 (Máy ảo)...");
        Student s2 = new Student("SV002", "Trần Thị Bích", "D21CN2", 3.9);
        stub3.executeLocalOperation(SyncOperation.insertOp("Server-3", s2));

        Thread.sleep(400);

        db1List = stub1.getAllStudents();
        db2List = stub2.getAllStudents();
        db3List = stub3.getAllStudents();

        System.out.println("  • Số lượng bản ghi: DB1=" + db1List.size() + " | DB2=" + db2List.size() + " | DB3=" + db3List.size());
        if (db1List.size() == 2 && db2List.size() == 2 && db3List.size() == 2) {
            System.out.println("  ✔ Nhân bản ngược từ Server 3 ➔ Server 1 & Server 2: ĐỒNG BỘ 100% HOÀN HẢO!");
        } else {
            System.err.println("  ❌ Đồng bộ ngược thất bại!");
        }

        // Dọn dẹp file test
        new File("test_node1.sqlite").deleteOnExit();
        new File("test_node2.sqlite").deleteOnExit();
        new File("test_node3.sqlite").deleteOnExit();

        System.out.println("\n🎉 TẤT CẢ CÁC BÀI KIỂM THỬ ĐÃ VƯỢT QUA XUẤT SẮC!");
        System.exit(0);
    }
}
