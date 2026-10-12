package com.festhub.frontend;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import com.festhub.proto.GreeterGrpc;
import com.festhub.proto.HelloRequest;
import com.sun.net.httpserver.HttpServer;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class Main {
    public static void main(String[] args) throws Exception {
        String backendHost = System.getenv().getOrDefault("BACKEND_HOST", "backend");

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(backendHost, 50051)
                .usePlaintext()
                .build();
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);

        HttpServer http = HttpServer.create(new InetSocketAddress(8080), 0);

        http.createContext("/", ex -> {

            try {
                byte[] fileBytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("/public/index.html"));
                String html = new String(fileBytes, StandardCharsets.UTF_8);
                respond(ex, 200, html, "text/html");
            } catch (Exception e) {
                e.printStackTrace();
                respond(ex, 404, "404: index.html not found.", "text/plain");
            }
        });

        http.createContext("/hello", ex -> {
            String query = ex.getRequestURI().getRawQuery();
            String name = "world";
            if (query != null && query.startsWith("name=")) {
                name = URLDecoder.decode(query.substring(5), StandardCharsets.UTF_8);
            }
            try {
                String msg = stub.sayHello(HelloRequest.newBuilder().setName(name).build()).getMessage();
                respond(ex, 200, escape(msg), "text/plain");
            } catch (StatusRuntimeException e) {
                respond(ex, 502, "Backend error: " + e.getStatus().getCode(), "text/plain");
            }
        });

        http.start();
        System.out.println("Frontend listening on :8080, backend at " + backendHost + ":50051");
    }

    private static void respond(com.sun.net.httpserver.HttpExchange ex, int code, String body, String type)
            throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}