package com.projectbharat.japatap.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

@Entity(tableName="mantras")
data class MantraEntity(@PrimaryKey val id:String,val title:String,val dailyMalaTarget:Int=0,val lifetimeJapaTarget:Long=0,val sortOrder:Int=0)
@Entity(tableName="japa_logs",indices=[Index("mantraId"),Index("timestamp")])
data class JapaLog(@PrimaryKey(autoGenerate=true) val id:Long=0,val mantraId:String,val timestamp:Long,val amount:Int=1)
@Dao interface JapaDao {
 @Query("SELECT * FROM mantras ORDER BY sortOrder,title") fun mantras():Flow<List<MantraEntity>>
 @Query("SELECT * FROM mantras WHERE id=:id") suspend fun mantra(id:String):MantraEntity?
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun putMantra(m:MantraEntity)
 @Insert suspend fun log(l:JapaLog)
 @Query("SELECT COALESCE(SUM(amount),0) FROM japa_logs WHERE mantraId=:id") fun total(id:String):Flow<Long>
 @Query("SELECT COALESCE(SUM(amount),0) FROM japa_logs WHERE mantraId=:id AND timestamp>=:start AND timestamp<:end") fun period(id:String,start:Long,end:Long):Flow<Long>
 @Query("SELECT timestamp FROM japa_logs WHERE mantraId=:id ORDER BY timestamp DESC") suspend fun timestamps(id:String):List<Long>
 @Query("SELECT timestamp FROM japa_logs WHERE mantraId=:id ORDER BY timestamp DESC") suspend fun practiceTimestamps(id:String):List<Long>
 @Query("SELECT * FROM japa_logs WHERE mantraId=:id ORDER BY timestamp DESC LIMIT 1") suspend fun latest(id:String):JapaLog?
 @Query("DELETE FROM japa_logs WHERE id=:id") suspend fun deleteLog(id:Long)
}
@Database(entities=[MantraEntity::class,JapaLog::class],version=1,exportSchema=false)
abstract class JapaDatabase:RoomDatabase(){
 abstract fun dao():JapaDao
 companion object { @Volatile private var INSTANCE:JapaDatabase?=null
 fun get(context:android.content.Context):JapaDatabase=INSTANCE?: synchronized(this){INSTANCE?:Room.databaseBuilder(context.applicationContext,JapaDatabase::class.java,"japa_tap.db").build().also{INSTANCE=it}}
 }
}
object JapaDefaults {
 val mantras=listOf(MantraEntity("ram","श्री राम जय राम जय जय राम",sortOrder=0),MantraEntity("hare-krishna","हरे कृष्ण महामंत्र",sortOrder=1),MantraEntity("om-namah-shivaya","ॐ नमः शिवाय",sortOrder=2),MantraEntity("pandurang","जय जय पांडुरंग हरी",sortOrder=3),MantraEntity("digambara","दिगंबरा दिगंबरा श्रीपाद वल्लभ दिगंबरा",sortOrder=4),MantraEntity("custom","स्वतःचा मंत्र",sortOrder=5))
 suspend fun seed(dao:JapaDao){mantras.forEach{if(dao.mantra(it.id)==null)dao.putMantra(it)}}
}
fun dayBounds(date:LocalDate=LocalDate.now()):Pair<Long,Long>{val z=ZoneId.systemDefault();return date.atStartOfDay(z).toInstant().toEpochMilli() to date.plusDays(1).atStartOfDay(z).toInstant().toEpochMilli()}
