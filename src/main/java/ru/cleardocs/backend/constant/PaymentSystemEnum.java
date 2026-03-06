package ru.cleardocs.backend.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Платежные системы
 */
@Getter
@RequiredArgsConstructor
public enum PaymentSystemEnum {

    TOCHKA("TOCHKA");

    private final String paymentSystemName;

}
