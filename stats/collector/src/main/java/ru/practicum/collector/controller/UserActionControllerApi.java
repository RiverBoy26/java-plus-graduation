
package ru.practicum.collector.controller;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import stats.service.collector.UserActionProto;

/**
 * gRPC API для приёма информации о действиях пользователей
 * с мероприятиями.
 * Обеспечивает передачу полученных действий в Collector
 * для последующей отправки в Kafka.
 *
 * @see UserActionProto
 * @see CollectorControllerApi
 */
public interface UserActionControllerApi {

    /**
     * Приём действия пользователя по gRPC.
     * Поддерживаются просмотр мероприятия, регистрация
     * на мероприятие и положительная оценка.
     *
     * @param request сообщение о действии пользователя в формате Protobuf
     * @param responseObserver наблюдатель для отправки
     *                         пустого ответа при успешной обработке
     *                         или ошибки при неудачном выполнении
     */
    void collectUserAction(
            UserActionProto request,
            StreamObserver<Empty> responseObserver
    );
}
