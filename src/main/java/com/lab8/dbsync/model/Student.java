package com.lab8.dbsync.model;

import java.io.Serializable;
import java.util.Objects;

public class Student implements Serializable {
    private static final long serialVersionUID = 1L;

    private String studentCode; // Mã sinh viên (Khóa chính)
    private String fullName;    // Họ và tên
    private String className;   // Lớp
    private double gpa;         // Điểm trung bình (GPA)
    private long updatedAt;     // Thời gian cập nhật gần nhất (epoch ms)
    private int version;        // Phiên bản bản ghi

    public Student() {
    }

    public Student(String studentCode, String fullName, String className, double gpa) {
        this.studentCode = studentCode;
        this.fullName = fullName;
        this.className = className;
        this.gpa = gpa;
        this.updatedAt = System.currentTimeMillis();
        this.version = 1;
    }

    public Student(String studentCode, String fullName, String className, double gpa, long updatedAt, int version) {
        this.studentCode = studentCode;
        this.fullName = fullName;
        this.className = className;
        this.gpa = gpa;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public double getGpa() {
        return gpa;
    }

    public void setGpa(double gpa) {
        this.gpa = gpa;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Student student = (Student) o;
        return Double.compare(student.gpa, gpa) == 0 &&
                Objects.equals(studentCode, student.studentCode) &&
                Objects.equals(fullName, student.fullName) &&
                Objects.equals(className, student.className);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentCode, fullName, className, gpa);
    }

    @Override
    public String toString() {
        return "Student{" +
                "code='" + studentCode + '\'' +
                ", name='" + fullName + '\'' +
                ", class='" + className + '\'' +
                ", gpa=" + gpa +
                ", ver=" + version +
                '}';
    }
}
