package com.lab8.dbsync.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class SyncOperation implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final String opId;
    private final String originNodeId;
    private final OperationType opType;
    private final String studentCode;
    private final Student student;
    private final long timestamp;

    public SyncOperation(String originNodeId, OperationType opType, String studentCode, Student student) {
        this.originNodeId = originNodeId;
        this.opType = opType;
        this.studentCode = studentCode;
        this.student = student;
        this.timestamp = System.currentTimeMillis();
        this.opId = "OP-" + originNodeId + "-" + this.timestamp + "-" + UUID.randomUUID().toString().substring(0, 6);
    }

    public SyncOperation(String opId, String originNodeId, OperationType opType, String studentCode, Student student, long timestamp) {
        this.opId = opId;
        this.originNodeId = originNodeId;
        this.opType = opType;
        this.studentCode = studentCode;
        this.student = student;
        this.timestamp = timestamp;
    }

    public static SyncOperation insertOp(String originNodeId, Student student) {
        return new SyncOperation(originNodeId, OperationType.INSERT, student.getStudentCode(), student);
    }

    public static SyncOperation updateOp(String originNodeId, Student student) {
        return new SyncOperation(originNodeId, OperationType.UPDATE, student.getStudentCode(), student);
    }

    public static SyncOperation deleteOp(String originNodeId, String studentCode) {
        return new SyncOperation(originNodeId, OperationType.DELETE, studentCode, null);
    }

    public String getOpId() {
        return opId;
    }

    public String getOriginNodeId() {
        return originNodeId;
    }

    public OperationType getOpType() {
        return opType;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public Student getStudent() {
        return student;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedTime() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).format(FORMATTER);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s từ %s: Mã SV=%s", getFormattedTime(), opType, originNodeId, studentCode);
    }
}
