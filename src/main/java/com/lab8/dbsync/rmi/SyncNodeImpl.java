package com.lab8.dbsync.rmi;

import com.lab8.dbsync.db.DatabaseManager;
import com.lab8.dbsync.model.NodeInfo;
import com.lab8.dbsync.model.Student;
import com.lab8.dbsync.model.SyncOperation;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SyncNodeImpl extends UnicastRemoteObject implements SyncNodeInterface {
    private static final long serialVersionUID = 1L;

    private final String nodeId;
    private final DatabaseManager db;
    private final List<NodeInfo> peers = new ArrayList<>();
    private final Map<String, SyncNodeInterface> peerCache = new ConcurrentHashMap<>();

    private final List<Consumer<String>> logListeners = new ArrayList<>();
    private final List<Runnable> dataChangeListeners = new ArrayList<>();

    public SyncNodeImpl(String nodeId, DatabaseManager db, int port) throws RemoteException {
        super(port);
        this.nodeId = nodeId;
        this.db = db;
    }

    public void addPeer(NodeInfo peer) {
        this.peers.add(peer);
    }

    public void addLogListener(Consumer<String> listener) {
        this.logListeners.add(listener);
    }

    public void addDataChangeListener(Runnable listener) {
        this.dataChangeListeners.add(listener);
    }

    private void log(String msg) {
        System.out.println("[" + nodeId + "] " + msg);
        for (Consumer<String> l : logListeners) {
            try {
                l.accept(msg);
            } catch (Exception ignored) {}
        }
    }

    private void notifyDataChanged() {
        for (Runnable r : dataChangeListeners) {
            try {
                r.run();
            } catch (Exception ignored) {}
        }
    }

    private SyncNodeInterface getPeerStub(NodeInfo peer) {
        try {
            SyncNodeInterface cached = peerCache.get(peer.getNodeId());
            if (cached != null) {
                try {
                    cached.ping();
                    return cached;
                } catch (Exception e) {
                    peerCache.remove(peer.getNodeId());
                }
            }

            Registry registry = LocateRegistry.getRegistry(peer.getHost(), peer.getPort());
            SyncNodeInterface stub = (SyncNodeInterface) registry.lookup(peer.getServiceName());
            peerCache.put(peer.getNodeId(), stub);
            return stub;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getNodeId() throws RemoteException {
        return this.nodeId;
    }

    @Override
    public boolean ping() throws RemoteException {
        return true;
    }

    @Override
    public synchronized boolean executeLocalOperation(SyncOperation op) throws RemoteException {
        log("Thực thi cục bộ: " + op);
        boolean ok = db.applySyncOperation(op);
        if (ok) {
            notifyDataChanged();

            // Phát lệnh nhân bản tới các Peer Server còn lại
            new Thread(() -> replicateToPeers(op)).start();
        }
        return ok;
    }

    private void replicateToPeers(SyncOperation op) {
        for (NodeInfo peer : peers) {
            try {
                SyncNodeInterface stub = getPeerStub(peer);
                if (stub != null) {
                    boolean success = stub.applyReplication(op);
                    if (success) {
                        log("✔ Đã đồng bộ thành công tới " + peer.getNodeId() + " (" + peer.getHost() + ":" + peer.getPort() + ")");
                    } else {
                        log("⚠ Đồng bộ tới " + peer.getNodeId() + " không thành công.");
                    }
                } else {
                    log("❌ " + peer.getNodeId() + " (" + peer.getHost() + ":" + peer.getPort() + ") hiện đang Offline. Sẽ bắt kịp sau.");
                }
            } catch (Exception e) {
                log("❌ Lỗi gửi nhân bản tới " + peer.getNodeId() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public synchronized boolean applyReplication(SyncOperation op) throws RemoteException {
        log("🔄 Nhận lệnh đồng bộ từ [" + op.getOriginNodeId() + "]: " + op.getOpType() + " SV=" + op.getStudentCode());
        boolean ok = db.applySyncOperation(op);
        if (ok) {
            notifyDataChanged();
        }
        return ok;
    }

    @Override
    public List<Student> getAllStudents() throws RemoteException {
        return db.getAllStudents();
    }

    @Override
    public Student getStudent(String studentCode) throws RemoteException {
        return db.getStudent(studentCode);
    }

    @Override
    public int getRecordCount() throws RemoteException {
        return db.getRecordCount();
    }

    @Override
    public List<SyncOperation> getMissedOperations(long lastTimestamp) throws RemoteException {
        return db.getOperationsSince(lastTimestamp);
    }

    @Override
    public List<Student> requestFullSnapshot() throws RemoteException {
        return db.getAllStudents();
    }

    @Override
    public synchronized void syncMissedFromPeers() throws RemoteException {
        log("🔄 Đang kiểm tra và bắt kịp dữ liệu từ các Peer Server...");
        boolean synced = false;

        for (NodeInfo peer : peers) {
            try {
                SyncNodeInterface stub = getPeerStub(peer);
                if (stub != null) {
                    List<Student> snapshot = stub.requestFullSnapshot();
                    if (snapshot != null) {
                        log("Nhận snapshot " + snapshot.size() + " bản ghi từ " + peer.getNodeId() + ". Đang áp dụng...");
                        db.replaceAllStudents(snapshot);
                        notifyDataChanged();
                        synced = true;
                        log("✅ Đồng bộ bắt kịp thành công từ " + peer.getNodeId() + "!");
                        break;
                    }
                }
            } catch (Exception e) {
                log("Không thể kéo dữ liệu từ " + peer.getNodeId() + ": " + e.getMessage());
            }
        }

        if (!synced) {
            log("ℹ Không có Peer Server nào phản hồi hoặc dữ liệu đã đồng bộ.");
        }
    }
}
