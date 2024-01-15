package com.example.sock.handler;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import java.nio.charset.StandardCharsets;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

@Component
public class TerminalHandler extends TextWebSocketHandler {
    @Autowired
    DockerClient client;
    Map<String, TerminalCallback> callbackMap = new HashMap<>();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        TerminalCallback term = callbackMap.get(session.getId());

        term.sendInput(new String(message.asBytes()).replace("\r\n", "\n").getBytes());

        String result = term.recvResult();
        if (result != null) {
            session.sendMessage(new TextMessage(result));
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        var container = client.createContainerCmd("ubuntu")
                .withStdinOpen(true)
                .exec();

        TerminalCallback cb = new TerminalCallback();

        callbackMap.put(session.getId(), cb);

        client.startContainerCmd(container.getId()).exec();

        client.attachContainerCmd(container.getId())
                .withFollowStream(true)
                .withStdIn(cb.getStdinInputStream())
                .withStdOut(true)
                .withStdErr(true)
                .exec(cb);
    }

    public static class TerminalCallback implements ResultCallback<Frame> {
        @Getter
        PipedInputStream stdinInputStream;
        PipedOutputStream stdinOutputStream;

        PipedInputStream stdoutInputStream;
        PipedOutputStream stdoutOutputStream;
        TerminalCallback() throws IOException {
            this.stdinOutputStream = new PipedOutputStream();
            this.stdinInputStream = new PipedInputStream(this.stdinOutputStream);

            this.stdoutOutputStream = new PipedOutputStream();
            this.stdoutInputStream = new PipedInputStream(this.stdoutOutputStream);
        }
        @Override
        public void onStart(Closeable closeable) {
            System.out.println("on start");
        }

        @Override
        public void onNext(Frame object) {
            try {
                stdoutOutputStream.write(object.getPayload());
                stdoutOutputStream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            System.out.println("on error");
        }

        @Override
        public void onComplete() {
            System.out.println("on complete");
        }

        @Override
        public void close() throws IOException {
            System.out.println("on close");
        }

        public void sendInput(byte[] message) throws IOException{
            stdinOutputStream.write(message);
            stdinOutputStream.flush();
        }

        public String recvResult() throws IOException {
            byte[] buf = new byte[4096];
            StringBuilder sb = new StringBuilder();
            while (true) {
                int rdBytes = stdoutInputStream.read(buf, 0, stdoutInputStream.available());
                if (rdBytes == -1) break;
                sb.append(new String(buf, 0, rdBytes, StandardCharsets.UTF_8));
                if (rdBytes < 4096) break;
            }
            if (sb.length() == 0) return null;
            return sb.toString();
        }
    }
}
