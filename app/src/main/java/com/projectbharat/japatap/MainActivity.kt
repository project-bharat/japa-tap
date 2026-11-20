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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
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

private val ink=Color(0xFF17161D)
private val panel=Color(0xFF23212B)
private val muted=Color(0xFF7A839E)
private val teal=Color(0xFF94B5C2)
private val cream=Color(0xFFFEF5D8)
private val categories=listOf("All","Ram","Krishna","Shiva","Ganpati","Devi","Hanuman","Datta","Vishnu","Guru","Other")
private fun deityFor(m:MantraEntity):String=when(m.id){
 "ram","vakratunda"->"Ram"
 "hare-krishna","vasudevaya","ram-krishna-hari"->"Krishna"
 "om-namah-shivaya","maha-mrityunjaya","rudra-gayatri"->"Shiva"
 "ganapataye"->"Ganpati"
 "durgaye","saraswatyai","mahalakshmyai","mahalakshmi","chamunda","gayatri"->"Devi"
 "hanumate"->"Hanuman"
 "dattatreya","digambara"->"Datta"
 "narayanaya","lokah"->"Vishnu"
 "guru"->"Guru"
 else ->"Other"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun JapaTapApp(dao:JapaDao){
 val scope=rememberCoroutineScope()
 val mantras by dao.mantras().collectAsStateWithLifecycle(emptyList())
 var selectedId by remember{mutableStateOf("ram")}
 var targetDialog by remember{mutableStateOf(false)}
 var achievementsDialog by remember{mutableStateOf(false)}
 var customDialog by remember{mutableStateOf(false)}
 var dailyDraft by remember{mutableIntStateOf(0)}
 var lifetimeDraft by remember{mutableStateOf("")}
 var customDraft by remember{mutableStateOf("")}
 var tab by remember{mutableStateOf("Home")}
 var search by remember{mutableStateOf("")}
 var searchOpen by remember{mutableStateOf(false)}
 var category by remember{mutableStateOf("All")}
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
 val visibleMantras=mantras.filter{(category=="All"||deityFor(it)==category)&&(search.isBlank()||it.title.contains(search,true)||it.id.contains(search,true))}
 MaterialTheme(colorScheme=darkColorScheme(primary=teal,secondary=Color(0xFFEADCC1),background=ink,surface=panel,onPrimary=ink,onSecondary=Color(0xFF423C6D),onBackground=cream,onSurface=cream,surfaceVariant=Color(0xFF343247),onSurfaceVariant=Color(0xFFBFC4D2))){
 Scaffold(containerColor=ink,topBar={TopAppBar(title={Column{Text("Bhajame",fontWeight=FontWeight.Bold);Text("Daily Jap & Bhakti",style=MaterialTheme.typography.labelSmall,color=muted)}},actions={IconButton(onClick={searchOpen=!searchOpen;tab="Library"}){Icon(Icons.Default.Search,"Search mantras")};IconButton(onClick={dailyDraft=dailyGoal;lifetimeDraft=lifetimeGoal.toString();targetDialog=true}){Icon(Icons.Default.Settings,"Targets")};IconButton(onClick={achievementsDialog=true}){Icon(Icons.Default.EmojiEvents,"Achievements")}},colors=TopAppBarDefaults.topAppBarColors(containerColor=ink))},
 bottomBar={NavigationBar(containerColor=panel,contentColor=cream){listOf("Home","Japa","Library","History").forEach{label->NavigationBarItem(selected=tab==label,onClick={tab=label},icon={Text(when(label){"Home"->"⌂";"Japa" ->"108";"Library" ->"☷";else ->"◷"},fontWeight=FontWeight.Bold)},label={Text(label)},colors=NavigationBarItemDefaults.colors(selectedIconColor=ink,selectedTextColor=teal,indicatorColor=teal,unselectedIconColor=muted,unselectedTextColor=muted))}}}
 ){padding->
 when(tab){
 "Library" ->LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=16.dp)){
 item{Text("Mantra Library",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Choose a mantra by your devotional focus",color=muted)}
 if(searchOpen||search.isNotBlank())item{OutlinedTextField(search,{search=it},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("Search mantra")},leadingIcon={Icon(Icons.Default.Search,null)})}
 item{Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){categories.take(6).forEach{c->FilterChip(selected=category==c,onClick={category=c},label={Text(c)})}}}
 item{Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){categories.drop(6).forEach{c->FilterChip(selected=category==c,onClick={category=c},label={Text(c)})}}}
 items(visibleMantras){m->Card(Modifier.fillMaxWidth().clickable{selectedId=m.id;tab="Japa"},colors=CardDefaults.cardColors(containerColor=panel),shape=RoundedCornerShape(18.dp)){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(deityFor(m).uppercase(),style=MaterialTheme.typography.labelSmall,color=teal);Text(m.title,fontWeight=FontWeight.SemiBold);Text("Tap to start counting",style=MaterialTheme.typography.bodySmall,color=muted)};Text(if(m.id==selectedId)"✓" else "→",color=teal)}}}
 item{OutlinedButton(onClick={customDraft="";customDialog=true},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Add,null);Text("Add custom mantra")}}
 }
 "Japa" ->JapaScreen(padding,selected,ringCount,total,today,dailyMalas,dailyGoal,lifetimeGoal,malas,unlocked,next,streakState,dao,scope,{targetDialog=true},{achievementsDialog=true})
  "History" -> LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   item { Text("Japa History",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold); Text("Your recent practice",color=muted) }
   item { Card(colors=CardDefaults.cardColors(containerColor=panel)) { Column(Modifier.padding(16.dp)) { Text(selected?.title ?: "Select mantra"); Text("Today's count: $today"); Text("Total count: $total"); Text("Current streak: ${streakState.current} days") } } }
   item { Text("Recent mantra activity will be expanded in the My Japa Report phase.",color=muted) }
  }
  else -> LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(top=12.dp,bottom=24.dp)){
 item{Card(colors=CardDefaults.cardColors(containerColor=panel),shape=RoundedCornerShape(24.dp)){Column(Modifier.padding(18.dp)){Text("Namaste 🙏",style=MaterialTheme.typography.titleMedium,color=teal);Text("Continue your daily jap",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text(selected?.title?: "Choose a mantra",color=muted);Spacer(Modifier.height(12.dp));Button(onClick={tab="Japa"},modifier=Modifier.fillMaxWidth()){Text("Start Japa")}}}}
 item{Text("Your practice today",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)}
 item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){KpiTile("TODAY",today.toString(),Modifier.weight(1f));KpiTile("TOTAL JAPA",total.toString(),Modifier.weight(1f));KpiTile("MALAS",malas.toString(),Modifier.weight(1f))}}
 item{ProgressPanel("Daily Sankalp",dailyMalas,dailyGoal.toLong(),"Malas")}
 item{Card(colors=CardDefaults.cardColors(containerColor=panel),shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(16.dp)){Text("Consistency",fontWeight=FontWeight.Bold);Text("${streakState.current} day current streak · ${streakState.best} best");Text("${streakState.days} practice days recorded",color=muted)}}}
 item{Text("Explore by deity",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)}
 item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){categories.drop(1).take(4).forEach{c->Card(Modifier.weight(1f).clickable{category=c;search="";searchOpen=false;tab="Library"},colors=CardDefaults.cardColors(containerColor=panel),shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(12.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(deitySymbol(c),style=MaterialTheme.typography.titleLarge);Text(c)}}}}}
 item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){categories.drop(5).take(4).forEach{c->Card(Modifier.weight(1f).clickable{category=c;search="";searchOpen=false;tab="Library"},colors=CardDefaults.cardColors(containerColor=panel),shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(12.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(deitySymbol(c),style=MaterialTheme.typography.titleLarge);Text(c)}}}}}
 item{Text("Continue with your mantra",fontWeight=FontWeight.Bold)}
 items(mantras.take(5)){m->Card(Modifier.fillMaxWidth().clickable{selectedId=m.id;tab="Japa"},colors=CardDefaults.cardColors(containerColor=panel),shape=RoundedCornerShape(16.dp)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(deityFor(m).uppercase(),style=MaterialTheme.typography.labelSmall,color=teal);Text(m.title)};Text("→",color=teal)}}}
 }
 }
 }
 if(targetDialog&&selected!=null){AlertDialog(onDismissRequest={targetDialog=false},title={Text("Targets · "+selected.title)},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Daily Mala target (0 = disabled)");Row(verticalAlignment=Alignment.CenterVertically){OutlinedButton(onClick={dailyDraft=(dailyDraft-1).coerceAtLeast(0)}){Text("−")};Text(dailyDraft.toString()+" Malas",Modifier.weight(1f),textAlign=TextAlign.Center);OutlinedButton(onClick={dailyDraft++}){Text("+")}};Text("Lifetime Japa target");OutlinedTextField(lifetimeDraft,{lifetimeDraft=it.filter(Char::isDigit)},label={Text("Repetitions")},singleLine=true)}},confirmButton={TextButton(onClick={scope.launch{dao.putMantra(selected.copy(dailyMalaTarget=dailyDraft,lifetimeJapaTarget=lifetimeDraft.toLongOrNull()?.coerceAtLeast(0L)?:0L));targetDialog=false}}){Text("Save")}},dismissButton={TextButton(onClick={targetDialog=false}){Text("Cancel")}})}
 if(achievementsDialog){AlertDialog(onDismissRequest={achievementsDialog=false},title={Text("Badges · "+(selected?.title?: "Japa"))},text={LazyColumn{items(badgeMilestones){m->ListItem(headlineContent={Text(rankFor(m))},supportingContent={Text(m.toString()+" Malas · "+(m*108)+" Japa")},trailingContent={Text(if(malas>=m)"UNLOCKED" else "LOCKED")})}}},confirmButton={TextButton(onClick={achievementsDialog=false}){Text("Close")}})}
 if(customDialog){AlertDialog(onDismissRequest={customDialog=false},title={Text("Add your mantra")},text={OutlinedTextField(customDraft,{customDraft=it},label={Text("Mantra text")},modifier=Modifier.fillMaxWidth())},confirmButton={TextButton(onClick={scope.launch{val id="custom-"+System.currentTimeMillis();dao.putMantra(MantraEntity(id,customDraft.trim(),sortOrder=mantras.size+1));selectedId=id;customDialog=false;tab="Japa"}} ,enabled=customDraft.isNotBlank()){Text("Add")}},dismissButton={TextButton(onClick={customDialog=false}){Text("Cancel")}})}
 }
}
@Composable private fun JapaScreen(padding:PaddingValues,selected:MantraEntity?,ringCount:Int,total:Long,today:Long,dailyMalas:Long,dailyGoal:Int,lifetimeGoal:Long,malas:Long,unlocked:Int,next:Long?,streak:PracticeStreak,dao:JapaDao,scope:kotlinx.coroutines.CoroutineScope,onTargets:()->Unit,onBadges:()->Unit){
 LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=16.dp)){
 item{Text("Your Japa",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text(selected?.title?: "Choose a mantra",color=muted)}
 item{Card(colors=CardDefaults.cardColors(containerColor=panel),shape=RoundedCornerShape(26.dp)){Column(Modifier.fillMaxWidth().padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(selected?.title?: "जप",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center);Spacer(Modifier.height(12.dp));Box(Modifier.size(230.dp).clip(CircleShape).clickable{scope.launch{selected?.let{dao.log(JapaLog(mantraId=it.id,timestamp=System.currentTimeMillis()))}}},contentAlignment=Alignment.Center){Canvas(Modifier.fillMaxSize()){drawArc(Color(0xFF343247),-90f,360f,false,style=androidx.compose.ui.graphics.drawscope.Stroke(15.dp.toPx(),cap=StrokeCap.Round));drawArc(teal,-90f,ringCount/108f*360f,false,style=androidx.compose.ui.graphics.drawscope.Stroke(15.dp.toPx(),cap=StrokeCap.Round))};Column(horizontalAlignment=Alignment.CenterHorizontally){Text(ringCount.toString(),style=MaterialTheme.typography.displayMedium,fontWeight=FontWeight.Bold);Text("/ 108");Text("CURRENT MALA",style=MaterialTheme.typography.labelMedium,color=muted)}};Text("Tap to count",color=muted);Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){OutlinedButton(onClick={scope.launch{selected?.let{dao.latest(it.id)?.let{last->dao.deleteLog(last.id)}}}}){Icon(Icons.Default.Undo,null);Text("Undo")};Button(onClick={scope.launch{selected?.let{dao.log(JapaLog(mantraId=it.id,timestamp=System.currentTimeMillis()))}}}){Text("+1 Japa")}}}}}
 item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){KpiTile("TOTAL",total.toString(),Modifier.weight(1f));KpiTile("TODAY",today.toString(),Modifier.weight(1f));KpiTile("MALAS",malas.toString(),Modifier.weight(1f))}}
 item{ProgressPanel("Daily Mala target",dailyMalas,dailyGoal.toLong(),"Malas")}
 item{ProgressPanel("Lifetime target",total,lifetimeGoal,"Japa")}
 item{Card(colors=CardDefaults.cardColors(containerColor=panel)){Column(Modifier.padding(16.dp)){Text("Spiritual progress",fontWeight=FontWeight.Bold);Text(rankFor(malas),style=MaterialTheme.typography.titleLarge);Text("$unlocked of 8 milestones unlocked");Text(next?.let{"Next: $it Malas / ${it*108} Japa"}?:"All milestones reached");if(next!=null)LinearProgressIndicator(progress={malas.toFloat()/next.toFloat()},modifier=Modifier.fillMaxWidth());TextButton(onClick=onBadges){Text("View achievements")};TextButton(onClick=onTargets){Text("Edit goals")}}}}
 item{Card(colors=CardDefaults.cardColors(containerColor=panel)){Column(Modifier.padding(16.dp)){Text("Consistency",fontWeight=FontWeight.Bold);Text("${streak.current} day streak · best ${streak.best}");Text("${streak.days} practice days")}}}
 }
}
@Composable private fun KpiTile(title:String,value:String,modifier:Modifier=Modifier){Card(modifier,colors=CardDefaults.cardColors(containerColor=panel),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(12.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(title,style=MaterialTheme.typography.labelSmall,color=muted);Text(value,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,color=cream)}}}
@Composable private fun ProgressPanel(title:String,value:Long,target:Long,unit:String){Card(colors=CardDefaults.cardColors(containerColor=panel),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){Text(title,fontWeight=FontWeight.SemiBold);Text("$value / $target · $unit",color=muted);if(target>0)LinearProgressIndicator(progress={(value.toFloat()/target.toFloat()).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())else Text("Set your goal to track progress.",color=muted)}}}
private fun deitySymbol(c:String)=when(c){"Ram"->"राम";"Krishna"->"कृष्ण";"Shiva"->"ॐ";"Ganpati"->"गणेश";"Devi"->"शक्ति";"Hanuman"->"हनुमान";"Datta"->"दत्त";"Vishnu"->"नारायण";"Guru"->"गुरु";else ->"जप"}
private data class PracticeStreak(val current:Int,val best:Int,val days:Int)
private fun calculatePracticeStreak(timestamps:List<Long>):PracticeStreak{val zone=ZoneId.systemDefault();val dates=timestamps.map{Instant.ofEpochMilli(it).atZone(zone).toLocalDate()}.distinct().sortedDescending();if(dates.isEmpty())return PracticeStreak(0,0,0);val today=LocalDate.now(zone);var current=0;var cursor=if(dates.first()==today)today else today.minusDays(1);while(dates.contains(cursor)){current++;cursor=cursor.minusDays(1)};var best=0;var run=0;var previous:LocalDate?=null;for(date in dates.sorted()){run=if(previous?.plusDays(1)==date)run+1 else 1;best=maxOf(best,run);previous=date};return PracticeStreak(current,best,dates.size)}
private fun rankFor(malas:Long)=when{malas>=10008->"महासंकल्प";malas>=1008->"महासाधना";malas>=501->"साधना समर्पण";malas>=108->"साधना पथ";malas>=51->"जप संकल्प";malas>=21->"जप निष्ठा";malas>=11->"नियमित जप";malas>=1->"जप आरंभ";else ->"जप साधना प्रारंभ"}
