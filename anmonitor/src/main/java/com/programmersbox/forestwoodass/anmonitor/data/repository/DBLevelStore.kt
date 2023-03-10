package com.programmersbox.forestwoodass.anmonitor.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.*

class DBLevelStore  // creating a constructor for our database handler.
    (context: Context?) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    data class SampleValue(
        var sampleValue: Float = 0.0f,
        var timestamp: Long = 0L
    )
    // below method is for creating a database by running a sqlite query
    override fun onCreate(db: SQLiteDatabase) {
        val query = ("CREATE TABLE " + TABLE_NAME + " ("
                + ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + NAME_COL + " FLOAT,"
                + DURATION_COL + " LONG )"
                )

        db.execSQL(query)
    }

    fun addNewSample(
        courseName: Float?
    ) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(NAME_COL, courseName)
        values.put(DURATION_COL, Calendar.getInstance().timeInMillis)
        db.insert(TABLE_NAME, null, values)
        db.close()
    }

    fun getMostRecentSample(): SampleValue {
        val db = this.readableDatabase
        val cursorSamples: Cursor = db.rawQuery("SELECT $NAME_COL,$DURATION_COL FROM $TABLE_NAME ORDER BY ID DESC LIMIT 1", null)

        if (cursorSamples.moveToFirst()) {
            val rc =
                SampleValue(
                    cursorSamples.getFloat(0),
                    cursorSamples.getLong(1),
                )
            cursorSamples.close()
            db.close()
            return rc
        }
        cursorSamples.close()
        db.close()
        return SampleValue(0f, 0L)
    }

    fun getAllSamples(weekly: Boolean, timestamp: Long): ArrayList<SampleValue> {
        val cal = Calendar.getInstance()
        if ( timestamp != 0L ) {
            cal.timeInMillis = timestamp
        }
        val beforeTime = when ( weekly ) {
            false ->
                    cal.timeInMillis - ((Calendar.getInstance().get(Calendar.HOUR_OF_DAY)*(1000*60*60))
                        +(cal.get(Calendar.MINUTE)*(1000*60))
                            +(cal.get(Calendar.SECOND)*(1000))
                            )

            true -> {
                cal.timeInMillis - ((cal.get(Calendar.DAY_OF_WEEK)-1) * (1000*60*60*24)+
                        ((cal.get(Calendar.HOUR_OF_DAY)*(1000*60*60))
                                +(cal.get(Calendar.MINUTE)*(1000*60))
                                +(cal.get(Calendar.SECOND)*(1000))))
            }
        }
        val afterTime = beforeTime + when (weekly ) {
            false -> (1000 * 60 * 60 * 24)
            true -> (1000 * 60 * 60 * 24 * 7)
        }

        return getResults(beforeTime, afterTime)
    }

    private fun getResults(beforeTime: Long, afterTime: Long ): ArrayList<SampleValue> {
        val db = this.readableDatabase
        val cursorSamples: Cursor =
            db.rawQuery("SELECT $NAME_COL,$DURATION_COL FROM $TABLE_NAME WHERE $DURATION_COL > $beforeTime AND $DURATION_COL < $afterTime", null)
        val samplesModelArrayList: ArrayList<SampleValue> = ArrayList()

        if (cursorSamples.moveToFirst()) {
            do {
                samplesModelArrayList.add(
                    SampleValue(
                        cursorSamples.getFloat(0),
                        cursorSamples.getLong(1),
                    )
                )
            } while (cursorSamples.moveToNext())
            // moving our cursor to next.
        }
        cursorSamples.close()
        db.close()
        return samplesModelArrayList
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // this method is called to check if the table exists already.
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    companion object {
        private const val DB_NAME = "db_levels"
        private const val DB_VERSION = 1
        private const val TABLE_NAME = "raw_samples"
        private const val ID_COL = "id"
        private const val NAME_COL = "level"
        private const val DURATION_COL = "recorded_at"
    }
}