package ru.practicum.ewm.stats.collector.mapper;

import com.google.protobuf.Timestamp;
import java.time.Instant;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.grpc.ActionTypeProto;
import ru.practicum.ewm.stats.grpc.UserActionProto;

/**
 * Преобразует gRPC-сообщения о действиях пользователей в Avro-сообщения.
 */
@Component
public class UserActionMapper {

    public UserActionAvro toAvro(UserActionProto proto) {
        return new UserActionAvro(
                proto.getUserId(),
                proto.getEventId(),
                toAvroActionType(proto.getActionType()),
                toInstant(proto.getTimestamp())
        );
    }

    private ActionTypeAvro toAvroActionType(ActionTypeProto actionType) {
        return switch (actionType) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Неизвестный тип действия пользователя");
        };
    }

    private Instant toInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}
