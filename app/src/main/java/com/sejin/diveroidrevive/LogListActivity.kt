package com.sejin.diveroidrevive

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.sejin.diveroidrevive.databinding.ActivityLogListBinding
import com.sejin.diveroidrevive.databinding.RowDiveBinding

/** 다이빙 기록 목록. 탭 = 상세, 길게 = 삭제. */
class LogListActivity : AppCompatActivity() {
    private lateinit var b: ActivityLogListBinding
    private var dives: List<Dive> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLogListBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.list.setOnItemClickListener { _, _, pos, _ ->
            startActivity(Intent(this, DiveDetailActivity::class.java).putExtra("id", dives[pos].id))
        }
        b.list.setOnItemLongClickListener { _, _, pos, _ ->
            AlertDialog.Builder(this)
                .setMessage(R.string.delete_confirm)
                .setPositiveButton(R.string.delete) { _, _ -> DiveLog.delete(this, dives[pos].id); refresh() }
                .setNegativeButton(R.string.cancel, null)
                .show()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        dives = DiveLog.all(this)
        b.empty.visibility = if (dives.isEmpty()) View.VISIBLE else View.GONE
        b.list.adapter = object : ArrayAdapter<Dive>(this, R.layout.row_dive, dives) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val row = if (convertView == null) RowDiveBinding.inflate(layoutInflater, parent, false)
                else RowDiveBinding.bind(convertView)
                val d = dives[position]
                row.date.text = "${DiveLog.dateTime(d.start)} → ${DiveLog.time(d.end)}"
                row.title.text = d.title.ifEmpty { getString(R.string.untitled) }
                row.loc.text = d.sloc ?: d.eloc ?: ""
                row.loc.visibility = if (row.loc.text.isEmpty()) View.GONE else View.VISIBLE
                row.duration.text = DiveLog.durationStr(context, d.end - d.start)
                return row.root
            }
        }
    }
}
