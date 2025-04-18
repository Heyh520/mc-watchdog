package org.example;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class RconClient implements AutoCloseable {
    private final String host;
    private final int port;
    private final String password;

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private final Random random = new Random();

    public RconClient(String host, int port, String password) {
        this.host = host;
        this.port = port;
        this.password = password;
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
        authenticate();
    }

    private void authenticate() throws IOException {
        int requestId = random.nextInt();
        sendPacket(requestId, 3, password);
        int receivedId = receiveResponseId();

        if (requestId != receivedId) {
            throw new IOException("RCON 密码错误或身份验证失败！");
        }
    }

    public String sendCommand(String command) throws IOException {
        int requestId = random.nextInt();
        sendPacket(requestId, 2, command);

        int size = in.readInt(); // total size
        int responseId = in.readInt();
        int type = in.readInt();

        byte[] bytes = new byte[size - 10]; // 减去id(4)+type(4)+2个\0
        in.readFully(bytes);
        in.readByte(); // \0
        in.readByte(); // \0

        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void sendPacket(int requestId, int type, String payload) throws IOException {
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream packet = new DataOutputStream(baos);

        packet.writeInt(requestId);
        packet.writeInt(type);
        packet.write(payloadBytes);
        packet.writeByte(0);
        packet.writeByte(0);

        byte[] packetBytes = baos.toByteArray();

        out.writeInt(packetBytes.length);
        out.write(packetBytes);
        out.flush();
    }

    private int receiveResponseId() throws IOException {
        int size = in.readInt();      // total size
        int requestId = in.readInt(); // request id
        int type = in.readInt();      // response type

        byte[] rest = new byte[size - 8]; // 剩余 body + 2 null bytes
        in.readFully(rest); // discard

        return requestId;
    }

    @Override
    public void close() throws IOException {
        if (socket != null) socket.close();
    }
}
