
package ru.practicum.collector.controller;

import stats.service.collector.UserActionProto;

/**
 * API для обработки действий пользователей,
 * поступающих в сервис Collector.
 * Обеспечивает преобразование Protobuf-сообщений
 * в формат Avro и их отправку в Kafka.
 *
 * @see UserActionProto
 */
public interface CollectorControllerApi {
    /**
     * Обработка сообщения о действии пользователя.
     *
     * @param proto сообщение о действии пользователя в формате Protobuf
     */
    void sendMessage(UserActionProto proto);
}
