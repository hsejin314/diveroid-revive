package com.sejin.diveroidrevive

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 다이빙 1회. id = 시작 시각. 위치는 "lat,lon" 문자열. */
data class Dive(
    val id: Long,
    val start: Long,
    val end: Long,
    val sloc: String? = null,
    val eloc: String? = null,
    val title: String = "",
    val memo: String = "",
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("start", start).put("end", end)
        .put("sloc", sloc ?: "").put("eloc", eloc ?: "")
        .put("title", title).put("memo", memo)

    companion object {
        fun from(o: JSONObject) = Dive(
            id = o.optLong("id", o.getLong("start")),
            start = o.getLong("start"),
            end = o.getLong("end"),
            sloc = o.optString("sloc").ifEmpty { null },
            eloc = o.optString("eloc").ifEmpty { null },
            title = o.optString("title"),
            memo = o.optString("memo"),
        )
    }
}

/** 앱 내부 파일에 한 줄 JSON. DB 없음. 수십~수백 건 규모라 매번 전체 읽고 씀. */
object DiveLog {
    private fun file(c: Context) = File(c.filesDir, "dives.jsonl")

    /** 최신이 앞 */
    fun all(c: Context): List<Dive> {
        val f = file(c)
        if (!f.exists()) return emptyList()
        return f.readLines().filter { it.isNotBlank() }.map { Dive.from(JSONObject(it)) }.sortedByDescending { it.start }
    }

    private fun write(c: Context, dives: List<Dive>) {
        file(c).writeText(dives.sortedBy { it.start }.joinToString("\n") { it.toJson().toString() } + "\n")
    }

    fun get(c: Context, id: Long): Dive? = all(c).firstOrNull { it.id == id }
    fun add(c: Context, d: Dive) = write(c, all(c) + d)
    fun update(c: Context, d: Dive) = write(c, all(c).map { if (it.id == d.id) d else it })
    fun delete(c: Context, id: Long) = write(c, all(c).filter { it.id != id })

    /** 종료 위치는 수면 복귀 후 늦게 잡히므로 가장 최근 기록을 갱신. 제목이 비어 있으면 종료 지명으로 채움 */
    fun setLastEndLoc(c: Context, loc: String, place: String? = null) {
        all(c).firstOrNull()?.let { d ->
            update(c, d.copy(eloc = loc, title = d.title.ifEmpty { place ?: "" }))
        }
    }

    /** 다이빙 종료 공통: 기록 추가 + 진행 상태 초기화 + 종료 위치/지명 비동기 갱신 */
    fun finishDive(c: Context, prefs: Prefs) {
        val start = prefs.diveStart
        add(c, Dive(id = start, start = start, end = System.currentTimeMillis(), sloc = prefs.diveStartLoc, title = prefs.diveStartPlace ?: ""))
        prefs.diveStart = 0L
        prefs.diveStartLoc = null
        prefs.diveStartPlace = null
        val app = c.applicationContext
        Gps.current(app) { loc ->
            loc ?: return@current
            Gps.place(app, loc) { place -> setLastEndLoc(app, Gps.fmt(loc), place) }
        }
    }

    fun durationStr(c: Context, ms: Long): String {
        val m = ms / 60_000
        return if (m >= 60) c.getString(R.string.hours_min_fmt, m / 60, m % 60) else c.getString(R.string.min_fmt, m)
    }

    fun dateTime(ms: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ms))
    fun time(ms: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))
}
