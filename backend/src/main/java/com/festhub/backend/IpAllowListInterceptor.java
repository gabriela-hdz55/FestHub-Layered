package com.festhub.backend;

import io.grpc.*;
import java.net.InetSocketAddress;

/** Rejects any call whose TCP source IP is not the presentation tier's IP. */
public class IpAllowListInterceptor implements ServerInterceptor {
    private final String allowedIp;

    public IpAllowListInterceptor(String allowedIp) {
        this.allowedIp = allowedIp;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        var remote = call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
        String ip = (remote instanceof InetSocketAddress isa)
                ? isa.getAddress().getHostAddress() : null;

        if (!allowedIp.equals(ip)) {   // fail closed
            System.err.println("REJECTED call from " + ip);
            call.close(Status.PERMISSION_DENIED.withDescription("Source IP not allowed"), new Metadata());
            return new ServerCall.Listener<>() {};
        }
        return next.startCall(call, headers);
    }
}