package com.programmersbox.forestwoodass.anmonitor.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.util.Calendar

class DBLevelStore  // creating a constructor for our database handler.
    (context: Context?) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    data class SampleValue(
        var sampleValue: Float = 0.0f,
        var timestamp: Long = 0L
    ) {
    }
    // below method is for creating a database by running a sqlite query
    override fun onCreate(db: SQLiteDatabase) {
        // on below line we are creating
        // an sqlite query and we are
        // setting our column names
        // along with their data types.
        val query = ("CREATE TABLE " + TABLE_NAME + " ("
                + ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + NAME_COL + " FLOAT,"
                + DURATION_COL + " LONG )"
                )

        // at last we are calling a exec sql
        // method to execute above sql query
        db.execSQL(query)
    }

    // this method is use to add new course to our sqlite database.
    fun addNewSample(
        courseName: Float?
    ) {

        // on below line we are creating a variable for
        // our sqlite database and calling writable method
        // as we are writing data in our database.
        val db = this.writableDatabase

        // on below line we are creating a
        // variable for content values.
        val values = ContentValues()

        // on below line we are passing all values
        // along with its key and value pair.
        values.put(NAME_COL, courseName)


        values.put(DURATION_COL, Calendar.getInstance().timeInMillis)

        // after adding all values we are passing
        // content values to our table.
        db.insert(TABLE_NAME, null, values)

        // at last we are closing our
        // database after adding database.
        db.close()
    }

    fun getAllSamples(weekly: Boolean, dow: Int): ArrayList<SampleValue> {
        Log.d("DBLevelStore", "getting $dow and today is ${Calendar.getInstance().get(Calendar.DAY_OF_WEEK)}")
        val db = this.readableDatabase
        val beforeTime = when ( weekly ) {
            false -> if ( dow == -1 ) {
                    Calendar.getInstance().timeInMillis - ((Calendar.getInstance().get(Calendar.HOUR_OF_DAY)*(1000*60*60))
                        +(Calendar.getInstance().get(Calendar.MINUTE)*(1000*60)))
                    }else{
                        if ( dow > Calendar.getInstance().get(Calendar.DAY_OF_WEEK) ) {
                            Calendar.getInstance().timeInMillis
                        } else {
                            // Adjust this to include the current hour.
                            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                            val currentMinute = Calendar.getInstance().get(Calendar.MINUTE)
                            Calendar.getInstance().timeInMillis - (((Calendar.getInstance()
                                .get(Calendar.DAY_OF_WEEK) - dow) ) * (1000 * 60 * 60 * 24) - ((1000 * 60 * 60 * (24-currentHour))))
                        }
                    }
            true -> {
                Calendar.getInstance().timeInMillis - (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)+1) * (1000*60*60*24)
            }
        }
        val afterTime = beforeTime.toLong() + (1000*60*60*24)
        Log.d("DBLevelStore", "before ${beforeTime} and $afterTime and ${Calendar.getInstance().timeInMillis} ")
        val cursorSamples: Cursor = when ( dow ) {
            -1 -> db.rawQuery("SELECT * FROM $TABLE_NAME WHERE $DURATION_COL > $beforeTime", null)
            else -> db.rawQuery("SELECT * FROM $TABLE_NAME WHERE $DURATION_COL > $beforeTime AND $DURATION_COL < $afterTime", null)
        }
        val samplesModelArrayList: ArrayList<SampleValue> = ArrayList()

        if (cursorSamples.moveToFirst()) {
            do {
                samplesModelArrayList.add(
                    SampleValue(
                        cursorSamples.getFloat(1),
                        cursorSamples.getLong(2),
                    )
                )
            } while (cursorSamples.moveToNext())
            // moving our cursor to next.
        }

        cursorSamples.close()
        return samplesModelArrayList
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // this method is called to check if the table exists already.
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    companion object {
        // creating a constant variables for our database.
        // below variable is for our database name.
        private const val DB_NAME = "db_levels"

        // below int is our database version
        private const val DB_VERSION = 1

        // below variable is for our table name.
        private const val TABLE_NAME = "raw_samples"

        // below variable is for our id column.
        private const val ID_COL = "id"

        // below variable is for our course name column
        private const val NAME_COL = "level"

        // below variable id for our course duration column.
        private const val DURATION_COL = "recorded_at"

    }
}