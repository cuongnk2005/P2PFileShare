package org.example.p2pfileshare.Model;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class PeerDiscovery {

    // Cổng UDP để phát hiện peer
    public static final int DISCOVERY_PORT = 50003;

    private static final String DISCOVER_MSG = "P2P_DISCOVER_REQUEST";
    private static final String RESPONSE_PREFIX = "P2P_DISCOVER_RESPONSE";

    /**
     * Thread này luôn chạy trên mỗi peer, lắng nghe broadcast
     * và trả lời thông tin của chính peer đó.
     */
    public static void startResponder(String myName, int fileServerPort) {
        Thread t = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(DISCOVERY_PORT)) {
                byte[] buf = new byte[1024];
                System.out.println("[Discovery] Responder started on UDP port " + DISCOVERY_PORT);
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);  // chờ gói DISCOVER_REQUEST

                    String msg = new String(packet.getData(), 0, packet.getLength()).trim();
                    if (DISCOVER_MSG.equals(msg)) {
                        // gửi lại thông tin peer
                        String ip = packet.getAddress().getHostAddress(); // IP mà peer kia nhìn thấy
                        String response = RESPONSE_PREFIX + "|" + myName + "|" + fileServerPort;

                        byte[] respData = response.getBytes();
                        DatagramPacket resp = new DatagramPacket(
                                respData,
                                respData.length,
                                packet.getAddress(),
                                packet.getPort()
                        );
                        socket.send(resp);
                        System.out.println("[Discovery] Reply to " + ip + ": " + response);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        t.setDaemon(true); // để app tắt được khi đóng GUI
        t.start();
    }

    /**
     * Gửi 1 broadcast DISCOVER, đợi phản hồi trong timeoutMillis,
     * trả về danh sách PeerInfo.
     */
    public static List<PeerInfo> discoverPeers(String myName,  int timeoutMillis) {
        List<PeerInfo> result = new ArrayList<>();

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            socket.setSoTimeout(timeoutMillis);

            // 1) Gửi gói broadcast
            byte[] data = DISCOVER_MSG.getBytes();
//            DatagramPacket packet = new DatagramPacket(
//                    data,
//                    data.length,
//                    InetAddress.getByName("255.255.255.255"),
//                    DISCOVERY_PORT
//            );
//            socket.send(packet);
            int[] ports = {50000, 50001, 50002, 50003, 50004};

            for (int p : ports) {
                DatagramPacket packet = new DatagramPacket(
                        data,
                        data.length,
                        InetAddress.getByName("255.255.255.255"),
                        p
                );
                socket.send(packet);
            }
            System.out.println("[Discovery] Sent broadcast");

            // 2) Nhận phản hồi cho đến khi timeout
            byte[] buf = new byte[1024];
            long start = System.currentTimeMillis();

            while (System.currentTimeMillis() - start < timeoutMillis) {
                try {
                    DatagramPacket resp = new DatagramPacket(buf, buf.length);
                    socket.receive(resp);

                    String msg = new String(resp.getData(), 0, resp.getLength()).trim();
                    if (msg.startsWith(RESPONSE_PREFIX)) {
                        // Định dạng: P2P_DISCOVER_RESPONSE|Tên|fileServerPort
                        String[] parts = msg.split("\\|");
                        if (parts.length >= 3) {
                            String peerName = parts[1];
                            int peerFilePort = Integer.parseInt(parts[2]);
                            String ip = resp.getAddress().getHostAddress();

                            // nếu muốn, có thể bỏ qua chính mình bằng cách check myName hoặc ip
                             if (peerName.equals(myName)) continue;

                            PeerInfo peer = new PeerInfo(peerName, ip, peerFilePort, "Online");
                            result.add(peer);
                            System.out.println("[Discovery] Found: " + peerName + " @ " + ip + ":" + peerFilePort);
                        }
                    }

                } catch (SocketTimeoutException e) {
                    // hết thời gian đợi - thoát vòng while
                    break;
                } catch (IOException ex) {
                    ex.printStackTrace();
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }
}
