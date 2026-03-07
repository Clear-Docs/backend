package ru.cleardocs.backend.client.tochka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TochkaSetSubscriptionStatusRequest {

    @JsonProperty("Data")
    private StatusData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StatusData {

        private String status;
    }

    public static TochkaSetSubscriptionStatusRequest cancelled() {
        StatusData data = new StatusData();
        data.setStatus("Cancelled");
        return TochkaSetSubscriptionStatusRequest.builder()
                .data(data)
                .build();
    }
}
