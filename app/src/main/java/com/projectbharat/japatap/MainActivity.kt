package com.projectbharat.japatap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.projectbharat.japatap.data.*

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);val dao=JapaDatabase.get(this).dao();lifecycleScope.launch{JapaDefaults.seed(dao)};setContent{JapaTapApp(dao)}}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun JapaTapApp(dao:JapaDao){
 val scope=rememberCoroutineScope()
 val mantras by dao.mantras().collectAsStateWithLifecycle(emptyList())
 var selectedId by remember{mutableStateOf("ram")}
 var targetDialog by remember{mutableStateOf(false)}
 var achievementsDialog by remember{mutableStateOf(false)}
 var dailyDraft by remember{mutableIntStateOf(0)}
 var lifetimeDraft by remember{mutableStateOf("")}
 val selected=mantras.firstOrNull{it.id==selectedId}?:mantras.firstOrNull()
 val total by (selected?.let{dao.total(it.id)}?:flowOf(0L)).collectAsStateWithLifecycle(0L)
 val bounds=dayBounds()
 val today by (selected?.let{dao.period(it.id,bounds.first,bounds.second)}?:flowOf(0L)).collectAsStateWithLifecycle(0L)
 val malas=total/108L
 val dailyMalas=today/108L
 val ringCount=(total%108L).toInt()
 val badgeMilestones=listOf(1L,11L,21L,51L,108L,501L,1008L,10008L)
 val unlocked=badgeMilestones.count{malas>=it}
 val next=badgeMilestones.firstOrNull{it>malas}
 var streakState by remember(selectedId){mutableStateOf(PracticeStreak(0,0,0))}
 LaunchedEffect(selectedId){selected?.let{streakState=calculatePracticeStreak(dao.practiceTimestamps(it.id))}}
 val dailyGoal=selected?.dailyMalaTarget?:0
 val lifetimeGoal=selected?.lifetimeJapaTarget?:0L
 MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF94B5C2),secondary=Color(0xFFEADCC1),background=Color(0xFF17161D),surface=Color(0xFF23212B),onPrimary=Color(0xFF17161D),onSecondary=Color(0xFF423C6D),onBackground=Color(0xFFFEF5D8),onSurface=Color(0xFFFEF5D8),surfaceVariant=Color(0xFF343247),onSurfaceVariant=Color(0xFFBFC4D2))){
 Scaffold(topBar={TopAppBar(title={Text("Bhajame · Daily Jap & Bhakti",fontWeight=FontWeight.SemiBold)},actions={IconButton(onClick={dailyDraft=selected?.dailyMalaTarget?:0;lifetimeDraft=(selected?.lifetimeJapaTarget?:0L).toString();targetDialog=true}){Icon(Icons.Default.Settings,"Targets")};IconButton(onClick={achievementsDialog=true}){Icon(Icons.Default.EmojiEvents,"Achievements")}})},containerColor=Color(0xFF17161D)){padding->
 LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=20.dp)){
 item{Text("Choose your mantra",style=MaterialTheme.typography.labelLarge,color=Color(0xFF94B5C2))}
 items(mantras){m->FilterChip(selected=m.id==selectedId,onClick={selectedId=m.id},label={Text(m.title)},modifier=Modifier.fillMaxWidth())}
 item{Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Color(0xFF23212B)),shape=RoundedCornerShape(24.dp)){Column(Modifier.padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally){
 Text(selected?.title?: "जप",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
 Spacer(Modifier.height(12.dp))
 Box(Modifier.size(220.dp).clip(CircleShape).clickable{scope.launch{selected?.let{dao.log(JapaLog(mantraId=it.id,timestamp=System.currentTimeMillis()))}}},contentAlignment=Alignment.Center){
 Canvas(Modifier.fillMaxSize()){drawArc(Color(0xFF343247),-90f,360f,false,style=androidx.compose.ui.graphics.drawscope.Stroke(15.dp.toPx(),cap=StrokeCap.Round));drawArc(Color(0xFF94B5C2),-90f,ringCount/108f*360f,false,style=androidx.compose.ui.graphics.drawscope.Stroke(15.dp.toPx(),cap=StrokeCap.Round))}
 Column(horizontalAlignment=Alignment.CenterHorizontally){Text(ringCount.toString(),style=MaterialTheme.typography.displayMedium,fontWeight=FontWeight.Bold);Text("/ 108");Text("CURRENT MALA",style=MaterialTheme.typography.labelMedium,color=Color(0xFF7A839E))}
 }
 Text("Tap anywhere to count",textAlign=TextAlign.Center,color=Color(0xFF7A839E))
 Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){OutlinedButton(onClick={scope.launch{selected?.let{dao.latest(it.id)?.let{last->dao.deleteLog(last.id)}}}}){Icon(Icons.Default.Undo,null);Text(" Undo")};Button(onClick={scope.launch{selected?.let{dao.log(JapaLog(mantraId=it.id,timestamp=System.currentTimeMillis()))}}}){Text("+ 1 Japa")}}
 }}
 }
 item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){KpiTile("TOTAL JAPA",total.toString(),Modifier.weight(1f));KpiTile("MALAS",malas.toString(),Modifier.weight(1f));KpiTile("TODAY",today.toString(),Modifier.weight(1f))}}
 item{ProgressPanel("Daily Mala target",dailyMalas,dailyGoal.toLong(),"Malas completed today")}
 item{ProgressPanel("Lifetime Japa target",total,lifetimeGoal,"Japa counted lifetime")}
 item{Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text("SPIRITUAL PROGRESS",fontWeight=FontWeight.Bold);Text(rankFor(malas),style=MaterialTheme.typography.titleLarge);Text(unlocked.toString()+" of "+badgeMilestones.size+" badges unlocked");Text(next?.let{"Next milestone: "+it+" Malas ("+(it*108)+" Japa)"}?: "All milestones reached");if(next!=null)LinearProgressIndicator(progress={malas.toFloat()/next.toFloat()},modifier=Modifier.fillMaxWidth())}}}
 item{Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text("CONSISTENCY · सातत्य",fontWeight=FontWeight.Bold);Text("Current streak: "+streakState.current+" days · Best: "+streakState.best+" days",style=MaterialTheme.typography.titleMedium);Text("Practice days recorded: "+streakState.days);Text("Consistency milestones: "+listOf(3,7,21,40,108).filter{streakState.best>=it}.joinToString(separator=", "))}}}
 }
 }}
 if(targetDialog&&selected!=null){AlertDialog(onDismissRequest={targetDialog=false},title={Text("Targets · "+selected.title)},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Daily Mala target (0 = disabled)");Row(verticalAlignment=Alignment.CenterVertically){OutlinedButton(onClick={dailyDraft=(dailyDraft-1).coerceAtLeast(0)}){Text("−")};Text(dailyDraft.toString()+" Malas",Modifier.weight(1f),textAlign=TextAlign.Center);OutlinedButton(onClick={dailyDraft++}){Text("+")}};Text("Lifetime Japa target");OutlinedTextField(lifetimeDraft,{lifetimeDraft=it.filter(Char::isDigit)},label={Text("Repetitions")},singleLine=true)}},confirmButton={TextButton(onClick={scope.launch{dao.putMantra(selected.copy(dailyMalaTarget=dailyDraft,lifetimeJapaTarget=lifetimeDraft.toLongOrNull()?.coerceAtLeast(0L)?:0L));targetDialog=false}}){Text("Save")}},dismissButton={TextButton(onClick={targetDialog=false}){Text("Cancel")}})}
 if(achievementsDialog){AlertDialog(onDismissRequest={achievementsDialog=false},title={Text("Badges · "+(selected?.title?: "Japa"))},text={LazyColumn{items(badgeMilestones){m->ListItem(headlineContent={Text(rankFor(m))},supportingContent={Text(m.toString()+" Malas · "+(m*108)+" Japa")},trailingContent={Text(if(malas>=m)"UNLOCKED" else "LOCKED")})}}},confirmButton={TextButton(onClick={achievementsDialog=false}){Text("Close")}})}
 }
@Composable private fun KpiTile(title:String,value:String,modifier:Modifier=Modifier){Card(modifier){Column(Modifier.padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(title,style=MaterialTheme.typography.labelSmall);Text(value,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)}}}
@Composable private fun ProgressPanel(title:String,value:Long,target:Long,unit:String){Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){Text(title,fontWeight=FontWeight.SemiBold);Text(value.toString()+" / "+target+" · "+unit);if(target>0)LinearProgressIndicator(progress={ (value.toFloat()/target.toFloat()).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())else Text("Set your goal from settings to track progress.")}}}
private data class PracticeStreak(val current:Int,val best:Int,val days:Int)
private fun calculatePracticeStreak(timestamps:List<Long>):PracticeStreak{val zone=ZoneId.systemDefault();val dates=timestamps.map{Instant.ofEpochMilli(it).atZone(zone).toLocalDate()}.distinct().sortedDescending();if(dates.isEmpty())return PracticeStreak(0,0,0);val today=LocalDate.now(zone);var current=0;var cursor=if(dates.first()==today)today else today.minusDays(1);while(dates.contains(cursor)){current++;cursor=cursor.minusDays(1)};var best=0;var run=0;var previous:LocalDate?=null;for(date in dates.sorted()){run=if(previous?.plusDays(1)==date)run+1 else 1;best=maxOf(best,run);previous=date};return PracticeStreak(current,best,dates.size)}
private fun rankFor(malas:Long)=when{malas>=10008->"महासंकल्प";malas>=1008->"महासाधना";malas>=501->"साधना समर्पण";malas>=108->"साधना पथ";malas>=51->"जप संकल्प";malas>=21->"जप निष्ठा";malas>=11->"नियमित जप";malas>=1->"जप आरंभ";else->"जप साधना प्रारंभ"}
