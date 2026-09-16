package com.kc3whj.channelpicker

import android.content.Context
import org.json.JSONArray

/** Mirrors the fields written by build_channel_index.py. */
data class Channel(
    val channelNumber: Int,
    val name: String,
    val section: String,
    val group: Int?,
    val slot: Int?,
    val rxMhz: Double,
    val txMhz: Double,
    val mode: String,
    val toneMode: String,
    val ctcss: Double?,
    val rxOnly: Boolean,
)

/**
 * How to turn a Channel's raw JSON channel_number (or its position/group in
 * the list) into the number actually shown on that radio's own display -
 * radios disagree on both the starting point and whether channel_number
 * itself needs an offset applied.
 */
sealed class Numbering {
    /** channel_number as-is. */
    data object Raw : Numbering()

    /**
     * channel_number + offset. IC-7300/FT-891: offset = -150 - confirmed
     * against channel_maps/ic7300.json's own notes (CH151 -> memory 1,
     * CH233 -> memory 83), 1-based, no channel 0. FT-891 shares this same
     * flat 001-099 memory architecture (its own profile notes say so
     * explicitly) and the identical CH151-233 numbering in its own
     * channels_ft891.json, so the same offset applies there too.
     */
    data class Offset(val amount: Int) : Numbering()

    /**
     * The channel's position in the list, 0-based, zero-padded to 2
     * digits.
     */
    data object Positional0 : Numbering()

    /**
     * Icom-style "Group-Slot" (e.g. "12-01"). IC-705: this public channel
     * set spans multiple CI-V memory groups (confirmed live: groups 12-16
     * across these 83 channels, slot resetting within each group) - a
     * flat channel_number (151-233) doesn't match anything the radio
     * itself shows, unlike IC-7300/FT-891's flat memory.
     */
    data object GroupSlot : Numbering()
}

fun displayNumber(numbering: Numbering, channel: Channel, indexInList: Int): String =
    when (numbering) {
        is Numbering.Raw -> channel.channelNumber.toString()
        is Numbering.Offset -> (channel.channelNumber + numbering.amount).toString()
        is Numbering.Positional0 -> String.format("%02d", indexInList)
        is Numbering.GroupSlot -> {
            val g = channel.group
            val s = channel.slot
            if (g != null && s != null) String.format("%d-%02d", g, s)
            else channel.channelNumber.toString()
        }
    }

data class RadioSource(
    val label: String,
    val assetFile: String?,
    val numbering: Numbering = Numbering.Raw,
    val note: String? = null,
)

val RADIO_SOURCES = listOf(
    RadioSource("IC-705", "channels_ic705.json", numbering = Numbering.GroupSlot),
    RadioSource("IC-7300", "channels_ic7300.json", numbering = Numbering.Offset(-150)),
    RadioSource("FT-891", "channels_ft891.json", numbering = Numbering.Offset(-150)),
)

fun loadChannels(context: Context, assetFile: String): List<Channel> {
    val json = context.assets.open(assetFile).bufferedReader().use { it.readText() }
    val array = JSONArray(json)
    val out = ArrayList<Channel>(array.length())
    for (i in 0 until array.length()) {
        val o = array.getJSONObject(i)
        out.add(
            Channel(
                channelNumber = o.getInt("channel_number"),
                name = o.getString("name"),
                section = o.optString("section", ""),
                group = if (o.isNull("group")) null else o.optInt("group"),
                slot = if (o.isNull("slot")) null else o.optInt("slot"),
                rxMhz = o.getDouble("rx_mhz"),
                txMhz = o.optDouble("tx_mhz", o.getDouble("rx_mhz")),
                mode = o.getString("mode"),
                toneMode = o.optString("tone_mode", "OFF"),
                ctcss = if (o.isNull("ctcss")) null else o.optDouble("ctcss"),
                rxOnly = o.optBoolean("rx_only", false),
            )
        )
    }
    return out
}
