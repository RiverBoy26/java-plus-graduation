package ru.practicum.analyzer.service;

import ru.practicum.ewm.stats.service.dashboard.RecommendedEventProto;

import java.util.List;

public interface RecommendationService {
    List<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResult);

    List<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResult);

    List<RecommendedEventProto> getEventsRatings(List<Long> eventIds);
}
