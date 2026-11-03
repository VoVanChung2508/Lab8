package com.lab8.dbsync.rmi;

import com.lab8.dbsync.model.Student;
import com.lab8.dbsync.model.SyncOperation;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface SyncNodeInterface extends Remote {

    /**
     * Lấy ID của Server Node (ví dụ: "Server-1", "Server-2", "Server-3").
     */
    String getNodeId() throws RemoteException;

    /**
     * Kiểm tra trạng thái sống của Server (Health Check / Heartbeat).
     */
    boolean ping() throws RemoteException;

    /**
     * Thực thi một thao tác dữ liệu khởi nguồn từ Node này (từ Client hoặc Local Form).
     * Server sẽ lưu vào CSDL cục bộ và đồng bộ hóa tức thời sang 2 Server còn lại!
     */
    boolean executeLocalOperation(SyncOperation op) throws RemoteException;

    /**
     * Nhận lệnh đồng bộ hóa được gửi từ Server đối tác (Replication).
     * Áp dụng vào CSDL cục bộ và không chuyển tiếp tiếp (ngăn vòng lặp).
     */
    boolean applyReplication(SyncOperation op) throws RemoteException;

    /**
     * Lấy danh sách toàn bộ sinh viên có trong CSDL của Server này.
     */
    List<Student> getAllStudents() throws RemoteException;

    /**
     * Lấy thông tin chi tiết một sinh viên theo mã.
     */
    Student getStudent(String studentCode) throws RemoteException;

    /**
     * Lấy tổng số lượng bản ghi có trong CSDL của Server.
     */
    int getRecordCount() throws RemoteException;

    /**
     * Lấy các bản ghi đồng bộ đã phát sinh sau một mốc thời gian (dành cho server vừa phục hồi).
     */
    List<SyncOperation> getMissedOperations(long lastTimestamp) throws RemoteException;

    /**
     * Lấy bản chụp toàn bộ dữ liệu (Full Snapshot) để chữa lành dữ liệu khi lệch.
     */
    List<Student> requestFullSnapshot() throws RemoteException;

    /**
     * Kích hoạt tự động kéo dữ liệu còn thiếu từ 2 Server còn lại để bắt kịp.
     */
    void syncMissedFromPeers() throws RemoteException;
}
