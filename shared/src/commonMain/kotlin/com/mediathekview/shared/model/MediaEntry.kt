package com.mediathekview.shared.model

/**
 * Represents a single media entry from the film list.
 * Platform-agnostic version for shared code.
 */
data class MediaEntry(
    val channel: String = "",
    val theme: String = "",
    val title: String = "",
    val date: String = "",
    val time: String = "",
    val duration: String = "",
    val sizeMB: String = "",
    val description: String = "",
    val url: String = "",
    val website: String = "",
    val subtitleUrl: String = "",
    val urlSmall: String = "",
    val urlHd: String = "",
    val dateL: Long = 0,
    val geo: String = "",
    val isNew: Boolean = false,
    var inTimePeriod: Boolean = true
) {
    companion object {
        /**
         * Factory method to create MediaEntry from array, handling empty strings
         * by inheriting from previous entry.
         */
        fun fromArray(arr: Array<String>, previous: MediaEntry?): MediaEntry {
            fun getOrInherit(index: Int, previousValue: String): String =
                if (arr.size > index && arr[index].isNotEmpty()) arr[index]
                else previousValue

            val dateL = if (arr.size > 16) {
                arr[16].toLongOrNull() ?: 0L
            } else {
                0L
            }

            val isNew = arr.getOrNull(19)?.lowercase() == "true"

            return MediaEntry(
                channel = getOrInherit(0, previous?.channel ?: ""),
                theme = getOrInherit(1, previous?.theme ?: ""),
                title = arr.getOrNull(2) ?: "",
                date = arr.getOrNull(3) ?: "",
                time = arr.getOrNull(4) ?: "",
                duration = arr.getOrNull(5) ?: "",
                sizeMB = arr.getOrNull(6) ?: "",
                description = arr.getOrNull(7) ?: "",
                url = arr.getOrNull(8) ?: "",
                website = arr.getOrNull(9) ?: "",
                subtitleUrl = arr.getOrNull(10) ?: "",
                urlSmall = arr.getOrNull(12) ?: "",
                urlHd = arr.getOrNull(14) ?: "",
                dateL = dateL,
                geo = arr.getOrNull(18) ?: "",
                isNew = isNew,
                inTimePeriod = true
            )
        }
    }

    /**
     * Get the best available video URL (HD > Normal > Small)
     */
    fun getBestUrl(): String = when {
        urlHd.isNotBlank() -> urlHd
        url.isNotBlank() -> url
        else -> urlSmall
    }
}
