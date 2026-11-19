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
 val dailyGoal=selected?.dailyMalaTarget?:0
 val lifetimeGoal=selected?.lifetimeJapaTarget?:0L
 MaterialTheme(colorScheme=lightColorScheme(primary=Color(0xFF6353A4),secondary=Color(0xFFB98B43),background=Color(0xFFF7F5FA),surface=Color.White)){
 Scaffold(topBar={TopAppBar(title={Text("Japa Tap · भजन जप")},actions={IconButton(onClick={dailyDraft=selected?.dailyMalaTarget?:0;lifetimeDraft=(selected?.lifetimeJapaTarget?:0L).toString();targetDialog=true}){Icon(Icons.Default.Settings,"Targets")};IconButton(onClick={achievementsDialog=true}){Icon(Icons.Default.EmojiEvents,"Achievements")}})},containerColor=Color(0xFFF7F5FA)){padding->
 LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=20.dp)){
 item{Text("मंत्र निवडा / Choose mantra",style=MaterialTheme.typography.labelLarge)}
 items(mantras){m->FilterChip(selected=m.id==selectedId,onClick={selectedId=m.id},label={Text(m.title)},modifier=Modifier.fillMaxWidth())}
 item{Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp)){Column(Modifier.padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally){
 Text(selected?.title?: "जप",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
 Spacer(Modifier.height(12.dp))
 Box(Modifier.size(220.dp).clip(CircleShape).clickable{scope.launch{selected?.let{dao.log(JapaLog(mantraId=it.id,timestamp=System.currentTimeMillis()))}}},contentAlignment=Alignment.Center){
 Canvas(Modifier.fillMaxSize()){drawArc(Color(0xFFE6E1EF),-90f,360f,false,style=androidx.compose.ui.graphics.drawscope.Stroke(15.dp.toPx(),cap=StrokeCap.Round));drawArc(Color(0xFF7462B3),-90f,ringCount/108f*360f,false,style=androidx.compose.ui.graphics.drawscope.Stroke(15.dp.toPx(),cap=StrokeCap.Round))}
 Column(horizontalAlignment=Alignment.CenterHorizontally){Text(ringCount.toString(),style=MaterialTheme.typography.displayMedium,fontWeight=FontWeight.Bold);Text("/ 108");Text("CURRENT MALA",style=MaterialTheme.typography.labelMedium)}
 }
 Text("Tap anywhere to count · स्पर्श करून जप मोजा",textAlign=TextAlign.Center)
 Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){OutlinedButton(onClick={scope.launch{selected?.let{dao.latest(it.id)?.let{last->dao.deleteLog(last.id)}}}}){Icon(Icons.Default.Undo,null);Text(" Undo")};Button(onClick={scope.launch{selected?.let{dao.log(JapaLog(mantraId=it.id,timestamp=System.currentTimeMillis()))}}}){Text("+ 1 Japa")}}
 }}
 }
 item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){KpiTile("TOTAL JAPA",total.toString(),Modifier.weight(1f));KpiTile("MALAS",malas.toString(),Modifier.weight(1f));KpiTile("TODAY",today.toString(),Modifier.weight(1f))}}
 item{ProgressPanel("Daily Mala target",dailyMalas,dailyGoal.toLong(),"Malas completed today")}
 item{ProgressPanel("Lifetime Japa target",total,lifetimeGoal,"Japa counted lifetime")}
 item{Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text("SPIRITUAL PROGRESS",fontWeight=FontWeight.Bold);Text(rankFor(malas),style=MaterialTheme.typography.titleLarge);Text(unlocked.toString()+" of "+badgeMilestones.size+" badges unlocked");Text(next?.let{"Next milestone: "+it+" Malas ("+(it*108)+" Japa)"}?: "All milestones reached");if(next!=null)LinearProgressIndicator(progress={malas.toFloat()/next.toFloat()},modifier=Modifier.fillMaxWidth())}}}
 item{Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text("CONSISTENCY · सातत्य",fontWeight=FontWeight.Bold);Text("Daily practice streaks and bonus badges will be added in this phase's next increment. Lifetime milestones never expire.")}}}
 }
 }}
 if(targetDialog&&selected!=null){AlertDialog(onDismissRequest={targetDialog=false},title={Text("Targets · "+selected.title)},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Daily Mala target (0 = disabled)");Row(verticalAlignment=Alignment.CenterVertically){OutlinedButton(onClick={dailyDraft=(dailyDraft-1).coerceAtLeast(0)}){Text("−")};Text(dailyDraft.toString()+" Malas",Modifier.weight(1f),textAlign=TextAlign.Center);OutlinedButton(onClick={dailyDraft++}){Text("+")}};Text("Lifetime Japa target");OutlinedTextField(lifetimeDraft,{lifetimeDraft=it.filter(Char::isDigit)},label={Text("Repetitions")},singleLine=true)}},confirmButton={TextButton(onClick={scope.launch{dao.putMantra(selected.copy(dailyMalaTarget=dailyDraft,lifetimeJapaTarget=lifetimeDraft.toLongOrNull()?.coerceAtLeast(0L)?:0L));targetDialog=false}}){Text("Save")}},dismissButton={TextButton(onClick={targetDialog=false}){Text("Cancel")}})}
 if(achievementsDialog){AlertDialog(onDismissRequest={achievementsDialog=false},title={Text("Badges · "+(selected?.title?: "Japa"))},text={LazyColumn{items(badgeMilestones){m->ListItem(headlineContent={Text(rankFor(m))},supportingContent={Text(m.toString()+" Malas · "+(m*108)+" Japa")},trailingContent={Text(if(malas>=m)"UNLOCKED" else "LOCKED")})}}},confirmButton={TextButton(onClick={achievementsDialog=false}){Text("Close")}})}
 }
}
@Composable private fun KpiTile(title:String,value:String,modifier:Modifier=Modifier){Card(modifier){Column(Modifier.padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(title,style=MaterialTheme.typography.labelSmall);Text(value,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)}}}
@Composable private fun ProgressPanel(title:String,value:Long,target:Long,unit:String){Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){Text(title,fontWeight=FontWeight.SemiBold);Text(value.toString()+" / "+target+" · "+unit);if(target>0)LinearProgressIndicator(progress={ (value.toFloat()/target.toFloat()).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())else Text("Set your goal from settings to track progress.")}}}
private fun rankFor(malas:Long)=when{malas>=10008->"महासंकल्प";malas>=1008->"महासाधना";malas>=501->"साधना समर्पण";malas>=108->"साधना पथ";malas>=51->"जप संकल्प";malas>=21->"जप निष्ठा";malas>=11->"नियमित जप";malas>=1->"जप आरंभ";else->"जप साधना प्रारंभ"}
