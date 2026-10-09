package com.festhub.backend;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.ServerInterceptors;

public class BackendServer {
    public static void main(String[] args) throws Exception {
        String allowedIp = System.getenv("ALLOWED_CLIENT_IP");
        if (allowedIp == null || allowedIp.isBlank()) {
            throw new IllegalStateException("ALLOWED_CLIENT_IP must be set");
        }

        Server server = Grpc.newServerBuilderForPort(50051, InsecureServerCredentials.create())
                .addService(ServerInterceptors.intercept(
                        new GreeterImpl(), new IpAllowListInterceptor(allowedIp)))
                .build()
                .start();

        System.out.println("Backend listening on :50051, allowing only " + allowedIp);
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        server.awaitTermination();
    }
}