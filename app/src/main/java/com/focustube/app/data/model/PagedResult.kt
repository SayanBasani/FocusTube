package com.focustube.app.data.model

/**
 * Standard pagination result contract supporting both cursor-based (continuation tokens)
 * and page-indexed pagination mechanisms across different API providers.
 */
data class PagedResult<T>(
    val items: List<T>,
    val nextCursor: String? = null,
    val hasMore: Boolean = true,
    val sourceId: String = "invidious",
    val isExhausted: Boolean = false,
    val isRetryable: Boolean = true,
    val pageNumber: Int = 1
) {
    companion object {
        fun <T> empty(sourceId: String = "unknown", isExhausted: Boolean = true): PagedResult<T> {
            return PagedResult(
                items = emptyList(),
                nextCursor = null,
                hasMore = false,
                sourceId = sourceId,
                isExhausted = isExhausted,
                isRetryable = !isExhausted
            )
        }
    }
}
