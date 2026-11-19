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
 val mantras=listOf(
  MantraEntity("om-namah-shivaya","ॐ नमः शिवाय",sortOrder=0),
  MantraEntity("maha-mrityunjaya","महामृत्युंजय मंत्र",sortOrder=1),
  MantraEntity("vasudevaya","ॐ नमो भगवते वासुदेवाय",sortOrder=2),
  MantraEntity("hare-krishna","हरे कृष्ण हरे कृष्ण, कृष्ण कृष्ण हरे हरे। हरे राम हरे राम, राम राम हरे हरे॥",sortOrder=3),
  MantraEntity("ganapataye","ॐ गं गणपतये नमः",sortOrder=4),
  MantraEntity("durgaye","ॐ दुं दुर्गायै नमः",sortOrder=5),
  MantraEntity("gayatri","गायत्री मंत्र",sortOrder=6),
  MantraEntity("ram","श्री राम जय राम जय जय राम",sortOrder=7),
  MantraEntity("hanumate","ॐ हनुमते नमः",sortOrder=8),
  MantraEntity("guru","गुरु मंत्र",sortOrder=9),
  MantraEntity("om","ॐ",sortOrder=10),
  MantraEntity("lokah","लोकाः समस्ताः सुखिनो भवन्तु",sortOrder=11),
  MantraEntity("ram-krishna-hari","राम कृष्ण हरी",sortOrder=12),
  MantraEntity("narayanaya","ॐ नमो नारायणाय",sortOrder=13),
  MantraEntity("dattatreya","ॐ द्रां दत्तात्रेयाय नमः",sortOrder=14),
  MantraEntity("digambara","दिगंबरा दिगंबरा श्रीपाद वल्लभ दिगंबरा॥",sortOrder=15),
  MantraEntity("saraswatyai","ॐ ऐं सरस्वत्यै नमः",sortOrder=16),
  MantraEntity("mahalakshmyai","ॐ महालक्ष्म्यै नमः",sortOrder=17),
  MantraEntity("mahalakshmi","महालक्ष्मी मंत्र",sortOrder=18),
  MantraEntity("surya","ॐ घृणि सूर्याय नमः",sortOrder=19),
  MantraEntity("mitraya","ॐ मित्राय नमः",sortOrder=20),
  MantraEntity("chamunda","ॐ ऐं ह्रीं क्लीं चामुण्डायै विच्चे",sortOrder=21),
  MantraEntity("rudra-gayatri","रुद्र गायत्री मंत्र",sortOrder=22),
  MantraEntity("vakratunda","वक्रतुंड महाकाय",sortOrder=23),
  MantraEntity("asato-ma","असतो मा सद्गमय",sortOrder=24),
  MantraEntity("pandurang-hari","पांडुरंग हरी",sortOrder=25),
  MantraEntity("pandurang","जय जय पांडुरंग हरी",sortOrder=26),
  MantraEntity("custom","स्वतःचा मंत्र",sortOrder=27)
 )
 suspend fun seed(dao:JapaDao){mantras.forEach{if(dao.mantra(it.id)==null)dao.putMantra(it)}}
}
fun dayBounds(date:LocalDate=LocalDate.now()):Pair<Long,Long>{val z=ZoneId.systemDefault();return date.atStartOfDay(z).toInstant().toEpochMilli() to date.plusDays(1).atStartOfDay(z).toInstant().toEpochMilli()}
