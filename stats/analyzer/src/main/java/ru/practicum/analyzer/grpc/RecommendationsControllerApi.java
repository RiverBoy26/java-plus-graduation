
package ru.practicum.analyzer.grpc;

import io.grpc.stub.StreamObserver;
import ru.practicum.ewm.stats.service.dashboard.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.service.dashboard.RecommendedEventProto;
import ru.practicum.ewm.stats.service.dashboard.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.service.dashboard.UserPredictionsRequestProto;

/**
 * gRPC API для получения рекомендаций мероприятий.
 * Предоставляет рекомендации на основе истории
 * взаимодействий пользователей и коэффициентов
 * сходства мероприятий.
 *
 * @see RecommendedEventProto
 * @see UserPredictionsRequestProto
 * @see SimilarEventsRequestProto
 * @see InteractionsCountRequestProto
 */
public interface RecommendationsControllerApi {

    /**
     * Получение списка мероприятий, похожих на указанное.
     * Из результата исключаются мероприятия,
     * с которыми пользователь уже взаимодействовал.
     *
     * @param request запрос с идентификаторами мероприятия
     *                и пользователя, а также ограничением
     *                количества результатов
     * @param responseObserver наблюдатель для потоковой отправки похожих мероприятий
     */
    void getSimilarEvents(
            SimilarEventsRequestProto request,
            StreamObserver<RecommendedEventProto> responseObserver
    );

    /**
     * Получение суммы максимальных весов взаимодействий
     * пользователей с указанными мероприятиями.
     *
     * @param proto запрос со списком идентификаторов мероприятий
     * @param responseObserver наблюдатель для потоковой
     *                         отправки мероприятий и их оценок
     */
    void getInteractionsCount(
            InteractionsCountRequestProto proto,
            StreamObserver<RecommendedEventProto> responseObserver
    );

    /**
     * Получение персональных рекомендаций мероприятий
     * для указанного пользователя.
     * Рекомендации формируются на основе истории
     * взаимодействий и сходства мероприятий.
     *
     * @param proto запрос с идентификатором пользователя
     *              и максимальным количеством рекомендаций
     * @param responseObserver наблюдатель для потоковой
     *                         отправки рекомендуемых мероприятий
     *                         с предсказанными оценками
     */
    void getRecommendationsForUser(
            UserPredictionsRequestProto proto,
            StreamObserver<RecommendedEventProto> responseObserver
    );
}
