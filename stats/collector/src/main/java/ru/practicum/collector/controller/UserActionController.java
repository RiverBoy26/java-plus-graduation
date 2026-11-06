package ru.practicum.collector.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import stats.service.collector.UserActionControllerGrpc;
import stats.service.collector.UserActionProto;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserActionController extends UserActionControllerGrpc.UserActionControllerImplBase
        implements UserActionControllerApi {
    private final CollectorController controller;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        log.info(
                "gRPC request: eventId={}, timestamp={}, type={}",
                request.getEventId(),
                request.getTimestamp(),
                request.getActionType()
        );
        try {
            log.info("Получен proto request={}", request);
            controller.sendMessage(request);
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
            log.info("gRPC response sent: eventId={}", request.getEventId());
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }
}