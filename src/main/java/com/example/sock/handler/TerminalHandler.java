package com.example.sock.handler;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.AttachContainerCmd;
import com.github.dockerjava.api.model.Frame;
import org.apache.hc.core5.http.nio.support.TerminalAsyncServerFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.print.attribute.HashAttributeSet;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

@Component
public class TerminalHandler extends TextWebSocketHandler {
    @Autowired
    DockerClient client;
    Map<String, ResultCallback<Frame>> callbackMap = new HashMap<>();
    Map<String, InputStream> inputStreamMap = new HashMap<>();
    Map<String, OutputStream> outputStreamMap = new HashMap<>();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        OutputStream pos = outputStreamMap.get(session.getId());
        InputStream pis = inputStreamMap.get(session.getId());
        pos.write(message.asBytes());
        pos.flush();
        Thread.sleep(100);
        var msg = new TextMessage(pis.readNBytes(4096));
        session.sendMessage(msg);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        var container = client.createContainerCmd("ubuntu")
                .withStdinOpen(true)
                .exec();
        PipedInputStream pis = new PipedInputStream();
        PipedOutputStream pos = new PipedOutputStream(pis);
        TerminalCallback cb = new TerminalCallback(pos);

        inputStreamMap.put(session.getId(), pis);
        outputStreamMap.put(session.getId(), pos);
        callbackMap.put(session.getId(), cb);

        client.startContainerCmd(container.getId()).exec();

        client.attachContainerCmd(container.getId())
                .withFollowStream(true)
                .withStdIn(pis)
                .withStdOut(true)
                .withStdErr(true)
                .exec(cb);
    }

    public static class TerminalCallback implements ResultCallback<Frame> {
        OutputStream os;
        TerminalCallback(OutputStream outputStream) {
            this.os = outputStream;
        }
        @Override
        public void onStart(Closeable closeable) {
            System.out.println("on start");
        }

        @Override
        public void onNext(Frame object) {
            System.out.println("on next");
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
    }
}
