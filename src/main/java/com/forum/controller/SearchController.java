package com.forum.controller;

import com.forum.activity.ActivityLogService;
import com.forum.activity.ActivityType;
import com.forum.model.Question;
import com.forum.service.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;
    private final ActivityLogService activityLogService;

    public SearchController(SearchService searchService, ActivityLogService activityLogService) {
        this.searchService = searchService;
        this.activityLogService = activityLogService;
    }

    @GetMapping
    public Flux<Question> search(@RequestParam String q, ServerWebExchange exchange) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        activityLogService.log(userId,
                ActivityType.SEARCH_PERFORMED,
                "Search: " + q,
                null,
                Map.of("query", q));

        return searchService.search(q);
    }
}
