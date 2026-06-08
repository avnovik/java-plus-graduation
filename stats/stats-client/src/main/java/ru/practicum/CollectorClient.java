package ru.practicum;

import com.google.protobuf.Timestamp;
import java.time.Instant;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.grpc.ActionTypeProto;
import ru.practicum.ewm.stats.grpc.UserActionControllerGrpc;
import ru.practicum.ewm.stats.grpc.UserActionProto;

@Service
public class CollectorClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub client;

    public void collectView(long userId, long eventId) {
        collectUserAction(userId, eventId, ActionTypeProto.ACTION_VIEW);
    }

    public void collectLike(long userId, long eventId) {
        collectUserAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
    }

    public void collectRegister(long userId, long eventId) {
        collectUserAction(userId, eventId, ActionTypeProto.ACTION_REGISTER);
    }

    private void collectUserAction(long userId, long eventId, ActionTypeProto actionType) {
        Instant now = Instant.now();
        UserActionProto request = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(actionType)
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(now.getEpochSecond())
                        .setNanos(now.getNano())
                        .build())
                .build();

        client.collectUserAction(request);
    }
}
