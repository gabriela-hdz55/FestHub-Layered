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


    //////////////////
    /// Contents:
    /// ARTIST MANAGEMENT
    /// CONCERT EVENT MANAGEMENT
    /// BOOKING CATEGORY MANAGEMENT
    /// PROMOTIONAL MEDIA MANAGEMENT
    //////////////////

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

    ///////////
    // ARTIST C-R-U-D & MANAGEMENT //
    ///////////

        http.createContext("/api/artist/create", ex -> {
                if (!"POST".equals(ex.getRequestMethod())) {
                respond(ex, 405, "Method Not Allowed", "text/plain");
                return;
            }

            try {
                // read JSON->text-converted string from JS fetch
                String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

                // parse string for name + contact
                String name = "";
                String contact = "";
                for (String pair : body.split("&")) {
                    String[] split = pair.split("=");
                    if (split.length == 2) {
                        if (split[0].equals("name")) name = URLDecoder.decode(split[1], StandardCharsets.UTF_8);
                        if (split[0].equals("contact")) contact = URLDecoder.decode(split[1], StandardCharsets.UTF_8);
                    }
                }

                // terminal confirmation message for proof
                System.out.println("FRONTEND RECEIVED -> Name: " + name + " | Contact: " + contact);

                // Pending: Pass this data to gRPC backend here
              

                // success message
                respond(ex, 200, "Successfully processed " + name + ".", "text/plain");

            } catch (Exception e) {
                e.printStackTrace();
                respond(ex, 500, "Server crashed reading the data.", "text/plain");
            }
        });

        // READ CATCHER (GET)
        http.createContext("/api/artist/read", ex -> {
            // Extract the ID from the URL (e.g., ?id=123)
            String query = ex.getRequestURI().getRawQuery();
            String id = (query != null && query.startsWith("id=")) ? query.substring(3) : "UNKNOWN";
            
            System.out.println("FRONTEND RECEIVED READ FOR -> ID: " + id);
            // Pending: Execute gRPC read request here using the ID
            
            respond(ex, 200, "Successfully requested read for ID: " + id, "text/plain");
        });

        // DELETE CATCHER (DELETE)
        http.createContext("/api/artist/delete", ex -> {
            // Extract the ID from the URL
            String query = ex.getRequestURI().getRawQuery();
            String id = (query != null && query.startsWith("id=")) ? query.substring(3) : "UNKNOWN";
            
            System.out.println("FRONTEND RECEIVED DELETE FOR -> ID: " + id);
            // Pending: Execute gRPC delete request here using the ID
            
            respond(ex, 200, "Successfully requested delete for ID: " + id, "text/plain");
        });

        // UPDATE CATCHER (POST)
        http.createContext("/api/artist/update", ex -> {
            try {
                // Read the packaged URLSearchParams string sent by JS
                String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String id = "", name = "", contact = "";
                
                // parse to extract the 3 variables
                for (String pair : body.split("&")) {
                    String[] split = pair.split("=");
                    if (split.length == 2) {
                        if (split[0].equals("id")) id = URLDecoder.decode(split[1], StandardCharsets.UTF_8);
                        if (split[0].equals("name")) name = URLDecoder.decode(split[1], StandardCharsets.UTF_8);
                        if (split[0].equals("contact")) contact = URLDecoder.decode(split[1], StandardCharsets.UTF_8);
                    }
                }
                
                System.out.println("FRONTEND RECEIVED UPDATE -> ID: " + id + " | New Name: " + name + " | New Contact: " + contact);
                
                // Pending; Execute gRPC update request here using the ID, name, and contact
                
                respond(ex, 200, "Successfully processed update for ID: " + id, "text/plain");
            } catch (Exception e) {
                e.printStackTrace();
                respond(ex, 500, "Server crashed reading update data.", "text/plain");
            }
        });

    /////////////////    
    // CONCERT EVENT C-R-U-D & MANAGEMENT //
    /////////////////
    
    // CREATE catcher
        http.createContext("/api/event/create", ex -> {
            try {
                String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String name = "", desc = "", tickets = "", price = "";
                for (String pair : body.split("&")) {
                    String[] split = pair.split("=");
                    if (split.length == 2) {
                        if (split[0].equals("name")) name = URLDecoder.decode(split[1], StandardCharsets.UTF_8);
                        if (split[0].equals("desc")) desc = URLDecoder.decode(split[1], StandardCharsets.UTF_8);
                        if (split[0].equals("tickets")) tickets = URLDecoder.decode(split[1], StandardCharsets.UTF_8);
                        if (split[0].equals("price")) price = URLDecoder.decode(split[1], StandardCharsets.UTF_8);
                    }
                }
                System.out.println("FRONTEND RECEIVED EVENT CREATE -> Name: " + name + " | Price: $" + price);
                
                // Pending: Pass this data to gRPC backend here
                
                respond(ex, 200, "Successfully processed create for event: " + name, "text/plain");
            } catch (Exception e) {
                respond(ex, 500, "Server Error", "text/plain");
            }
        });

        // READ catcher
        http.createContext("/api/event/read", ex -> {
            String query = ex.getRequestURI().getRawQuery();
            String id = (query != null && query.startsWith("id=")) ? query.substring(3) : "UNKNOWN";
            System.out.println("FRONTEND RECEIVED EVENT READ -> ID: " + id);
            
            // Pending: Pass this data to gRPC backend here
            
            respond(ex, 200, "Successfully processed read for event id: " + id, "text/plain");
        });

        // DELETE catcher
        http.createContext("/api/event/delete", ex -> {
            String query = ex.getRequestURI().getRawQuery();
            String id = (query != null && query.startsWith("id=")) ? query.substring(3) : "UNKNOWN";
            System.out.println("FRONTEND RECEIVED EVENT DELETE -> ID: " + id);
            
            // Pending: Pass this data to gRPC backend here
            
            respond(ex, 200, "Successfully processed delete for event id: " + id, "text/plain");
        });

        // UPDATE catcher
        http.createContext("/api/event/update", ex -> {
            try {
                String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String id = "", name = "", desc = "", tickets = "", price = "";
                for (String pair : body.split("&")) {
                    String[] split = pair.split("=");
                    if (split.length == 2) {
                        if (split[0].equals("id")) id = URLDecoder.decode(split[1], StandardCharsets.UTF_8);
                        if (split[0].equals("name")) name = URLDecoder.decode(split[1], StandardCharsets.UTF_8);
                        if (split[0].equals("desc")) desc = URLDecoder.decode(split[1], StandardCharsets.UTF_8);
                        if (split[0].equals("tickets")) tickets = URLDecoder.decode(split[1], StandardCharsets.UTF_8);
                        if (split[0].equals("price")) price = URLDecoder.decode(split[1], StandardCharsets.UTF_8);
                    }
                }
                System.out.println("FRONTEND RECEIVED EVENT UPDATE -> ID: " + id);
                
                // Pending: Pass this data to gRPC backend here
                
                respond(ex, 200, "Successfully processed update for event id: " + id, "text/plain");
            } catch (Exception e) {
                respond(ex, 500, "Server Error", "text/plain");
            }
        });

        ////////////////////////
        // BOOKING CATEGORY C-R-U-D & MANAGEMENT
        ////////////////////////
        

        // CREATE catcher
        http.createContext("/api/category/create", ex -> {
            try {
                String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String name = "", desc = "";
                for (String pair : body.split("&")) {
                    String[] split = pair.split("=");
                    if (split.length == 2) {
                        if (split[0].equals("name")) name = URLDecoder.decode(split[1], StandardCharsets.UTF_8);
                        if (split[0].equals("desc")) desc = URLDecoder.decode(split[1], StandardCharsets.UTF_8);
                    }
                }
                System.out.println("FRONTEND RECEIVED CATEGORY CREATE -> Name: " + name);
                
                // Pending: Pass this data to gRPC backend here
                
                respond(ex, 200, "Successfully processed create for category: " + name, "text/plain");
            } catch (Exception e) {
                respond(ex, 500, "Server Error", "text/plain");
            }
        });

        // READ catcher
        http.createContext("/api/category/read", ex -> {
            String query = ex.getRequestURI().getRawQuery();
            String id = (query != null && query.startsWith("id=")) ? query.substring(3) : "UNKNOWN";
            System.out.println("FRONTEND RECEIVED CATEGORY READ -> ID: " + id);
            
            // Pending: Pass this data to gRPC backend here
            
            respond(ex, 200, "Successfully processed read for category id: " + id, "text/plain");
        });

        // DELETE catcher
        http.createContext("/api/category/delete", ex -> {
            String query = ex.getRequestURI().getRawQuery();
            String id = (query != null && query.startsWith("id=")) ? query.substring(3) : "UNKNOWN";
            System.out.println("FRONTEND RECEIVED CATEGORY DELETE -> ID: " + id);
            
            // Pending: Pass this data to gRPC backend here
            
            respond(ex, 200, "Successfully processed delete for category id: " + id, "text/plain");
        });

        // UPDATE catcher
        http.createContext("/api/category/update", ex -> {
            try {
                String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String id = "", name = "", desc = "";
                for (String pair : body.split("&")) {
                    String[] split = pair.split("=");
                    if (split.length == 2) {
                        if (split[0].equals("id")) id = URLDecoder.decode(split[1], StandardCharsets.UTF_8);
                        if (split[0].equals("name")) name = URLDecoder.decode(split[1], StandardCharsets.UTF_8);
                        if (split[0].equals("desc")) desc = URLDecoder.decode(split[1], StandardCharsets.UTF_8);
                    }
                }
                System.out.println("FRONTEND RECEIVED CATEGORY UPDATE -> ID: " + id);
                
                // Pending: Pass this data to gRPC backend here
                
                respond(ex, 200, "Successfully processed update for category id: " + id, "text/plain");
            } catch (Exception e) {
                respond(ex, 500, "Server Error", "text/plain");
            }
        });

        //////////////////////
        // PROMOTIONAL MEDIA C-R-U-D & MANAGEMENT //
        //////////////////////
        
        // CREATE catcher
        http.createContext("/api/media/create", ex -> {
            try {
                String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String eventId = "", url = "";
                for (String pair : body.split("&")) {
                    String[] split = pair.split("=");
                    if (split.length == 2) {
                        if (split[0].equals("eventId")) eventId = URLDecoder.decode(split[1], StandardCharsets.UTF_8);
                        if (split[0].equals("url")) url = URLDecoder.decode(split[1], StandardCharsets.UTF_8);
                    }
                }
                System.out.println("FRONTEND RECEIVED MEDIA CREATE -> Event ID: " + eventId);
                
                // Pending: Pass this data to gRPC backend here
                
                respond(ex, 200, "Successfully processed create for media linked to event: " + eventId, "text/plain");
            } catch (Exception e) {
                respond(ex, 500, "Server Error", "text/plain");
            }
        });

        // READ catcher
        http.createContext("/api/media/read", ex -> {
            String query = ex.getRequestURI().getRawQuery();
            String id = (query != null && query.startsWith("id=")) ? query.substring(3) : "UNKNOWN";
            System.out.println("FRONTEND RECEIVED MEDIA READ -> ID: " + id);
            
            // Pending: Pass this data to gRPC backend here
            
            respond(ex, 200, "Successfully processed read for media id: " + id, "text/plain");
        });

        // DELETE catcher
        http.createContext("/api/media/delete", ex -> {
            String query = ex.getRequestURI().getRawQuery();
            String id = (query != null && query.startsWith("id=")) ? query.substring(3) : "UNKNOWN";
            System.out.println("FRONTEND RECEIVED MEDIA DELETE -> ID: " + id);
            
            // Pending: Pass this data to gRPC backend here
            
            respond(ex, 200, "Successfully processed delete for media id: " + id, "text/plain");
        });

        // UPDATE catcher
        http.createContext("/api/media/update", ex -> {
            try {
                String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String id = "", eventId = "", url = "";
                for (String pair : body.split("&")) {
                    String[] split = pair.split("=");
                    if (split.length == 2) {
                        if (split[0].equals("id")) id = URLDecoder.decode(split[1], StandardCharsets.UTF_8);
                        if (split[0].equals("eventId")) eventId = URLDecoder.decode(split[1], StandardCharsets.UTF_8);
                        if (split[0].equals("url")) url = URLDecoder.decode(split[1], StandardCharsets.UTF_8);
                    }
                }
                System.out.println("FRONTEND RECEIVED MEDIA UPDATE -> ID: " + id);
                
                // Pending: Pass this data to gRPC backend here
                
                respond(ex, 200, "Successfully processed update for media id: " + id, "text/plain");
            } catch (Exception e) {
                respond(ex, 500, "Server Error", "text/plain");
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