package ru.practicum.statistics.client.controller;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.service.dashboard.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.service.dashboard.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.service.dashboard.RecommendedEventProto;
import ru.practicum.ewm.stats.service.dashboard.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.service.dashboard.UserPredictionsRequestProto;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Component
public class AnalyzerClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc
            .RecommendationsControllerBlockingStub client;

    public Stream<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();

        Iterator<RecommendedEventProto> iterator = client.getSimilarEvents(request);

        return asStream(iterator);
    }

    private Stream<RecommendedEventProto> asStream(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        );
    }

    public Stream<RecommendedEventProto> getRecommendations(
            long userId,
            int maxResults
    ) {
        UserPredictionsRequestProto request =
                UserPredictionsRequestProto.newBuilder()
                        .setUserId(userId)
                        .setMaxResults(maxResults)
                        .build();

        Iterator<RecommendedEventProto> iterator =
                client.getRecommendationsForUser(request);

        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        iterator,
                        Spliterator.ORDERED
                ),
                false
        );
    }

    public Stream<RecommendedEventProto> getInteractionsCount(
            List<Long> eventIds
    ) {
        InteractionsCountRequestProto.Builder request =
                InteractionsCountRequestProto.newBuilder();

        request.addAllEventId(eventIds);

        Iterator<RecommendedEventProto> iterator =
                client.getInteractionsCount(request.build());

        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        iterator,
                        Spliterator.ORDERED
                ),
                false
        );
    }
}
