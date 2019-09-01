package nz.co.olliechick.hivo.util

import android.content.Context
import androidx.room.Room
import nz.co.olliechick.hivo.RecordingDatabase

class Database {
    companion object {

        fun initialiseDb(applicationContext: Context): RecordingDatabase {
            return Room.databaseBuilder(
                applicationContext,
                RecordingDatabase::class.java, "db"
            ).build()
        }
    }
}