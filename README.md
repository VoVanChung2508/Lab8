# HƯỚNG DẪN THỰC HIỆN LAB 8: LẬP TRÌNH MẠNG
**Đề tài:**
1. **Xây dựng chương trình Chat bằng RMI**
2. **Xây dựng chương trình đồng bộ hóa 3 Cơ sở dữ liệu (CSDL) trên 3 Server**
*(Môi trường triển khai: Server chạy trên 2 máy thực và các Client chạy trên các máy ảo)*

---

## 📑 MỤC LỤC
1. [Tổng quan kiến trúc](#1-tổng-quan-kiến-trúc)
2. [Cấu trúc thư mục dự án](#2-cấu-trúc-thư-mục-dự-án)
3. [Cấu hình mạng (Máy thực & Máy ảo)](#3-cấu-hình-mạng-máy-thực--máy-ảo)
4. [Hướng dẫn chạy Bài 1: RMI Chat Room](#4-hướng-dẫn-chạy-bài-1-rmi-chat-room)
5. [Hướng dẫn chạy Bài 2: Đồng bộ hóa 3 CSDL](#5-hướng-dẫn-chạy-bài-2-đồng-bộ-hóa-3-csdl)
6. [Kịch bản kiểm thử & chấm điểm cho giảng viên](#6-kịch-bản-kiểm-thử--chấm-điểm-cho-giảng-viên)

---

## 1. TỔNG QUAN KIẾN TRÚC

### 💬 Phần 1: Chương trình Chat bằng Java RMI
- **Mô hình RMI Callback thời gian thực**: 
  - `ChatServerInterface`: Server đăng ký client, nhận tin broadcast/private và forward.
  - `ChatClientInterface`: Callback interface trên Client. Khi Server nhận tin nhắn, Server sẽ tự động gọi ngược lại client method `receiveMessage()` để đẩy tin nhắn tức thời (Push notification), không cần polling.
- **Hỗ trợ Cụm 2 Server trên 2 máy thực (Server Federation)**:
  - Máy thực 1 chạy `Server-1`.
  - Máy thực 2 chạy `Server-2`.
  - Hai server liên kết với nhau qua RMI. Khi client trên Máy thực 1 nhắn tin, server sẽ vừa gửi cho client nội bộ vừa chuyển tiếp sang `Server-2` để client trên Máy thực 2/Máy ảo nhận được.
- **Tính năng nổi bật**:
  - Giao diện FlatLaf hiện đại, trực quan (Dark Mode).
  - Chat nhóm công khai (Public) và Chat riêng tư (Private 1-1).
  - Tự động phát hiện IP LAN (`NetworkUtils.setRmiHostname`) để tránh lỗi RMI callback khi chạy qua máy ảo.

### 🗄️ Phần 2: Đồng bộ hóa 3 CSDL trên 3 Server (Multi-Master Database Replication)
- **3 Server Node độc lập**:
  - `Server-1` (chạy trên Máy thực 1, quản lý CSDL SQLite `server1_db.sqlite`).
  - `Server-2` (chạy trên Máy thực 2, quản lý CSDL SQLite `server2_db.sqlite`).
  - `Server-3` (chạy trên Máy ảo, quản lý CSDL SQLite `server3_db.sqlite`).
- **Cơ chế đồng bộ phân tán (Distributed Replication)**:
  1. Khi có thao tác Thêm / Sửa / Xóa (INSERT / UPDATE / DELETE) sinh viên từ bất kỳ Server nào (hoặc từ Client máy ảo gửi tới 1 Server):
     - Server tiếp nhận sẽ ghi vào CSDL cục bộ và sinh mã giao dịch duy nhất `opId`.
     - Server gọi hàm RMI `applyReplication(op)` tới 2 Server đối tác còn lại.
     - 2 Server nhận lệnh sẽ kiểm tra tính lũy biến (Idempotent - tránh trùng lặp) và áp dụng vào CSDL của mình.
     - Cơ chế chặn lặp (Replication Loop Prevention): Server nhận replication sẽ không chuyển tiếp tiếp.
  2. **Cơ chế phục hồi & bắt kịp (Eventual Consistency & Catch-Up Sync)**:
     - Khi 1 server bị mất mạng hoặc tắt đi, 2 server kia vẫn tiếp tục ghi nhận dữ liệu.
     - Khi server đó bật lại, hệ thống có nút **"🔄 Bắt kịp dữ liệu (Catch-Up)"** để tự động kéo toàn bộ bản ghi còn thiếu từ các server đang sống về CSDL của mình, đảm bảo 100% dữ liệu đồng nhất.
- **Bảng điều khiển 3 CSDL (Unified Monitor Dashboard)**:
  - Cho phép quan sát dữ liệu của cả 3 CSDL trực tiếp trên 1 màn hình với 3 cột độc lập.
  - Tự động kiểm tra Checksum và báo trạng thái `🟢 ĐỒNG BỘ HOÀN TOÀN 100%`.

---

## 2. CẤU TRÚC THƯ MỤC DỰ ÁN

```
d:/lab7/lab8/
├── pom.xml                               # File cấu hình Maven & Shading Fat JAR
├── target/lab8-1.0-SNAPSHOT.jar          # File JAR đóng gói hoàn chỉnh (All-in-One)
├── run-launcher.bat                      # Bảng điều khiển khởi chạy tổng hợp
├── run-chat-server.bat                   # Khởi chạy Chat Server
├── run-chat-client.bat                   # Khởi chạy Chat Client
├── run-db-node1.bat                      # Khởi chạy DB Server Node 1 (Port 1099)
├── run-db-node2.bat                      # Khởi chạy DB Server Node 2 (Port 1100)
├── run-db-node3.bat                      # Khởi chạy DB Server Node 3 (Port 1101)
├── run-db-dashboard.bat                  # Bảng giám sát đồng bộ 3 CSDL thời gian thực
├── run-db-client.bat                     # Client thao tác CSDL dành cho Máy ảo
├── run.sh                                # Shell script chạy trên máy ảo Linux/Ubuntu
└── src/main/java/com/lab8/
    ├── MainLauncher.java                 # Giao diện Launcher chính
    ├── common/
    │   ├── NetworkUtils.java             # Phát hiện IP LAN & cấu hình java.rmi.server.hostname
    │   └── UITheme.java                  # Giao diện đồ họa FlatLaf
    ├── chat/                             # BÀI 1: RMI CHAT
    │   ├── model/ (ChatMessage, MessageType)
    │   ├── rmi/ (ChatServerInterface, ChatClientInterface, ChatServerImpl, ChatClientImpl)
    │   ├── server/ (ChatServerApp, ChatServerWindow)
    │   └── client/ (ChatClientApp, ChatClientWindow)
    └── dbsync/                           # BÀI 2: ĐỒNG BỘ 3 CSDL
        ├── model/ (Student, SyncOperation, OperationType, NodeInfo)
        ├── db/ (DatabaseManager - SQLite JDBC)
        ├── rmi/ (SyncNodeInterface, SyncNodeImpl)
        ├── server/ (SyncServerApp, SyncServerWindow)
        ├── client/ (SyncClientApp, SyncClientWindow)
        └── monitor/ (SyncDashboardApp, SyncDashboardWindow)
```

---

## 3. CẤU HÌNH MẠNG (MÁY THỰC & MÁY ẢO)

Để các chương trình RMI giao tiếp được giữa **2 Máy thực** và các **Máy ảo (VMware / VirtualBox)**:

1. **Chế độ mạng máy ảo (Network Adapter)**:
   - Trong VMware / VirtualBox: Chuyển card mạng của Máy ảo sang chế độ **Bridged Adapter** (hoặc **Host-Only Adapter** nếu 2 máy thực nối chung switch/mạng nội bộ).
   - Khi đó máy ảo sẽ nhận được địa chỉ IP cùng dải mạng với máy thực (ví dụ: `192.168.1.x`).
2. **Kiểm tra kết nối**:
   - Mở CMD trên máy thực gõ: `ipconfig` để lấy IP máy thực (ví dụ: `192.168.1.10`).
   - Mở Terminal/CMD trên máy ảo gõ `ping 192.168.1.10` để đảm bảo thông mạng.
3. **Tường lửa (Windows Firewall)**:
   - Trên các máy thực chạy Server, hãy tắt tường lửa tạm thời hoặc mở port `1099`, `1100`, `1101` cho Java:
     ```powershell
     netsh advfirewall firewall add rule name="Java RMI" dir=in action=allow protocol=TCP localport=1099,1100,1101
     ```

---

## 4. HƯỚNG DẪN CHẠY BÀI 1: RMI CHAT ROOM

### Cách 1: Sử dụng Launcher (Khuyên dùng)
Double click vào file `run-launcher.bat`. Chọn:
- **🚀 Khởi Chạy Chat Server** trên máy thực.
- **💬 Khởi Chạy Chat Client** trên máy ảo hoặc máy thực khác.

### Cách 2: Chạy trực tiếp qua file `.bat`
- **Trên Máy thực 1**: Chạy `run-chat-server.bat`. 
  - Chọn IP máy thực 1, Port 1099. Bấm **▶ Khởi Động Server**.
- **Trên Máy thực 2 (nếu có 2 server)**: Chạy `run-chat-server.bat`.
  - Đổi Server ID thành `Server-2`, Port 1099. Bấm **▶ Khởi Động Server**.
  - Tại ô *Liên kết Server Máy thực 2*, điền IP của Máy thực 1 và bấm **🔗 Kết Nối Liên Server**.
- **Trên các Máy ảo**:
  - Copy file `target/lab8-1.0-SNAPSHOT.jar` và `run-chat-client.bat` sang máy ảo.
  - Chạy `run-chat-client.bat`.
  - Nhập **IP Server (Máy thực)**, nhập Tên người dùng (ví dụ: *Client_VM1*), chọn IP của máy ảo tại mục IP Callback, bấm **OK**.
  - Bắt đầu chat! Bấm đúp vào tên người dùng khác trên danh sách để chat riêng tư (1-1).

---

## 5. HƯỚNG DẪN CHẠY BÀI 2: ĐỒNG BỘ HÓA 3 CSDL

### Mô hình triển khai:
- **Server 1 (Máy thực 1)**: Port `1099`, CSDL `server1_db.sqlite`.
- **Server 2 (Máy thực 2)**: Port `1100` (hoặc 1099 trên máy 2), CSDL `server2_db.sqlite`.
- **Server 3 (Máy ảo)**: Port `1101` (hoặc 1099 trên máy ảo), CSDL `server3_db.sqlite`.

### Các bước thực hiện:
1. **Khởi động 3 Server**:
   - Chạy `run-db-node1.bat` trên Máy thực 1.
   - Chạy `run-db-node2.bat` trên Máy thực 2 (hoặc máy thực 1 khác port để demo).
   - Chạy `run-db-node3.bat` trên Máy ảo.
   *(Điền địa chỉ IP của 2 server đối tác vào khung "Kết Nối 2 Server Đối Tác" và bấm Khởi Động).*

2. **Mở Bảng Giám Sát Đồng Bộ (Dashboard)**:
   - Chạy `run-db-dashboard.bat`.
   - Bảng điều khiển sẽ hiển thị song song dữ liệu của 3 CSDL.
   - Nhập thông tin sinh viên (*Mã SV, Họ tên, Lớp, GPA*), chọn gửi vào **Server 1**, bấm **➕ Thêm**.
   - Quan sát: Cả 3 bảng CSDL trên 3 Server lập tức xuất hiện bản ghi mới cùng lúc! Trạng thái báo `🟢 ĐỒNG BỘ HOÀN HẢO 100%`.

3. **Chạy Client trên Máy ảo**:
   - Chạy `run-db-client.bat` trên Máy ảo.
   - Kết nối tới bất kỳ Server nào (Server 1, Server 2 hoặc Server 3).
   - Thêm/Sửa/Xóa sinh viên từ máy ảo ➔ CSDL trên 2 máy thực lập tức cập nhật tương ứng!

---

## 6. KỊCH BẢN KIỂM THỬ & CHẤM ĐIỂM CHO GIẢNG VIÊN

| Bước | Hành động | Kết quả mong đợi | Đánh giá |
|---|---|---|---|
| **1** | Khởi động Chat Server trên máy thực, mở 2 Chat Client trên máy ảo và máy thực. | Kết nối thành công, danh sách Online cập nhật đúng tên. | Đạt |
| **2** | Client A gửi tin công khai. | Client B nhận tin ngay lập tức (RMI Callback push notification). | Đạt |
| **3** | Client A click vào Client B chọn gửi tin nhắn riêng tư. | Chỉ Client B và Client A nhận được tin (màu cam/tím), người khác không thấy. | Đạt |
| **4** | Mở Bảng giám sát 3 CSDL (`run-db-dashboard.bat`). Thêm sinh viên `SV001` tại Server 1. | Dữ liệu lập tức xuất hiện ở Server 2 và Server 3. | Đạt |
| **5** | Từ Client trên máy ảo, gửi lệnh sửa điểm của `SV001` tới Server 3. | CSDL trên Máy thực 1 và Máy thực 2 cập nhật điểm mới ngay tức khắc. | Đạt |
| **6** | **Thử nghiệm chịu lỗi & Bắt kịp (Fault Tolerance & Catch-Up)**:<br>1. Tắt Server 3.<br>2. Thêm sinh viên `SV002` vào Server 1.<br>3. Bật lại Server 3.<br>4. Bấm "🔄 Bắt kịp dữ liệu". | Server 3 tự động kéo bản ghi `SV002` về CSDL của mình, cả 3 CSDL lại đồng nhất 100%. | Điểm tối đa |
