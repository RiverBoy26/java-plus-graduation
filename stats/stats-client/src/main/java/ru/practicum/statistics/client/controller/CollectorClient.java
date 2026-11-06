package ru.practicum.statistics.client.controller;

import com.google.protobuf.util.Timestamps;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import stats.service.collector.ActionTypeProto;
import stats.service.collector.UserActionControllerGrpc;
import stats.service.collector.UserActionProto;

@Component
public class CollectorClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub client;

    public void sendView(long userId, long eventId) {
        sendAction(userId, eventId, ActionTypeProto.ACTION_VIEW);
    }

    public void sendLike(long userId, long eventId) {
        sendAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
    }

    public void sendRegister(long userId, long eventId) {
        sendAction(userId, eventId, ActionTypeProto.ACTION_REGISTER);
    }

    private void sendAction(
            long userId,
            long eventId,
            ActionTypeProto actionType
    ) {
        UserActionProto request = UserActionProto.newBuilder()
                .setId(userId)
                .setEventId(eventId)
                .setActionType(actionType)
                .setTimestamp(
                        Timestamps.fromMillis(System.currentTimeMillis())
                )
                .build();

        client.collectUserAction(request);
    }
}
