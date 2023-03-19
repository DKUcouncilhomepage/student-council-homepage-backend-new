package com.dku.council.infra.bus.provider;

import com.dku.council.domain.bus.model.BusStation;
import com.dku.council.global.error.exception.UnexpectedResponseException;
import com.dku.council.infra.bus.exception.CannotGetBusArrivalException;
import com.dku.council.infra.bus.model.BusArrival;
import com.dku.council.infra.bus.model.ResponseGGBusArrival;
import com.dku.council.infra.bus.model.mapper.BusResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GGBusProvider implements BusArrivalProvider {

    private final WebClient webClient;

    @Value("${bus.ggbus.api-path}")
    private final String apiPath;

    @Value("${bus.ggbus.key}")
    private final String serviceKey;


    public List<BusArrival> retrieveBusArrival(BusStation station) {
        try {
            ResponseGGBusArrival response = request(station.getGgNodeId());

            if (response == null) {
                throw new UnexpectedResponseException("Failed response");
            }

            ResponseGGBusArrival.Header header = response.getMsgHeader();
            if (header == null) {
                return List.of();
            }

            Integer code = header.getResultCode();

            if (code == 4) {
                return List.of();
            }

            if (code != 0) {
                throw new UnexpectedResponseException(header.getResultMessage());
            }

            return response.getMsgBody().getBusArrivalList().stream()
                    .map(BusResponseMapper::to)
                    .collect(Collectors.toList());
        } catch (Throwable e) {
            throw new CannotGetBusArrivalException(e);
        }
    }

    @Override
    public String getProviderPrefix() {
        return "GG_";
    }

    private ResponseGGBusArrival request(String stationId) {
        URI uri = UriComponentsBuilder.fromHttpUrl(apiPath)
                .queryParam("serviceKey", serviceKey)
                .queryParam("stationId", stationId)
                .build()
                .toUri();

        // TODO 비동기 방식으로 처리해보기
        return webClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)
                .retrieve()
                .bodyToMono(ResponseGGBusArrival.class)
                .block();
    }
}
