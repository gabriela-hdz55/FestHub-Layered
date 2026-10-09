package com.festhub.backend;

import com.festhub.proto.GreeterGrpc;
import com.festhub.proto.HelloReply;
import com.festhub.proto.HelloRequest;
import io.grpc.stub.StreamObserver;

public class GreeterImpl extends GreeterGrpc.GreeterImplBase {
    @Override
    public void sayHello(HelloRequest req, StreamObserver<HelloReply> out) {
        // Business logic (and later, database access) goes here.
        out.onNext(HelloReply.newBuilder()
                .setMessage("Hello, " + req.getName() + " (from the backend tier)")
                .build());
        out.onCompleted();
    }
}