package ru.cleardocs.backend.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Статусы платежей <br>
 * <li>
 *     <ul>CREATED Создан</ul>
 *     <ul>PENDING Создан, ожидает оплаты</ul>
 * </li>
 */
@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
    CREATED("CREATED"),
    PENDING("PENDING");

    private final String status;

}
