package com.vyrena.mconbridge.artwork

import com.vyrena.mconbridge.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class SteamGridDbProvider(
    private val client: OkHttpClient,
    private val settings: SettingsRepository,
) : ArtworkProvider {
    override val name = "SteamGridDB"
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun search(query: ArtworkQuery): List<ArtworkCandidate> = withContext(Dispatchers.IO) {
        val apiKey = settings.steamGridDbKey() ?: return@withContext emptyList()
        val searchUrl = "https://www.steamgriddb.com/api/v2/search/autocomplete"
            .toHttpUrl().newBuilder().addPathSegment(query.title).build()
        val searchRoot = executeJson(searchUrl.toString(), apiKey)
        val games = searchRoot["data"]?.jsonArray.orEmpty()
        val rankedGames = games.mapNotNull { element ->
            val value = element.jsonObject
            val id = value["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val title = value["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            Triple(id, title, ArtworkMatchScorer.score(query.title, title))
        }.filter { it.third >= 0.45f }.sortedByDescending { it.third }.take(3)

        rankedGames.flatMap { (gameId, gameTitle, confidence) ->
            val gridsUrl = "https://www.steamgriddb.com/api/v2/grids/game/$gameId".toHttpUrl().newBuilder()
                .addQueryParameter("dimensions", "600x900,342x482,660x930")
                .addQueryParameter("types", "static")
                .addQueryParameter("nsfw", "false")
                .addQueryParameter("humor", "false")
                .build()
            val gridRoot = executeJson(gridsUrl.toString(), apiKey)
            gridRoot["data"]?.jsonArray.orEmpty().take(12).mapNotNull { element ->
                val value = element.jsonObject
                val id = value["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val imageUrl = value["url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                if (!imageUrl.startsWith("https://")) return@mapNotNull null
                val author = value["author"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull ?: "community contributor"
                ArtworkCandidate(
                    id = "sgdb:$id",
                    provider = name,
                    title = gameTitle,
                    imageUrl = imageUrl,
                    thumbnailUrl = value["thumb"]?.jsonPrimitive?.contentOrNull,
                    attribution = "Artwork by $author via SteamGridDB",
                    sourceUrl = "https://www.steamgriddb.com/grid/$id",
                    width = value["width"]?.jsonPrimitive?.intOrNull,
                    height = value["height"]?.jsonPrimitive?.intOrNull,
                    confidence = confidence,
                )
            }
        }.distinctBy(ArtworkCandidate::imageUrl)
    }

    private fun executeJson(url: String, apiKey: String) = client.newCall(
        Request.Builder().url(url).header("Authorization", "Bearer $apiKey").build(),
    ).execute().use { response ->
        if (!response.isSuccessful) error("SteamGridDB request failed (${response.code})")
        val body = response.body?.string() ?: error("SteamGridDB returned no data")
        val root = json.parseToJsonElement(body).jsonObject
        if (root["success"]?.jsonPrimitive?.contentOrNull != "true") error("SteamGridDB rejected the request")
        root
    }
}
