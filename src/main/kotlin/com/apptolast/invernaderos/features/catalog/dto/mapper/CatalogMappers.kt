package com.apptolast.invernaderos.features.catalog.dto.mapper

import com.apptolast.invernaderos.features.catalog.ActuatorState
import com.apptolast.invernaderos.features.catalog.AlertSeverity
import com.apptolast.invernaderos.features.catalog.AlertType
import com.apptolast.invernaderos.features.catalog.DataType
import com.apptolast.invernaderos.features.catalog.DeviceCategory
import com.apptolast.invernaderos.features.catalog.DeviceType
import com.apptolast.invernaderos.features.catalog.Period
import com.apptolast.invernaderos.features.catalog.Unit
import com.apptolast.invernaderos.features.catalog.dto.response.ActuatorStateResponse
import com.apptolast.invernaderos.features.catalog.dto.response.AlertSeverityResponse
import com.apptolast.invernaderos.features.catalog.dto.response.AlertTypeResponse
import com.apptolast.invernaderos.features.catalog.dto.response.DataTypeResponse
import com.apptolast.invernaderos.features.catalog.dto.response.DeviceCategoryResponse
import com.apptolast.invernaderos.features.catalog.dto.response.DeviceTypeResponse
import com.apptolast.invernaderos.features.catalog.dto.response.PeriodResponse
import com.apptolast.invernaderos.features.catalog.dto.response.UnitResponse

fun DeviceCategory.toResponse() = DeviceCategoryResponse(
    id = this.id ?: throw IllegalStateException("DeviceCategory ID cannot be null"),
    name = this.name
)

fun DeviceType.toResponse() = DeviceTypeResponse(
    id = this.id ?: throw IllegalStateException("DeviceType ID cannot be null"),
    name = this.name,
    description = this.description,
    categoryId = this.categoryId,
    categoryName = this.category?.name,
    defaultUnitId = this.defaultUnitId,
    defaultUnitSymbol = this.defaultUnit?.symbol,
    dataType = this.dataType,
    minExpectedValue = this.minExpectedValue,
    maxExpectedValue = this.maxExpectedValue,
    controlType = this.controlType,
    isActive = this.isActive
)

fun Unit.toResponse() = UnitResponse(
    id = this.id ?: throw IllegalStateException("Unit ID cannot be null"),
    symbol = this.symbol,
    name = this.name,
    description = this.description,
    isActive = this.isActive
)

fun AlertType.toResponse() = AlertTypeResponse(
    id = this.id ?: throw IllegalStateException("AlertType ID cannot be null"),
    name = this.name,
    description = this.description
)

fun AlertSeverity.toResponse() = AlertSeverityResponse(
    id = this.id ?: throw IllegalStateException("AlertSeverity ID cannot be null"),
    name = this.name,
    level = this.level,
    description = this.description,
    color = this.color,
    requiresAction = this.requiresAction,
    notificationDelayMinutes = this.notificationDelayMinutes,
    notifyPush = this.notifyPush
)

fun Period.toResponse() = PeriodResponse(
    id = this.id ?: throw IllegalStateException("Period ID cannot be null"),
    name = this.name
)

fun ActuatorState.toResponse() = ActuatorStateResponse(
    id = this.id ?: throw IllegalStateException("ActuatorState ID cannot be null"),
    name = this.name,
    description = this.description,
    isOperational = this.isOperational,
    displayOrder = this.displayOrder,
    color = this.color
)

fun DataType.toResponse() = DataTypeResponse(
    id = this.id ?: throw IllegalStateException("DataType ID cannot be null"),
    name = this.name,
    description = this.description,
    validationRegex = this.validationRegex,
    exampleValue = this.exampleValue,
    displayOrder = this.displayOrder,
    isActive = this.isActive
)
