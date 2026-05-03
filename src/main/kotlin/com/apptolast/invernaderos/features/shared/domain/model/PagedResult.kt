package com.apptolast.invernaderos.features.shared.domain.model

data class PagedResult<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val total: Long,
    val hasMore: Boolean,
)
