package nz.co.olliechick.hivo

import androidx.room.*
import java.util.*

@Dao
interface RecordingDao {
    @Insert
    fun insert(recording: Recording): Long

    @Update
    fun update(recording: Recording)

    @Delete
    fun delete(recording: Recording)

    @Query("SELECT * FROM recordings")
    fun getAll(): List<Recording>

    @Query("""SELECT CASE WHEN EXISTS (SELECT 1 FROM recordings WHERE name = :name)
                        THEN CAST (1 AS BIT)
                        ELSE CAST (0 AS BIT) END""")
    fun nameExists(name: String): Boolean
}

@Database(entities = [Recording::class], version = 1)
@TypeConverters(HivoTypeConverters::class)
abstract class RecordingDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
}

class HivoTypeConverters {
    @TypeConverter
    fun toCalendar(l: Long?): Calendar? =
        if (l == null) null else Calendar.getInstance().apply { timeInMillis = l }

    @TypeConverter
    fun fromCalendar(c: Calendar?): Long? = c?.time?.time

}
