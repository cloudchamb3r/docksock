package com.example.sock.callback;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import lombok.Getter;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;

public class TerminalCallback extends ResultCallback.Adapter<Frame> {

    @Getter
    PipedInputStream stdinInputStream;
    PipedOutputStream stdinOutputStream;

    PipedInputStream stdoutInputStream;
    PipedOutputStream stdoutOutputStream;

    WebSocketSession boundSession;

    public TerminalCallback(WebSocketSession session) throws IOException {
        this.stdinOutputStream = new PipedOutputStream();
        this.stdinInputStream = new PipedInputStream(this.stdinOutputStream);

        this.stdoutOutputStream = new PipedOutputStream();
        this.stdoutInputStream = new PipedInputStream(this.stdoutOutputStream);

        this.boundSession = session;
    }

    @Override
    public void onNext(Frame object) {
        try {
            stdoutOutputStream.write(object.getPayload());
            stdoutOutputStream.flush();


            byte[] buf = new byte[4096];
            StringBuilder sb = new StringBuilder();
            while (true) {
                int rdBytes = stdoutInputStream.read(buf, 0, stdoutInputStream.available());
                if (rdBytes == -1) break;
                sb.append(new String(buf, 0, rdBytes, StandardCharsets.UTF_8));
                if (rdBytes < 4096) break;
            }
            if (sb.isEmpty()) return;
            boundSession.sendMessage(new TextMessage(sb.toString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void sendInput(byte[] message) throws IOException {
        stdinOutputStream.write(message);
        stdinOutputStream.flush();
    }
}
