package ru.cleardocs.backend.mapper;

import org.springframework.stereotype.Component;
import ru.cleardocs.backend.client.tochka.TochkaClientAcquiringPaymentResponse;
import ru.cleardocs.backend.dto.TochkaPaymentResponseDto;

@Component
public class TochkaPaymentMapper {

    public TochkaPaymentResponseDto mapToPaymentResponseDto(TochkaClientAcquiringPaymentResponse clientResponse) {
        return new TochkaPaymentResponseDto(clientResponse.getData().getPaymentLink());
    }

}
