package io.github.eyalshimshi.betterstips.data

import android.content.Context
import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.api.StipsApi
import io.github.eyalshimshi.betterstips.data.model.Channel
import io.github.eyalshimshi.betterstips.data.parse.Parsers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Channels: the bundled snapshot first (instant, offline), then the live list from Stips. */
class ChannelsSource(private val context: Context, private val api: StipsApi) {
    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    @Volatile
    private var snapshot: List<Channel> = emptyList()

    suspend fun loadSnapshot() {
        if (snapshot.isNotEmpty()) return
        snapshot = withContext(Dispatchers.IO) {
            try {
                Parsers.channelsFromAsset(context.assets.open("channels.json").bufferedReader().use { it.readText() })
            } catch (e: Exception) {
                emptyList()
            }
        }
        if (_channels.value.isEmpty()) _channels.value = snapshot
    }

    suspend fun refresh() {
        loadSnapshot()
        try {
            val data = api.call(
                "smartdata.get",
                buildJsonObject {
                    put("namespace", "item")
                    put("unit", "RemoteConfig")
                },
            )
            val live = Parsers.channelsFromConfig(data)
            if (live.isNotEmpty()) {
                _channels.value = live.map { channel ->
                    val known = snapshot.firstOrNull { it.id == channel.id }
                    channel.copy(
                        nameEn = known?.nameEn,
                        icon = channel.icon.ifEmpty { known?.icon.orEmpty() },
                        colors = channel.colors.ifEmpty { known?.colors.orEmpty() },
                    )
                }
            }
        } catch (e: ApiException) {
            // The snapshot stays in use.
        }
    }

    fun byId(id: Int): Channel? = if (id == 0) null else _channels.value.firstOrNull { it.id == id }
}
