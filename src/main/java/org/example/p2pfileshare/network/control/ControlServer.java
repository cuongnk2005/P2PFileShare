package org.example.p2pfileshare.network.control;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Function;

/**
 * Lắng nghe các yêu cầu điều khiển:
 * - CONNECT_REQUEST|fromPeer|toPeer
 *
 * Sau khi nhận:
 *   1) Gọi callback onIncomingConnect(fromPeer) -> boolean accept
 *   2) Trả về 1 dòng:
 *        CONNECT_ACCEPT|toPeer|fromPeer|note
 *      hoặc
 *        CONNECT_REJECT|toPeer|fromPeer|note
 */
public class ControlServer {

    private final int port;
    private volatile boolean running = false;

    /**
     * callback: nhận tên peer gửi yêu cầu, trả true nếu chấp nhận, false nếu từ chối
     */
    private final Function<String, Boolean> onIncomingConnect;

    public ControlServer(int port, Function<String, Boolean> onIncomingConnect) {
        this.port = port;
        this.onIncomingConnect = onIncomingConnect;
    }

    public void start() {
        if (running) return;
        running = true;

        Thread t = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("[ControlServer] Listening on port " + port);

                while (running) {
                    Socket client = serverSocket.accept();
                    handleClient(client);
                }

            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                } else {
                    System.out.println("[ControlServer] Stopped");
                }
            }
        }, "control-server");

        t.setDaemon(true);
        t.start();
    }

    public void stop() {
        running = false;
        // Nếu cần, có thể mở 1 socket ảo vào chính mình để giải phóng accept()
    }

    private void handleClient(Socket socket) {
        Thread t = new Thread(() -> {
            try (Socket s = socket;
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(s.getInputStream()));
                 PrintWriter writer = new PrintWriter(
                         new OutputStreamWriter(s.getOutputStream()), true)) {

                String raw = reader.readLine();
                if (raw == null || raw.isEmpty()) return;

                ControlProtocol.ParsedMessage msg = ControlProtocol.parse(raw);
                if (msg == null) return;

                System.out.println("[ControlServer] Received: " + raw);

                if (ControlProtocol.CONNECT_REQUEST.equals(msg.command)) {
                    String fromPeer = msg.fromPeer; // người yêu cầu
                    String toPeer   = msg.toPeer;   // mình

                    boolean accept = true;
                    if (onIncomingConnect != null) {
                        try {
                            accept = Boolean.TRUE.equals(onIncomingConnect.apply(fromPeer));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            accept = false;
                        }
                    }

                    String respCmd  = accept ? ControlProtocol.CONNECT_ACCEPT
                            : ControlProtocol.CONNECT_REJECT;
                    String respNote = accept ? "Accepted" : "Rejected";

                    // Lưu ý: từ phía server, fromPeer = "mình", toPeer = "người gửi request"
                    String resp = ControlProtocol.build(
                            respCmd,
                            toPeer,     // from = mình
                            fromPeer,   // to   = người yêu cầu
                            respNote
                    );

                    writer.println(resp);
                    System.out.println("[ControlServer] Sent: " + resp);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "control-handler");

        t.setDaemon(true);
        t.start();
    }
}
