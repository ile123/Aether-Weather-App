package com.ile.weather.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class WebClientConfig {

    @Bean("geocodingWebClient")
    public WebClient geocodingWebClient() {
        return WebClient.builder()
                .baseUrl("https://geocoding-api.open-meteo.com/v1")
                .clientConnector(new ReactorClientHttpConnector(buildHttpClient()))
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    @Bean("forecastWebClient")
    public WebClient forecastWebClient() {
        return WebClient.builder()
                .baseUrl("https://api.open-meteo.com/v1")
                .clientConnector(new ReactorClientHttpConnector(buildHttpClient()))
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    private HttpClient buildHttpClient() {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(10))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)));
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.debug("[Open-Meteo] Request: {} {}", request.method(), request.url());
            return Mono.just(request);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            log.debug("[Open-Meteo] Response status: {}", response.statusCode());
            return Mono.just(response);
        });
    }
}