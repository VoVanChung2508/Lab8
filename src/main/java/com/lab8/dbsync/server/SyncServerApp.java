package com.lab8.dbsync.server;

import com.lab8.common.NetworkUtils;
import com.lab8.dbsync.db.DatabaseManager;
import com.lab8.dbsync.model.NodeInfo;
import com.lab8.dbsync.rmi.SyncNodeImpl;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class SyncServerApp {

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("--cli")) {
            // Mode CLI (Headless Linux / VM server)
            String nodeId = args.length > 1 ? args[1] : "Server-1";
            int port = args.length > 2 ? Integer.parseInt(args[2]) : 1099;
            String hostIp = args.length > 3 ? args[3] : NetworkUtils.getPreferredLocalIp();
            String dbPath = args.length > 4 ? args[4] : (nodeId.toLowerCase() + "_db.sqlite");

            System.out.println("=== KHỞI ĐỘNG CSDL SYNC NODE (CLI MODE) ===");
            System.out.println("Node: " + nodeId + " | Port: " + port + " | IP: " + hostIp + " | DB: " + dbPath);

            NetworkUtils.setRmiHostname(hostIp);

            try {
                Registry registry;
                try {
                    registry = LocateRegistry.createRegistry(port);
                } catch (Exception e) {
                    registry = LocateRegistry.getRegistry(port);
                }

                DatabaseManager db = new DatabaseManager(dbPath);
                SyncNodeImpl node = new SyncNodeImpl(nodeId, db, 0);

                // Thêm peers từ args nếu có: --peer Node-2 192.168.1.101 1100 --peer Node-3 192.168.1.102 1101
                for (int i = 5; i < args.length; i++) {
                    if (args[i].equalsIgnoreCase("--peer") && i + 3 < args.length) {
                        String peerId = args[i + 1];
                        String peerHost = args[i + 2];
                        int peerPort = Integer.parseInt(args[i + 3]);
                        node.addPeer(new NodeInfo(peerId, peerHost, peerPort, ""));
                        i += 3;
                    }
                }

                String serviceName = "SyncNode_" + nodeId;
                registry.rebind(serviceName, node);

                System.out.println("✅ Sync Node [" + nodeId + "] đã sẵn sàng tại rmi://" + hostIp + ":" + port + "/" + serviceName);
                System.out.println("Nhấn Ctrl+C để dừng.");

                // Thử catch-up
                Thread.sleep(1000);
                node.syncMissedFromPeers();

            } catch (Exception e) {
                System.err.println("Lỗi khởi động Sync Node: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // GUI Mode
            SyncServerWindow.main(args);
        }
    }
}
