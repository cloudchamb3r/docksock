package com.example.sock.handler;

import com.example.sock.callback.TerminalCallback;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TerminalHandler extends TextWebSocketHandler {
    private final DockerClient client;
    Map<String, String> containerIdMap = new HashMap<>();
    Map<String, TerminalCallback> callbackMap = new HashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        var container = client.createContainerCmd("ubuntu")
                .withStdinOpen(true)
                .exec();

        TerminalCallback cb = new TerminalCallback(session);

        containerIdMap.put(session.getId(), container.getId());
        callbackMap.put(session.getId(), cb);

        client.startContainerCmd(container.getId()).exec();
        client.attachContainerCmd(container.getId())
                .withFollowStream(true)
                .withStdIn(cb.getStdinInputStream())
                .withStdOut(true)
                .withStdErr(true)
                .exec(cb);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        TerminalCallback term = callbackMap.get(session.getId());

        term.sendInput(new String(message.asBytes()).replace("\r\n", "\n").getBytes());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String containerId = containerIdMap.get(session.getId());
        if (containerId == null) {
            return;
        }
        client.stopContainerCmd(containerId).exec();
        client.waitContainerCmd(containerId).exec(new WaitContainerResultCallback());
        client.removeContainerCmd(containerId).exec();

        callbackMap.remove(session.getId()).close();
        containerIdMap.remove(session.getId());
    }
}
