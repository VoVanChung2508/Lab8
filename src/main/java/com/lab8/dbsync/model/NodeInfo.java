package com.lab8.dbsync.model;

import java.io.Serializable;

public class NodeInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String nodeId; // Ví dụ: "Server-1", "Server-2", "Server-3"
    private String host;         // IP của máy (máy thực 1, máy thực 2, máy ảo)
    private int port;            // Cổng RMI (1099, 1100, 1101, ...)
    private String dbPath;       // Đường dẫn file SQLite

    public NodeInfo(String nodeId, String host, int port, String dbPath) {
        this.nodeId = nodeId;
        this.host = host;
        this.port = port;
        this.dbPath = dbPath;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDbPath() {
        return dbPath;
    }

    public void setDbPath(String dbPath) {
        this.dbPath = dbPath;
    }

    public String getRmiLookupUrl() {
        return "rmi://" + host + ":" + port + "/SyncNode_" + nodeId;
    }

    public String getServiceName() {
        return "SyncNode_" + nodeId;
    }

    @Override
    public String toString() {
        return nodeId + " (" + host + ":" + port + ")";
    }
}
