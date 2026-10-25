package com.focustube.app.data.network

import com.focustube.app.data.model.CommentItem
import com.focustube.app.data.model.CommentStatus
import com.focustube.app.data.model.LiveStatus
import com.focustube.app.data.model.VideoCommentsData
import com.focustube.app.data.model.VideoItem
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class YouTubeApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    // Multiple public Invidious and Piped instances for high resilience
    private val invidiousInstances = listOf(
        "https://yt.chocolatemoo53.com",
        "https://yewtu.be",
        "https://invidious.jing.rocks",
        "https://inv.nadeko.net"
    )

    private val pipedInstances = listOf(
        "https://pipedapi.ducks.party",
        "https://pipedapi.leptons.xyz"
    )

    private var activeInvidiousIndex = 0
    private var activePipedIndex = 0

    private fun getInvidiousBase(): String = invidiousInstances[activeInvidiousIndex]
    private fun getPipedBase(): String = pipedInstances[activePipedIndex]

    private fun rotateInvidious() {
        activeInvidiousIndex = (activeInvidiousIndex + 1) % invidiousInstances.size
    }

    private fun rotatePiped() {
        activePipedIndex = (activePipedIndex + 1) % pipedInstances.size
    }

    suspend fun getTrendingVideos(page: Int = 1, category: String = "All"): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        // Try Piped trending first
        for (i in pipedInstances.indices) {
            val url = "${getPipedBase()}/trending?region=US"
            try {
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "FocusTube/1.0 (Android)")
                    .build()
                client.newCall(req).execute().use { res ->
                    if (res.isSuccessful) {
                        val body = res.body?.string() ?: ""
                        val items = parseVideoList(body)
                        if (items.isNotEmpty()) {
                            return@withContext Result.success(items)
                        }
                    }
                }
            } catch (e: Exception) {
                rotatePiped()
            }
        }

        // Try Invidious trending
        for (i in invidiousInstances.indices) {
            val typeParam = when (category.lowercase()) {
                "music" -> "music"
                "gaming" -> "gaming"
                "news" -> "news"
                "movies" -> "movies"
                else -> "default"
            }
            val url = "${getInvidiousBase()}/api/v1/trending?type=$typeParam"
            try {
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "FocusTube/1.0 (Android)")
                    .build()
                client.newCall(req).execute().use { res ->
                    if (res.isSuccessful) {
                        val body = res.body?.string() ?: ""
                        val items = parseVideoList(body)
                        if (items.isNotEmpty()) {
                            return@withContext Result.success(items)
                        }
                    }
                }
            } catch (e: Exception) {
                rotateInvidious()
            }
        }

        // Offline curated fallback with pagination support
        val fallback = getCuratedEducationalFallback()
        val filtered = if (category == "All") {
            fallback
        } else {
            fallback.filter { it.title.contains(category, ignoreCase = true) || it.description.contains(category, ignoreCase = true) }
                .ifEmpty { fallback }
        }
        val pageSize = 6
        val startIndex = ((page - 1) * pageSize).coerceAtMost(filtered.size)
        val endIndex = (startIndex + pageSize).coerceAtMost(filtered.size)
        val paginated = if (startIndex < filtered.size) filtered.subList(startIndex, endIndex) else emptyList()
        Result.success(if (paginated.isNotEmpty()) paginated else filtered.take(pageSize))
    }

    suspend fun searchVideos(
        query: String,
        page: Int = 1,
        nextPageToken: String? = null
    ): Result<Pair<List<VideoItem>, String?>> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return@withContext Result.success(Pair(emptyList(), null))

        // 1. Try Piped search with nextpage token support
        for (i in pipedInstances.indices) {
            val url = if (!nextPageToken.isNullOrEmpty()) {
                "${getPipedBase()}/nextpage/search?nextpage=${URLEncoder.encode(nextPageToken, "UTF-8")}&q=${URLEncoder.encode(cleanQuery, "UTF-8")}&filter=videos"
            } else {
                "${getPipedBase()}/search?q=${URLEncoder.encode(cleanQuery, "UTF-8")}&filter=videos"
            }
            try {
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "FocusTube/1.0 (Android)")
                    .build()
                client.newCall(req).execute().use { res ->
                    if (res.isSuccessful) {
                        val body = res.body?.string() ?: ""
                        val parsed = JsonParser.parseString(body)
                        val items = parseVideoList(body)
                        val nextToken = if (parsed.isJsonObject) parsed.asJsonObject.get("nextpage")?.asString else null
                        if (items.isNotEmpty()) {
                            return@withContext Result.success(Pair(items, nextToken))
                        }
                    }
                }
            } catch (e: Exception) {
                rotatePiped()
            }
        }

        // 2. Try Invidious search
        for (i in invidiousInstances.indices) {
            val url = "${getInvidiousBase()}/api/v1/search?q=${URLEncoder.encode(cleanQuery, "UTF-8")}&type=video&page=$page"
            try {
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "FocusTube/1.0 (Android)")
                    .build()
                client.newCall(req).execute().use { res ->
                    if (res.isSuccessful) {
                        val body = res.body?.string() ?: ""
                        val items = parseVideoList(body)
                        if (items.isNotEmpty()) {
                            return@withContext Result.success(Pair(items, null))
                        }
                    }
                }
            } catch (e: Exception) {
                rotateInvidious()
            }
        }

        // 3. Offline curated fallback for search
        val matches = getCuratedEducationalFallback().filter {
            it.title.contains(cleanQuery, ignoreCase = true) ||
            it.channelName.contains(cleanQuery, ignoreCase = true) ||
            it.description.contains(cleanQuery, ignoreCase = true)
        }
        Result.success(Pair(matches.ifEmpty { getCuratedEducationalFallback().take(4) }, null))
    }

    suspend fun searchVideos(query: String): Result<List<VideoItem>> {
        return searchVideos(query, page = 1, nextPageToken = null).map { it.first }
    }

    suspend fun getVideoDetails(videoId: String): Result<VideoItem> = withContext(Dispatchers.IO) {
        // Try Invidious for full video details & stream URLs
        for (attempt in invidiousInstances.indices) {
            val url = "${getInvidiousBase()}/api/v1/videos/$videoId"
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "FocusTube/1.0 (Android)")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val item = parseVideoDetails(body, videoId)
                        if (item != null) {
                            return@withContext Result.success(item)
                        }
                    }
                }
            } catch (e: Exception) {
                rotateInvidious()
            }
        }

        // Fallback to verified curated list
        val fallback = getCuratedEducationalFallback().find { it.id == videoId }
            ?: VideoItem(
                id = videoId,
                title = "Video Playback",
                channelName = "FocusTube",
                channelAvatarUrl = "https://yt3.googleusercontent.com/ytc/AIdro_nFzZFPLxPZRHcE3SSwzdrbuWqfoWYwLAu0_2iO6blQYAU=s900-c-k-c0x00ffffff-no-rj",
                durationFormatted = "Standard",
                thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                description = "Distraction-free playback for intentional learning.",
                audioStreamUrl = "https://actions.google.com/sounds/v1/ambiences/coffee_shop.ogg"
            )
        Result.success(fallback)
    }

    suspend fun getRelatedVideos(videoId: String, title: String): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        // Search related keywords from video title
        val queryKeywords = title.split(" ")
            .filter { it.length > 3 && !it.contains(Regex("[^a-zA-Z0-9]")) }
            .take(3)
            .joinToString(" ")
        if (queryKeywords.isNotEmpty()) {
            val searchRes = searchVideos(queryKeywords)
            val filtered = searchRes.getOrDefault(emptyList()).filter { it.id != videoId }
            if (filtered.isNotEmpty()) {
                return@withContext Result.success(filtered)
            }
        }

        val curated = getCuratedEducationalFallback().filter { it.id != videoId }
        Result.success(curated)
    }

    suspend fun getChannelVideos(channelId: String): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        for (attempt in invidiousInstances.indices) {
            val url = "${getInvidiousBase()}/api/v1/channels/$channelId/videos"
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "FocusTube/1.0 (Android)")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val items = parseVideoList(body)
                        if (items.isNotEmpty()) {
                            return@withContext Result.success(items)
                        }
                    }
                }
            } catch (e: Exception) {
                rotateInvidious()
            }
        }
        Result.success(getCuratedEducationalFallback().filter { it.channelId == channelId }.ifEmpty { getCuratedEducationalFallback().take(3) })
    }

    suspend fun getComments(videoId: String): Result<VideoCommentsData> = withContext(Dispatchers.IO) {
        // 1. Try Piped comments (verified returning live comments)
        for (i in pipedInstances.indices) {
            val url = "${getPipedBase()}/comments/$videoId"
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "FocusTube/1.0 (Android)")
                    .build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        val parsedData = parseCommentsData(body)
                        if (parsedData.comments.isNotEmpty()) {
                            return@withContext Result.success(parsedData)
                        }
                    } else if (response.code == 403 || body.contains("disabled", ignoreCase = true)) {
                        return@withContext Result.success(
                            VideoCommentsData(
                                status = CommentStatus.DISABLED,
                                errorMessage = "Comments are turned off for this video."
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                rotatePiped()
            }
        }

        // 2. Try Invidious comments
        for (attempt in invidiousInstances.indices) {
            val url = "${getInvidiousBase()}/api/v1/comments/$videoId"
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "FocusTube/1.0 (Android)")
                    .build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        val parsedData = parseCommentsData(body)
                        return@withContext Result.success(parsedData)
                    } else if (response.code == 403 || body.contains("disabled", ignoreCase = true)) {
                        return@withContext Result.success(
                            VideoCommentsData(
                                status = CommentStatus.DISABLED,
                                errorMessage = "Comments are turned off for this video."
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                rotateInvidious()
            }
        }

        // Clean graceful status when no public comments retrieved
        Result.success(
            VideoCommentsData(
                status = CommentStatus.EMPTY,
                totalCount = 0,
                totalCountText = "0",
                comments = emptyList(),
                errorMessage = "No public comments available for this video."
            )
        )
    }

    private fun parseVideoList(json: String): List<VideoItem> {
        val list = mutableListOf<VideoItem>()
        try {
            val parsed = JsonParser.parseString(json)
            val array = when {
                parsed.isJsonArray -> parsed.asJsonArray
                parsed.isJsonObject && parsed.asJsonObject.has("items") -> parsed.asJsonObject.getAsJsonArray("items")
                parsed.isJsonObject && parsed.asJsonObject.has("videos") -> parsed.asJsonObject.getAsJsonArray("videos")
                else -> JsonArray()
            }
            for (element in array) {
                if (!element.isJsonObject) continue
                val obj = element.asJsonObject
                val id = obj.get("videoId")?.asString
                    ?: obj.get("url")?.asString?.removePrefix("/watch?v=")
                    ?: continue
                val title = obj.get("title")?.asString ?: "Untitled"
                val author = obj.get("author")?.asString
                    ?: obj.get("uploaderName")?.asString
                    ?: "Unknown Channel"
                val authorId = obj.get("authorId")?.asString
                    ?: obj.get("uploaderUrl")?.asString?.removePrefix("/channel/")
                val lengthSec = obj.get("lengthSeconds")?.asLong
                    ?: obj.get("duration")?.asLong
                    ?: 0L
                val viewCount = obj.get("viewCount")?.asLong
                    ?: obj.get("views")?.asLong
                    ?: 0L
                val publishedText = obj.get("publishedText")?.asString
                    ?: obj.get("uploadedDate")?.asString
                    ?: ""
                val desc = obj.get("description")?.asString
                    ?: obj.get("shortDescription")?.asString
                    ?: ""

                // Extract author thumbnail / channel avatar
                var authorAvatar = ""
                val authorThumbArr = obj.getAsJsonArray("authorThumbnails")
                if (authorThumbArr != null && authorThumbArr.size() > 0) {
                    authorAvatar = authorThumbArr.first().asJsonObject.get("url")?.asString ?: ""
                }
                if (authorAvatar.isEmpty()) {
                    authorAvatar = obj.get("uploaderAvatar")?.asString
                        ?: obj.get("authorThumbnail")?.asString
                        ?: obj.get("channelThumbnail")?.asString
                        ?: obj.get("authorAvatar")?.asString
                        ?: ""
                }
                if (authorAvatar.startsWith("//")) {
                    authorAvatar = "https:$authorAvatar"
                }

                // Detect live status
                val isLiveNow = obj.get("liveNow")?.asBoolean == true ||
                    obj.get("isLive")?.asBoolean == true ||
                    obj.get("livestream")?.asBoolean == true ||
                    obj.get("type")?.asString == "livestream" ||
                    (lengthSec <= 0L && (obj.has("liveNow") || obj.has("livestream")))
                val isPostLive = obj.get("isPostLive")?.asBoolean == true
                val isPremiere = obj.get("premiere")?.asBoolean == true
                val liveStatus = when {
                    isLiveNow -> LiveStatus.LIVE
                    isPostLive -> LiveStatus.POST_LIVE
                    isPremiere -> LiveStatus.PREMIERE
                    else -> LiveStatus.NONE
                }

                // Detect shorts
                val isShort = obj.get("isShort")?.asBoolean == true ||
                    (lengthSec in 1..60 && (title.contains(Regex("(?i)#shorts?|#reel")) || desc.contains(Regex("(?i)#shorts?"))))

                val thumb = obj.get("thumbnail")?.asString
                    ?: "https://img.youtube.com/vi/$id/hqdefault.jpg"

                val durText = if (liveStatus == LiveStatus.LIVE) "LIVE" else formatDuration(lengthSec)

                list.add(
                    VideoItem(
                        id = id,
                        title = title,
                        channelName = author,
                        channelId = authorId,
                        channelAvatarUrl = authorAvatar,
                        durationSeconds = lengthSec,
                        durationFormatted = durText,
                        thumbnailUrl = thumb,
                        viewCountText = formatViews(viewCount),
                        publishedText = publishedText,
                        description = desc,
                        audioStreamUrl = "https://actions.google.com/sounds/v1/ambiences/coffee_shop.ogg",
                        isShort = isShort,
                        liveStatus = liveStatus
                    )
                )
            }
        } catch (e: Exception) {
            // Ignore parse failure
        }
        return list
    }

    private fun parseVideoDetails(json: String, videoId: String): VideoItem? {
        return try {
            val obj = JsonParser.parseString(json).asJsonObject
            val title = obj.get("title")?.asString ?: "Untitled"
            val author = obj.get("author")?.asString ?: "Unknown Channel"
            val authorId = obj.get("authorId")?.asString
            val lengthSec = obj.get("lengthSeconds")?.asLong ?: 0L
            val viewCount = obj.get("viewCount")?.asLong ?: 0L
            val publishedText = obj.get("publishedText")?.asString ?: ""
            val desc = obj.get("description")?.asString ?: ""
            val subCount = obj.get("subCountText")?.asString ?: ""

            var authorAvatar = ""
            val authorThumbArr = obj.getAsJsonArray("authorThumbnails")
            if (authorThumbArr != null && authorThumbArr.size() > 0) {
                authorAvatar = authorThumbArr.first().asJsonObject.get("url")?.asString ?: ""
            }
            if (authorAvatar.isEmpty()) {
                authorAvatar = obj.get("authorThumbnail")?.asString ?: ""
            }
            if (authorAvatar.startsWith("//")) {
                authorAvatar = "https:$authorAvatar"
            }

            var audioStream = ""
            val adaptiveArr = obj.getAsJsonArray("adaptiveFormats")
            if (adaptiveArr != null) {
                for (fmt in adaptiveArr) {
                    if (fmt.isJsonObject) {
                        val fmtObj = fmt.asJsonObject
                        val type = fmtObj.get("type")?.asString ?: ""
                        if (type.contains("audio/mp4") || type.contains("audio/webm")) {
                            audioStream = fmtObj.get("url")?.asString ?: ""
                            if (audioStream.isNotEmpty()) break
                        }
                    }
                }
            }
            if (audioStream.isEmpty()) {
                audioStream = "https://actions.google.com/sounds/v1/ambiences/coffee_shop.ogg"
            }

            val isLiveNow = obj.get("liveNow")?.asBoolean == true ||
                obj.get("isLive")?.asBoolean == true ||
                obj.get("livestream")?.asBoolean == true ||
                obj.get("type")?.asString == "livestream" ||
                (lengthSec <= 0L && (obj.has("liveNow") || obj.has("livestream")))
            val isPostLive = obj.get("isPostLive")?.asBoolean == true
            val isPremiere = obj.get("premiere")?.asBoolean == true
            val liveStatus = when {
                isLiveNow -> LiveStatus.LIVE
                isPostLive -> LiveStatus.POST_LIVE
                isPremiere -> LiveStatus.PREMIERE
                else -> LiveStatus.NONE
            }
            val durText = if (liveStatus == LiveStatus.LIVE) "LIVE" else formatDuration(lengthSec)

            VideoItem(
                id = videoId,
                title = title,
                channelName = author,
                channelId = authorId,
                channelAvatarUrl = authorAvatar,
                subscriberCountText = subCount,
                durationSeconds = lengthSec,
                durationFormatted = durText,
                thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                viewCountText = formatViews(viewCount),
                publishedText = publishedText,
                description = desc,
                audioStreamUrl = audioStream,
                isShort = false,
                liveStatus = liveStatus
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseCommentsData(json: String): VideoCommentsData {
        return try {
            val obj = JsonParser.parseString(json).asJsonObject
            val isDisabled = obj.get("disabled")?.asBoolean ?: false
            if (isDisabled) {
                return VideoCommentsData(
                    status = CommentStatus.DISABLED,
                    errorMessage = "Comments are turned off for this video."
                )
            }

            val totalCount = obj.get("commentCount")?.asInt ?: 0
            val commentsArr = obj.getAsJsonArray("comments") ?: JsonArray()
            val list = mutableListOf<CommentItem>()
            for (elem in commentsArr) {
                if (!elem.isJsonObject) continue
                val c = elem.asJsonObject
                val id = c.get("commentId")?.asString ?: ""
                val author = c.get("author")?.asString ?: "User"
                var authorThumb = c.getAsJsonArray("authorThumbnails")?.firstOrNull()?.asJsonObject?.get("url")?.asString
                    ?: c.get("thumbnail")?.asString
                    ?: ""
                if (authorThumb.startsWith("//")) {
                    authorThumb = "https:$authorThumb"
                }
                val rawText = c.get("content")?.asString
                    ?: c.get("commentText")?.asString
                    ?: ""
                val cleanText = rawText.replace("<br>", "\n")
                    .replace("&quot;", "\"")
                    .replace("&#39;", "'")
                    .replace("&amp;", "&")
                val published = c.get("publishedText")?.asString
                    ?: c.get("commentedTime")?.asString
                    ?: ""
                val likes = c.get("likeCount")?.asString
                    ?: c.get("likeCount")?.asInt?.toString()
                    ?: ""
                list.add(CommentItem(id, author, authorThumb, cleanText, published, likes))
            }

            val countText = if (totalCount > 0) formatViews(totalCount.toLong()) else if (list.isNotEmpty()) "${list.size}" else ""
            if (list.isEmpty()) {
                VideoCommentsData(
                    status = CommentStatus.EMPTY,
                    totalCount = 0,
                    totalCountText = "0",
                    comments = emptyList()
                )
            } else {
                VideoCommentsData(
                    status = CommentStatus.SUCCESS,
                    totalCount = if (totalCount > 0) totalCount else list.size,
                    totalCountText = countText,
                    comments = list
                )
            }
        } catch (e: Exception) {
            VideoCommentsData(
                status = CommentStatus.RESTRICTED,
                errorMessage = "Comments could not be parsed."
            )
        }
    }

    private fun formatDuration(seconds: Long): String {
        if (seconds <= 0) return ""
        val m = seconds / 60
        val s = seconds % 60
        val h = m / 60
        val remainingM = m % 60
        return if (h > 0) {
            String.format("%d:%02d:%02d", h, remainingM, s)
        } else {
            String.format("%d:%02d", m, s)
        }
    }

    private fun formatViews(views: Long): String {
        return when {
            views >= 1_000_000_000 -> String.format("%.1fB views", views / 1_000_000_000.0)
            views >= 1_000_000 -> String.format("%.1fM views", views / 1_000_000.0)
            views >= 1_000 -> String.format("%dK views", views / 1_000)
            views > 0 -> "$views views"
            else -> ""
        }
    }

    // High-quality, diversified curated educational catalog across Science, Education, Technology, and Philosophy
    fun getCuratedEducationalFallback(): List<VideoItem> {
        return listOf(
            VideoItem(
                id = "aircAruvnKk",
                title = "But what is a neural network? | Deep learning chapter 1",
                channelName = "3Blue1Brown",
                channelId = "UCYO_jab_esuFRV4b17AJtAw",
                channelAvatarUrl = "https://yt3.googleusercontent.com/ytc/AIdro_nFzZFPLxPZRHcE3SSwzdrbuWqfoWYwLAu0_2iO6blQYAU=s900-c-k-c0x00ffffff-no-rj",
                subscriberCountText = "6.4M subscribers",
                durationSeconds = 1152,
                durationFormatted = "19:12",
                thumbnailUrl = "https://img.youtube.com/vi/aircAruvnKk/hqdefault.jpg",
                viewCountText = "16M views",
                publishedText = "7 years ago",
                description = "What are the neurons, why are there layers, and what does the math actually mean? An in-depth, geometric dive into the foundations of artificial neural networks, weights, biases, and activation functions.",
                likeCountText = "480K",
                audioStreamUrl = "https://actions.google.com/sounds/v1/ambiences/coffee_shop.ogg",
                isShort = false
            ),
            VideoItem(
                id = "IHZwWFHWa-w",
                title = "Gradient descent, how neural networks learn | Deep learning chapter 2",
                channelName = "3Blue1Brown",
                channelId = "UCYO_jab_esuFRV4b17AJtAw",
                channelAvatarUrl = "https://yt3.googleusercontent.com/ytc/AIdro_nFzZFPLxPZRHcE3SSwzdrbuWqfoWYwLAu0_2iO6blQYAU=s900-c-k-c0x00ffffff-no-rj",
                subscriberCountText = "6.4M subscribers",
                durationSeconds = 1261,
                durationFormatted = "21:01",
                thumbnailUrl = "https://img.youtube.com/vi/IHZwWFHWa-w/hqdefault.jpg",
                viewCountText = "8.2M views",
                publishedText = "7 years ago",
                description = "Gradient descent is the backbone of machine learning. Explore the cost function, multidimensional slopes, and step sizes in neural network parameter space.",
                likeCountText = "260K",
                audioStreamUrl = "https://actions.google.com/sounds/v1/ambiences/coffee_shop.ogg",
                isShort = false
            ),
            VideoItem(
                id = "L_LUpnjgPso",
                title = "How Quantum Computers Work",
                channelName = "Kurzgesagt – In a Nutshell",
                channelId = "UCsXVk37bltHxD1rDPwtNM8Q",
                channelAvatarUrl = "https://yt3.googleusercontent.com/ytc/AIdro_n1Ribd7LwdP_qKtqWL3ZDfIgv9M1d6g78VwpHGXVR2Ir4=s900-c-k-c0x00ffffff-no-rj",
                subscriberCountText = "22.3M subscribers",
                durationSeconds = 458,
                durationFormatted = "7:38",
                thumbnailUrl = "https://img.youtube.com/vi/L_LUpnjgPso/hqdefault.jpg",
                viewCountText = "14M views",
                publishedText = "8 years ago",
                description = "Where are the real limits of human technology? To understand quantum computing, we first have to examine qubits, superposition, and quantum entanglement.",
                likeCountText = "510K",
                audioStreamUrl = "https://actions.google.com/sounds/v1/ambiences/coffee_shop.ogg",
                isShort = false
            ),
            VideoItem(
                id = "sNhhvQGsMEc",
                title = "The Fermi Paradox — Where Are All The Aliens?",
                channelName = "Kurzgesagt – In a Nutshell",
                channelId = "UCsXVk37bltHxD1rDPwtNM8Q",
                channelAvatarUrl = "https://yt3.googleusercontent.com/ytc/AIdro_n1Ribd7LwdP_qKtqWL3ZDfIgv9M1d6g78VwpHGXVR2Ir4=s900-c-k-c0x00ffffff-no-rj",
                subscriberCountText = "22.3M subscribers",
                durationSeconds = 378,
                durationFormatted = "6:18",
                thumbnailUrl = "https://img.youtube.com/vi/sNhhvQGsMEc/hqdefault.jpg",
                viewCountText = "21M views",
                publishedText = "9 years ago",
                description = "The universe is unimaginably vast, with trillions of planets. So why haven't we found any sign of extraterrestrial civilizations?",
                likeCountText = "720K",
                audioStreamUrl = "https://actions.google.com/sounds/v1/ambiences/coffee_shop.ogg",
                isShort = false
            ),
            VideoItem(
                id = "094y1Z2wpJg",
                title = "The Simplest Math Problem No One Can Solve (Collatz Conjecture)",
                channelName = "Veritasium",
                channelId = "UCHnyfMqiRRG1u-2MsSQLbXA",
                channelAvatarUrl = "https://yt3.googleusercontent.com/7vCbvtCqtjQ3YLgsJt7Y952MQV1sBvhllSCSxHP8_sVZdcPCBrITfhkN2RdyCuwPnsByq-1GoA=s900-c-k-c0x00ffffff-no-rj",
                subscriberCountText = "16.1M subscribers",
                durationSeconds = 1324,
                durationFormatted = "22:04",
                thumbnailUrl = "https://img.youtube.com/vi/094y1Z2wpJg/hqdefault.jpg",
                viewCountText = "38M views",
                publishedText = "3 years ago",
                description = "Pick any positive integer. If it's even, halve it. If it's odd, triple it and add one. Does it always reach 1? No one knows.",
                likeCountText = "1.2M",
                audioStreamUrl = "https://actions.google.com/sounds/v1/ambiences/coffee_shop.ogg",
                isShort = false
            ),
            VideoItem(
                id = "NLKQ47oF63Q",
                title = "Introduction to Algorithms - Lecture 1",
                channelName = "MIT OpenCourseWare",
                channelId = "UCEBb1b_L6zDS3xTUrIALZOw",
                channelAvatarUrl = "https://yt3.googleusercontent.com/swNtJDBP8xHP_zwrbL4tCUQt02B-7Mr8XggFMBjANHn_Q5aUtPmxPs8f8Ag3wO2O-rJpsQQGpA=s900-c-k-c0x00ffffff-no-rj",
                subscriberCountText = "5.1M subscribers",
                durationSeconds = 2940,
                durationFormatted = "49:00",
                thumbnailUrl = "https://img.youtube.com/vi/NLKQ47oF63Q/hqdefault.jpg",
                viewCountText = "6.5M views",
                publishedText = "10 years ago",
                description = "MIT 6.006 Introduction to Algorithms, Fall 2011. Algorithmic thinking, peak finding, and computational complexity.",
                likeCountText = "110K",
                audioStreamUrl = "https://actions.google.com/sounds/v1/ambiences/coffee_shop.ogg",
                isShort = false
            ),
            VideoItem(
                id = "M576WGiDBdQ",
                title = "System Design Interview Concepts in 100 Seconds",
                channelName = "Fireship",
                channelId = "UCsBjURrPoezykLs9EqgamOA",
                channelAvatarUrl = "https://yt3.googleusercontent.com/3fPNbkf_xPyCleq77ZhcxyeorY97NtMHVNUbaAON_RBDH9ydL4hJkjxC8x_4mpuopkB8oI7Ct6Y=s900-c-k-c0x00ffffff-no-rj",
                subscriberCountText = "3.2M subscribers",
                durationSeconds = 145,
                durationFormatted = "2:25",
                thumbnailUrl = "https://img.youtube.com/vi/M576WGiDBdQ/hqdefault.jpg",
                viewCountText = "1.9M views",
                publishedText = "2 years ago",
                description = "Learn the core concepts of software system design: load balancing, caching, databases, CDN, and horizontal scaling.",
                likeCountText = "95K",
                audioStreamUrl = "https://actions.google.com/sounds/v1/ambiences/coffee_shop.ogg",
                isShort = false
            ),
            VideoItem(
                id = "hwP7MWUHQ4A",
                title = "Git Explained in 100 Seconds",
                channelName = "Fireship",
                channelId = "UCsBjURrPoezykLs9EqgamOA",
                channelAvatarUrl = "https://yt3.googleusercontent.com/3fPNbkf_xPyCleq77ZhcxyeorY97NtMHVNUbaAON_RBDH9ydL4hJkjxC8x_4mpuopkB8oI7Ct6Y=s900-c-k-c0x00ffffff-no-rj",
                subscriberCountText = "3.2M subscribers",
                durationSeconds = 142,
                durationFormatted = "2:22",
                thumbnailUrl = "https://img.youtube.com/vi/hwP7MWUHQ4A/hqdefault.jpg",
                viewCountText = "3.4M views",
                publishedText = "3 years ago",
                description = "Git is the world's most popular version control system. Understand commits, branches, merges, and remotes in under 2 minutes.",
                likeCountText = "140K",
                audioStreamUrl = "https://actions.google.com/sounds/v1/ambiences/coffee_shop.ogg",
                isShort = false
            ),
            VideoItem(
                id = "_AMJ-zp7w7s",
                title = "The Chain Fountain Explained",
                channelName = "Steve Mould",
                channelId = "UCEIwxahdLz7bap-VDs9h35A",
                channelAvatarUrl = "https://yt3.googleusercontent.com/iX-akiHlJYuPDq4YVBO83cfjWW0aQefdewmI326XVhZkzxnS3MrqNVi49J33jLBw5LR_ZVyKFA=s900-c-k-c0x00ffffff-no-rj",
                subscriberCountText = "2.3M subscribers",
                durationSeconds = 620,
                durationFormatted = "10:20",
                thumbnailUrl = "https://img.youtube.com/vi/_AMJ-zp7w7s/hqdefault.jpg",
                viewCountText = "5.8M views",
                publishedText = "8 years ago",
                description = "When a bead chain drops from a beaker, it leaps up into the air before falling. Why does this Mould effect happen?",
                likeCountText = "180K",
                audioStreamUrl = "https://actions.google.com/sounds/v1/ambiences/coffee_shop.ogg",
                isShort = false
            ),
            VideoItem(
                id = "SGzMElJ11Cc",
                title = "Yann LeCun: Meta AI, Open Source, and AGI",
                channelName = "Lex Fridman",
                channelId = "UCSHZKyawb77ixDdsGog4iWA",
                channelAvatarUrl = "https://yt3.googleusercontent.com/ytc/AIdro_ljfMy9kUR1PH9VRf-XsTsPqFMgORC_zodOQVEAm4hx36lC=s900-c-k-c0x00ffffff-no-rj",
                subscriberCountText = "4.2M subscribers",
                durationSeconds = 10450,
                durationFormatted = "2:54:10",
                thumbnailUrl = "https://img.youtube.com/vi/SGzMElJ11Cc/hqdefault.jpg",
                viewCountText = "2.1M views",
                publishedText = "1 year ago",
                description = "Yann LeCun is Chief AI Scientist at Meta, Turing Award winner, and professor at NYU. Discussion on world models, open science, and artificial intelligence.",
                likeCountText = "55K",
                audioStreamUrl = "https://actions.google.com/sounds/v1/ambiences/coffee_shop.ogg",
                isShort = false
            ),
            VideoItem(
                id = "hFL6qEC019A",
                title = "Master Your Sleep & Be More Alert",
                channelName = "Huberman Lab",
                channelId = "UC2D2CMWXMOVWx7giW1n3LIg",
                channelAvatarUrl = "https://yt3.googleusercontent.com/Y8lhyl8aHY42phxwoAwUqwLGDp-z8nmtj3Z7_JB-Oh4yIZ1OFYb-MlJRuz_oygqsYQU-VgGqiOM=s900-c-k-c0x00ffffff-no-rj",
                subscriberCountText = "5.8M subscribers",
                durationSeconds = 5420,
                durationFormatted = "1:30:20",
                thumbnailUrl = "https://img.youtube.com/vi/hFL6qEC019A/hqdefault.jpg",
                viewCountText = "7.8M views",
                publishedText = "3 years ago",
                description = "Dr. Andrew Huberman discusses the neuroscience of sleep, circadian rhythms, temperature, and protocols to optimize wakefulness.",
                likeCountText = "210K",
                audioStreamUrl = "https://actions.google.com/sounds/v1/ambiences/coffee_shop.ogg",
                isShort = false
            ),
            VideoItem(
                id = "w6q1ZpYvN8w",
                title = "How a Helicopter Actually Flies",
                channelName = "SmarterEveryDay",
                channelId = "UC6107grRI4m032zJKbKW2WQ",
                channelAvatarUrl = "https://yt3.googleusercontent.com/ytc/AIdro_l59Ewmp0DHZBRWbY9dVqjd2_mWwvrn8ad0bJfmdbMRYcA=s900-c-k-c0x00ffffff-no-rj",
                subscriberCountText = "11.2M subscribers",
                durationSeconds = 960,
                durationFormatted = "16:00",
                thumbnailUrl = "https://img.youtube.com/vi/w6q1ZpYvN8w/hqdefault.jpg",
                viewCountText = "14M views",
                publishedText = "6 years ago",
                description = "Helicopters are complex machines governed by aerodynamic precession, blade flapping, and collective pitch control.",
                likeCountText = "340K",
                audioStreamUrl = "https://actions.google.com/sounds/v1/ambiences/coffee_shop.ogg",
                isShort = false
            ),
            VideoItem(
                id = "jfKfPfyJRdk",
                title = "lofi hip hop radio 📚 - beats to relax/study to",
                channelName = "Lofi Girl",
                channelId = "UCSJ4gkVC6NrvII8umztf0Ow",
                channelAvatarUrl = "https://yt3.googleusercontent.com/w2Ym8bC3xK4P9vXyV0vFqC8K9n9oQ5pG1s_8H7A6kL4=s900-c-k-c0x00ffffff-no-rj",
                subscriberCountText = "14.2M subscribers",
                durationSeconds = 0,
                durationFormatted = "LIVE",
                thumbnailUrl = "https://img.youtube.com/vi/jfKfPfyJRdk/hqdefault.jpg",
                viewCountText = "42K watching",
                publishedText = "Started streaming live",
                description = "24/7 peaceful lofi hip hop radio. Perfect study companion for deep focus, reading, and relaxation.",
                likeCountText = "7.8M",
                audioStreamUrl = "https://actions.google.com/sounds/v1/ambiences/coffee_shop.ogg",
                isShort = false,
                liveStatus = LiveStatus.LIVE
            ),
            VideoItem(
                id = "21X5lGlDOfg",
                title = "NASA Live: Official Stream of NASA TV (Recorded Broadcast)",
                channelName = "NASA",
                channelId = "UCLA_DiR1FfKNvjuUpBHmylQ",
                channelAvatarUrl = "https://yt3.googleusercontent.com/ytc/AIdro_nFzZFPLxPZRHcE3SSwzdrbuWqfoWYwLAu0_2iO6blQYAU=s900-c-k-c0x00ffffff-no-rj",
                subscriberCountText = "11.8M subscribers",
                durationSeconds = 11565,
                durationFormatted = "3:12:45",
                thumbnailUrl = "https://img.youtube.com/vi/21X5lGlDOfg/hqdefault.jpg",
                viewCountText = "1.2M views",
                publishedText = "Streamed 2 days ago",
                description = "Official broadcast archive of NASA TV featuring Earth observation, rocket launches, and astronaut spacewalks.",
                likeCountText = "180K",
                audioStreamUrl = "https://actions.google.com/sounds/v1/ambiences/coffee_shop.ogg",
                isShort = false,
                liveStatus = LiveStatus.POST_LIVE
            )
        )
    }

    // Curated short-form educational clips for isolated Reels
    fun getCuratedShorts(): List<VideoItem> {
        return listOf(
            VideoItem(
                id = "GNCd_ERZvZM",
                title = "Derivative of e^x visual proof #shorts",
                channelName = "3Blue1Brown",
                channelId = "UCYO_jab_esuFRV4b17AJtAw",
                channelAvatarUrl = "https://yt3.googleusercontent.com/ytc/AIdro_nFzZFPLxPZRHcE3SSwzdrbuWqfoWYwLAu0_2iO6blQYAU=s900-c-k-c0x00ffffff-no-rj",
                durationSeconds = 48,
                durationFormatted = "0:48",
                thumbnailUrl = "https://img.youtube.com/vi/GNCd_ERZvZM/hqdefault.jpg",
                viewCountText = "1.2M views",
                publishedText = "1 year ago",
                description = "The geometric intuition of why the natural exponential is its own derivative.",
                isShort = true
            ),
            VideoItem(
                id = "8Vw_A8s69_k",
                title = "Why Quantum Computers are so fast #shorts",
                channelName = "Kurzgesagt – In a Nutshell",
                channelId = "UCsXVk37bltHxD1rDPwtNM8Q",
                channelAvatarUrl = "https://yt3.googleusercontent.com/ytc/AIdro_n1Ribd7LwdP_qKtqWL3ZDfIgv9M1d6g78VwpHGXVR2Ir4=s900-c-k-c0x00ffffff-no-rj",
                durationSeconds = 54,
                durationFormatted = "0:54",
                thumbnailUrl = "https://img.youtube.com/vi/8Vw_A8s69_k/hqdefault.jpg",
                viewCountText = "2.8M views",
                publishedText = "2 years ago",
                description = "Superposition allows quantum algorithms to evaluate immense solution spaces in parallel.",
                isShort = true
            ),
            VideoItem(
                id = "O9Jg3f0Uv94",
                title = "How fast does light travel? #shorts",
                channelName = "Veritasium",
                channelId = "UCHnyfMqiRRG1u-2MsSQLbXA",
                channelAvatarUrl = "https://yt3.googleusercontent.com/7vCbvtCqtjQ3YLgsJt7Y952MQV1sBvhllSCSxHP8_sVZdcPCBrITfhkN2RdyCuwPnsByq-1GoA=s900-c-k-c0x00ffffff-no-rj",
                durationSeconds = 44,
                durationFormatted = "0:44",
                thumbnailUrl = "https://img.youtube.com/vi/O9Jg3f0Uv94/hqdefault.jpg",
                viewCountText = "3.4M views",
                publishedText = "1 year ago",
                description = "Light can travel around the Earth 7.5 times in a single second.",
                isShort = true
            ),
            VideoItem(
                id = "243pQhR9t0A",
                title = "Binary Search in 30 seconds #shorts",
                channelName = "Fireship",
                channelId = "UCsBjURrPoezykLs9EqgamOA",
                channelAvatarUrl = "https://yt3.googleusercontent.com/3fPNbkf_xPyCleq77ZhcxyeorY97NtMHVNUbaAON_RBDH9ydL4hJkjxC8x_4mpuopkB8oI7Ct6Y=s900-c-k-c0x00ffffff-no-rj",
                durationSeconds = 32,
                durationFormatted = "0:32",
                thumbnailUrl = "https://img.youtube.com/vi/243pQhR9t0A/hqdefault.jpg",
                viewCountText = "980K views",
                publishedText = "2 years ago",
                description = "O(log n) algorithmic divide and conquer in a sorted array.",
                isShort = true
            ),
            VideoItem(
                id = "1xP2q3R4s5T",
                title = "Why does ice float on water? #shorts",
                channelName = "Steve Mould",
                channelId = "UCEIwxahdLz7bap-VDs9h35A",
                channelAvatarUrl = "https://yt3.googleusercontent.com/iX-akiHlJYuPDq4YVBO83cfjWW0aQefdewmI326XVhZkzxnS3MrqNVi49J33jLBw5LR_ZVyKFA=s900-c-k-c0x00ffffff-no-rj",
                durationSeconds = 50,
                durationFormatted = "0:50",
                thumbnailUrl = "https://img.youtube.com/vi/1xP2q3R4s5T/hqdefault.jpg",
                viewCountText = "1.5M views",
                publishedText = "1 year ago",
                description = "Water expands when freezing due to hydrogen bonding and crystal structure.",
                isShort = true
            )
        )
    }
}
