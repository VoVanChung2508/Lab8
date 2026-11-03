package com.lab8.common;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class NetworkUtils {

    /**
     * Lấy danh sách tất cả các địa chỉ IPv4 không phải loopback của máy.
     */
    public static List<String> getAvailableIpv4Addresses() {
        List<String> ipList = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        String hostAddress = addr.getHostAddress();
                        if (!ipList.contains(hostAddress)) {
                            ipList.add(hostAddress);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi tìm danh sách IP mạng: " + e.getMessage());
        }

        if (ipList.isEmpty()) {
            ipList.add("127.0.0.1");
        }
        return ipList;
    }

    /**
     * Chọn IP phù hợp nhất cho mạng LAN (ưu tiên 192.168.x.x, 10.x.x.x, 172.16.x.x).
     */
    public static String getPreferredLocalIp() {
        List<String> ips = getAvailableIpv4Addresses();
        for (String ip : ips) {
            if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                return ip;
            }
        }
        return ips.get(0);
    }

    /**
     * Cài đặt thuộc tính java.rmi.server.hostname để RMI sử dụng đúng IP
     * khi giao tiếp qua mạng giữa máy thực và máy ảo.
     */
    public static void setRmiHostname(String ip) {
        if (ip != null && !ip.trim().isEmpty()) {
            System.setProperty("java.rmi.server.hostname", ip.trim());
            System.out.println("[RMI] Đã cấu hình java.rmi.server.hostname = " + ip.trim());
        }
    }
}
