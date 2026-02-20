package com.forum.handler;

import com.forum.service.SearchService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class SearchHandler {

    private final SearchService searchService;

    public SearchHandler(SearchService searchService) {
        this.searchService = searchService;
    }

    public Mono<ServerResponse> search(ServerRequest request) {
        String keyword = request.queryParam("q").orElse("");
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(searchService.search(keyword), com.forum.model.Question.class);
    }
}
