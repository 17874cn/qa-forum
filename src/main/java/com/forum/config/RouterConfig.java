package com.forum.config;

import com.forum.handler.SearchHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

@Configuration
public class RouterConfig {

    @Bean
    public RouterFunction<ServerResponse> searchRoutes(SearchHandler searchHandler) {
        return RouterFunctions.route()
                .GET("/api/v2/search", searchHandler::search)
                .build();
    }
}
