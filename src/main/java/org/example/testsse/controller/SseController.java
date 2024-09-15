package org.example.testsse.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/sse")
@CrossOrigin("*")
public class SseController {

    // Thread-safe list to store all active emitters (clients)
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    // Register a new client to receive SSE events
    @GetMapping("/register")
    public SseEmitter registerClient() {
        SseEmitter emitter = new SseEmitter(60_000L); // 60-second timeout
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));

        // Handle error when the client disconnects unexpectedly
        emitter.onError((ex) -> emitters.remove(emitter));

        return emitter;
    }


    // Send a message to all connected clients
    @GetMapping("/message")
    public String sendMessageToClients(String message) {
        List<SseEmitter> deadEmitters = new ArrayList<>();

        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("chat-message").data(message));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        });

        emitters.removeAll(deadEmitters); // Remove disconnected clients

        return "Message sent to all clients!";
    }
}
