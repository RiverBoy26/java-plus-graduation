package ru.practicum.analyzer.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.analyzer.service.RecommendationService;
import ru.practicum.ewm.stats.service.dashboard.*;

import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    private final RecommendationService recommendationService;

    @Override
    public void getSimilarEvents(
            SimilarEventsRequestProto request,
            StreamObserver<RecommendedEventProto> responseObserver

    ) {
        try {
            List<RecommendedEventProto> result = recommendationService.getSimilarEvents(
                    request.getEventId(),
                    request.getUserId(),
                    request.getMaxResults()
            );

            result.forEach(responseObserver::onNext);

            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto proto,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            List<RecommendedEventProto> recommendations = recommendationService
                    .getEventsRatings(proto.getEventIdList());

            recommendations.forEach(responseObserver::onNext);

            responseObserver.onCompleted();
        } catch (Exception e) {

            responseObserver.onError(e);
        }
    }

    @Override
    public void getRecommendationsForUser(
            UserPredictionsRequestProto proto,
            StreamObserver<RecommendedEventProto> responseObserver
    ) {
        try {
            List<RecommendedEventProto> recommendations = recommendationService
                    .getRecommendationsForUser(proto.getUserId(), proto.getMaxResults());
            recommendations.forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
}