package com.sejin.diveroidrevive

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.sejin.diveroidrevive.databinding.ActivityDiveDetailBinding

/** 기록 상세: 제목·메모 편집, 삭제, 지도 열기. */
class DiveDetailActivity : AppCompatActivity() {
    private lateinit var b: ActivityDiveDetailBinding
    private lateinit var dive: Dive

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityDiveDetailBinding.inflate(layoutInflater)
        setContentView(b.root)

        dive = DiveLog.get(this, intent.getLongExtra("id", -1)) ?: run { finish(); return }
        b.header.text = DiveLog.dateTime(dive.start)

        b.title.setText(dive.title)
        b.memo.setText(dive.memo)
        b.start.text = DiveLog.time(dive.start)
        b.end.text = DiveLog.time(dive.end)
        b.duration.text = DiveLog.durationStr(this, dive.end - dive.start)
        b.startLoc.text = dive.sloc ?: getString(R.string.none)
        b.endLoc.text = dive.eloc ?: getString(R.string.none)

        val loc = dive.sloc ?: dive.eloc
        b.btnMap.visibility = if (loc == null) View.GONE else View.VISIBLE
        b.btnMap.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$loc?q=$loc(${dive.title.ifEmpty { getString(R.string.untitled) }})")))
        }

        b.btnSave.setOnClickListener {
            DiveLog.update(this, dive.copy(title = b.title.text.toString().trim(), memo = b.memo.text.toString().trim()))
            Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
            finish()
        }
        b.btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage(R.string.delete_confirm)
                .setPositiveButton(R.string.delete) { _, _ -> DiveLog.delete(this, dive.id); finish() }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }
}
