package com.apptolast.invernaderos.features.alert.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Generic paginated response wrapper")
data class PagedResponse<T>(
    @Schema(description = "Items on the current page") val items: List<T>,
    @Schema(description = "Zero-based page index", example = "0") val page: Int,
    @Schema(description = "Page size", example = "50") val size: Int,
    @Schema(description = "Total number of items matching the query", example = "243") val total: Long,
    @Schema(description = "True if there are more pages after this one") val hasMore: Boolean,
)
