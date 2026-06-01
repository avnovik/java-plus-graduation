package ru.practicum.ewm.stats.collector.grpc;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.collector.service.UserActionService;
import ru.practicum.ewm.stats.grpc.UserActionControllerGrpc;
import ru.practicum.ewm.stats.grpc.UserActionProto;

/**
 * gRPC endpoint Collector для приёма действий пользователей.
 */
@GrpcService
@RequiredArgsConstructor
public class UserActionGrpcService extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final UserActionService userActionService;

    /**
     * Принимает действие пользователя и передаёт его в обработку.
     */
    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        userActionService.collect(request);
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
