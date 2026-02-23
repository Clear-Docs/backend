package ru.cleardocs.backend.dto;

import java.util.List;

public record GetConnectorsDto(List<EntityConnectorDto> connectors) {
}
