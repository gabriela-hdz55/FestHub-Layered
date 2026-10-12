package com.festhub.backend;

import com.festhub.backend.data.Database;
import com.festhub.backend.grpc.FestHubServiceImpl;
import com.festhub.backend.repository.ArtistRepository;
import com.festhub.backend.repository.BookingCategoryRepository;
import com.festhub.backend.repository.ConcertEventRepository;
import com.festhub.backend.repository.JdbcArtistRepository;
import com.festhub.backend.repository.JdbcBookingCategoryRepository;
import com.festhub.backend.repository.JdbcConcertEventRepository;
import com.festhub.backend.repository.JdbcPromoMediaRepository;
import com.festhub.backend.repository.PromoMediaRepository;
import com.festhub.backend.service.ArtistService;
import com.festhub.backend.service.BookingCategoryService;
import com.festhub.backend.service.ConcertEventService;
import com.festhub.backend.service.PromoMediaService;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.ServerInterceptors;

public class BackendServer {

    public static void main(String[] args) throws Exception {
        String allowedIp = System.getenv("ALLOWED_CLIENT_IP");

        if (allowedIp == null || allowedIp.isBlank()) {
            throw new IllegalStateException(
                    "ALLOWED_CLIENT_IP must be set");
        }

        // Initialize / verify the database before starting the server.
        Database.initialize();

        // Data-access layer.
        BookingCategoryRepository bookingCategoryRepository = new JdbcBookingCategoryRepository();
        ArtistRepository artistRepository = new JdbcArtistRepository();
        PromoMediaRepository promoMediaRepository = new JdbcPromoMediaRepository();
        ConcertEventRepository concertEventRepository = new JdbcConcertEventRepository();

        // Business-logic layer.
        BookingCategoryService bookingCategoryService = new BookingCategoryService(bookingCategoryRepository);
        ArtistService artistService = new ArtistService(artistRepository);
        PromoMediaService promoMediaService = new PromoMediaService(promoMediaRepository);
        ConcertEventService concertEventService = new ConcertEventService(concertEventRepository);

        // gRPC adapter layer.
        FestHubServiceImpl festHubService = new FestHubServiceImpl(
                bookingCategoryService,
                artistService,
                promoMediaService,
                concertEventService);

        // Restrict gRPC requests to the presentation tier.
        IpAllowListInterceptor interceptor = new IpAllowListInterceptor(allowedIp);

        Server server = Grpc.newServerBuilderForPort(
                50051,
                InsecureServerCredentials.create())
                .addService(ServerInterceptors.intercept(
                        new GreeterImpl(),
                        interceptor))
                .addService(ServerInterceptors.intercept(
                        festHubService,
                        interceptor))
                .build()
                .start();

        System.out.println(
                "Backend listening on :50051, allowing only " + allowedIp);

        Runtime.getRuntime().addShutdownHook(
                new Thread(server::shutdown));

        server.awaitTermination();
    }
}