package com.leanora.app
import android.content.Context
import android.graphics.Bitmap
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
import android.widget.Toast
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.net.URL
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.Locale

private val Cream=Color(0xFFFFF8F1); private val Green=Color(0xFFFF7A32); private val Ink=Color(0xFF24201D); private val Muted=Color(0xFF82766E); private val Mint=Color(0xFFFFD7B8); private val Lavender=Color(0xFFFFE9D8)
data class Food(val name:String,val kcal:Int,val protein:Int,val carbs:Int,val fat:Int,val meal:String)
private val catalog=listOf(Food("Idli (2)",130,4,26,1,"Breakfast"),Food("Plain dosa",170,4,30,4,"Breakfast"),Food("Chapati",120,3,22,3,"Lunch"),Food("Cooked rice (150 g)",195,4,42,0,"Lunch"),Food("Chicken breast (100 g)",165,31,0,4,"Lunch"),Food("Boiled egg",78,6,1,5,"Snack"),Food("Banana",105,1,27,0,"Snack"),Food("Paneer (100 g)",265,18,4,20,"Dinner"),Food("Curd (100 g)",61,3,5,3,"Dinner"))
class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{LeanoraApp(applicationContext)}}}
@Composable fun LeanoraApp(ctx:Context){
 val prefs=remember{ctx.getSharedPreferences("leanora",Context.MODE_PRIVATE)}; val scope=rememberCoroutineScope()
 var diaryDate by remember{mutableStateOf(LocalDate.now())}; var foods by remember{mutableStateOf(loadFoods(prefs,diaryDate))}; var goal by remember{mutableIntStateOf(prefs.getInt("goal",1850))}; var weight by remember{mutableFloatStateOf(prefs.getFloat("weight",78f))}; var target by remember{mutableFloatStateOf(prefs.getFloat("target",72f))}
 var onboarded by remember{mutableStateOf(prefs.getBoolean("onboarded",false))}; var welcome by remember{mutableStateOf(!prefs.getBoolean("welcome_seen",false))}; var splash by remember{mutableStateOf(true)}
 LaunchedEffect(Unit){delay(1800);splash=false}
 var profileName by remember{mutableStateOf(prefs.getString("profile_name","") ?: "")}
 var profileAge by remember{mutableIntStateOf(prefs.getInt("profile_age",26))}
 var profileSex by remember{mutableStateOf(prefs.getString("profile_sex","Male") ?: "Male")}
 var profileHeight by remember{mutableIntStateOf(prefs.getInt("profile_height",173))}
 var activityLevel by remember{mutableStateOf(prefs.getString("activity","Lightly active") ?: "Lightly active")}
 var fitnessGoal by remember{mutableStateOf(prefs.getString("fitness_goal","Lose weight") ?: "Lose weight")}
 var waterMl by remember{mutableIntStateOf(prefs.getInt("water_"+LocalDate.now().toString(),0))}; LaunchedEffect(diaryDate){waterMl=prefs.getInt("water_"+LocalDate.now().toString(),0)}; var waterGoal by remember{mutableIntStateOf(prefs.getInt("water_goal",2000))}; var tab by remember{mutableIntStateOf(0)}; var dark by remember{mutableStateOf(prefs.getBoolean("dark",false))}; var backendUrl by remember{mutableStateOf(prefs.getString("backend","https://leanora-android.onrender.com") ?: "https://leanora-android.onrender.com")}; var voiceResult by remember{mutableStateOf("")}; val cameraLauncher=rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()){ bitmap -> if(bitmap!=null){scope.launch{val food=withContext(Dispatchers.IO){estimateFoodPhoto(backendUrl,bitmap)};foods=foods+food;saveFoods(prefs,foods,diaryDate);Toast.makeText(ctx,"Food estimate added: "+food.name,Toast.LENGTH_LONG).show()}} }; val voiceLauncher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){ result -> if(result.resultCode==android.app.Activity.RESULT_OK){ voiceResult=result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty(); if(voiceResult.isNotBlank()){ val matched=catalog.firstOrNull{voiceResult.contains(it.name.substringBefore("("),ignoreCase=true)||it.name.substringBefore("(").contains(voiceResult,ignoreCase=true)}; val food=matched ?: Food(voiceResult.take(60),200,8,25,7,"Snack"); foods=foods+food; saveFoods(prefs,foods,diaryDate) } } }; var picker by remember{mutableStateOf(false)}; var barcodeDialog by remember{mutableStateOf(false)}; var barcodeText by remember{mutableStateOf("")}; var barcodeLoading by remember{mutableStateOf(false)}; var foodSearch by remember{mutableStateOf("")}; var editingFood by remember{mutableStateOf<Food?>(null)}; var customDialog by remember{mutableStateOf(false)}; var customName by remember{mutableStateOf("")}; var customKcal by remember{mutableStateOf("")}; var customProtein by remember{mutableStateOf("0")}; var customCarbs by remember{mutableStateOf("0")}; var customFat by remember{mutableStateOf("0")}; var customMeal by remember{mutableStateOf("Snack")}; var goalDialog by remember{mutableStateOf(false)}; var weightDialog by remember{mutableStateOf(false)}; var targetDialog by remember{mutableStateOf(false)}; var targetText by remember{mutableStateOf(target.toString())}; var goalText by remember{mutableStateOf(goal.toString())}; var weightText by remember{mutableStateOf(weight.toString())}
 val kcal=foods.sumOf{it.kcal}; val p=foods.sumOf{it.protein}; val c=foods.sumOf{it.carbs}; val f=foods.sumOf{it.fat}
 MaterialTheme(colorScheme=if(dark)darkColorScheme(primary=Color(0xFF69C6A4),background=Color(0xFF101815),surface=Color(0xFFF0F3F0),onSurface=Ink)else lightColorScheme(primary=Green,background=Cream,surface=Color.White)){
 if(splash){ LeanoraSplash() } else if(welcome){ LeanoraWelcome{welcome=false;prefs.edit().putBoolean("welcome_seen",true).apply()} } else if(!onboarded){ ProfileSetup(profileName,profileAge,profileSex,profileHeight,weight,target,activityLevel,fitnessGoal,{name,age,sex,height,w,t,activity,goalName-> profileName=name;profileAge=age;profileSex=sex;profileHeight=height;weight=w;target=t;activityLevel=activity;fitnessGoal=goalName;goal=calorieTarget(age,sex,height,w,activity,goalName);prefs.edit().putBoolean("onboarded",true).putString("profile_name",name).putInt("profile_age",age).putString("profile_sex",sex).putInt("profile_height",height).putFloat("weight",w).putFloat("target",t).putString("activity",activity).putString("fitness_goal",goalName).putInt("goal",goal).apply();onboarded=true }) } else { Scaffold(containerColor=if(dark)Color(0xFF101815)else Cream,bottomBar={NavigationBar(containerColor=if(dark)Color(0xFF1A2420)else Color.White){NavigationBarItem(tab==0,{tab=0},icon={Icon(Icons.Default.Home,null)},label={Text("Today")});NavigationBarItem(tab==1,{tab=1},icon={Icon(Icons.Default.Restaurant,null)},label={Text("Food")});NavigationBarItem(tab==2,{tab=2},icon={Icon(Icons.Default.ShowChart,null)},label={Text("Progress")});NavigationBarItem(tab==3,{tab=3},icon={Icon(Icons.Default.SmartToy,null)},label={Text("Coach")});NavigationBarItem(tab==4,{tab=4},icon={Icon(Icons.Default.Settings,null)},label={Text("Settings")})}},floatingActionButton={if(tab<2)FloatingActionButton(onClick={picker=true},containerColor=Green){Icon(Icons.Default.Add,"Add food",tint=Color.White)}}){pad->
 when(tab){0->Dashboard(profileName,foods,kcal,goal,p,c,f,waterMl,waterGoal,Modifier.padding(pad),{picker=true},{amount->waterMl=amount;prefs.edit().putInt("water_"+LocalDate.now().toString(),amount).apply()});1->Diary(foods,diaryDate,Modifier.padding(pad),{date->diaryDate=date;foods=loadFoods(prefs,date)},{picker=true},{i->val updated=foods.toMutableList().also{it.removeAt(i)};foods=updated;saveFoods(prefs,updated,diaryDate)},{i->editingFood=foods[i]});2->Progress(weight,target,prefs.getString("weight_history","[]") ?: "[]",Modifier.padding(pad)){weightText=weight.toString();weightDialog=true};3->Coach(kcal,goal,p,Modifier.padding(pad),backendUrl);else->Settings(goal,weight,target,waterGoal,Modifier.padding(pad),{goalText=goal.toString();goalDialog=true},{weightText=weight.toString();weightDialog=true},{waterGoal=it;prefs.edit().putInt("water_goal",it).apply()},dark,{dark=it;prefs.edit().putBoolean("dark",it).apply()},backendUrl,{backendUrl=it;prefs.edit().putString("backend",it).apply()},{targetText=target.toString();targetDialog=true},{onboarded=false;prefs.edit().putBoolean("onboarded",false).apply()})}
 }
 if (picker) {
  AlertDialog(
   onDismissRequest = { picker = false },
   title = { Text("Add food") },
   text = {
    Column {
     Button(onClick={customDialog=true},modifier=Modifier.fillMaxWidth()){Text("Custom food / portion")}
     OutlinedButton(onClick={barcodeDialog=true},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.QrCodeScanner,null);Spacer(Modifier.width(8.dp));Text("Look up barcode")}

     Button(onClick = { cameraLauncher.launch(null) }, modifier = Modifier.fillMaxWidth()) { Text("Camera • estimate food") }
     Button(onClick = {
      val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
       .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
       .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
       .putExtra(RecognizerIntent.EXTRA_PROMPT, "Say food name and quantity")
      voiceLauncher.launch(intent)
     }, modifier = Modifier.fillMaxWidth()) { Text("Voice food logging • English") }
     Button(onClick = {
      val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
       .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
       .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ta-IN")
       .putExtra(RecognizerIntent.EXTRA_PROMPT, "உணவின் பெயர் மற்றும் அளவை சொல்லுங்கள்")
      voiceLauncher.launch(intent)
     }, modifier = Modifier.fillMaxWidth()) { Text("தமிழில் உணவு பதிவு") }
     OutlinedTextField(value=foodSearch,onValueChange={foodSearch=it},modifier=Modifier.fillMaxWidth(),singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)},placeholder={Text("Search food catalog")})
     LazyColumn(Modifier.heightIn(max = 350.dp)) {
      items(catalog.filter{it.name.contains(foodSearch,ignoreCase=true)}) { food ->
       TextButton(
        onClick = { foods = foods + food; saveFoods(prefs,foods,diaryDate); picker = false; foodSearch="" },
        modifier = Modifier.fillMaxWidth()
       ) {
        Column(Modifier.fillMaxWidth()) {
         Text(food.name, color = Ink, fontWeight = FontWeight.SemiBold)
         Text(food.kcal.toString() + " kcal • P" + food.protein + " C" + food.carbs + " F" + food.fat + "g", color = Muted)
        }
       }
      }
     }
    }
   },
   confirmButton = { TextButton(onClick = { picker = false }) { Text("Close") } }
  )
 }

 if(barcodeDialog)AlertDialog(onDismissRequest={if(!barcodeLoading)barcodeDialog=false},title={Text("Barcode nutrition lookup")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Enter the barcode number printed below the product barcode. Nutrition data is retrieved from Open Food Facts.");OutlinedTextField(value=barcodeText,onValueChange={barcodeText=it.filter(Char::isDigit).take(20)},label={Text("Barcode number")},singleLine=true,modifier=Modifier.fillMaxWidth());if(barcodeLoading)LinearProgressIndicator(Modifier.fillMaxWidth())}},confirmButton={TextButton(enabled=barcodeText.length>=8&&!barcodeLoading,onClick={barcodeLoading=true;scope.launch{val result=withContext(Dispatchers.IO){lookupFoodBarcode(barcodeText)};if(result.kcal>0){foods=foods+result;saveFoods(prefs,foods,diaryDate);picker=false;Toast.makeText(ctx,"Added "+result.name,Toast.LENGTH_LONG).show();barcodeDialog=false;barcodeText=""}else Toast.makeText(ctx,"No nutrition data found. Try entering food manually.",Toast.LENGTH_LONG).show();barcodeLoading=false}}){Text(if(barcodeLoading)"Looking up…" else "Look up & add")}},dismissButton={TextButton(enabled=!barcodeLoading,onClick={barcodeDialog=false}){Text("Cancel")}})
 if(customDialog)AlertDialog(onDismissRequest={customDialog=false},title={Text("Custom food entry")},text={Column(verticalArrangement=Arrangement.spacedBy(6.dp)){OutlinedTextField(customName,{customName=it},label={Text("Food name")});OutlinedTextField(customKcal,{customKcal=it.filter(Char::isDigit)},label={Text("Calories (kcal)")});OutlinedTextField(customProtein,{customProtein=it.filter(Char::isDigit)},label={Text("Protein (g)")});OutlinedTextField(customCarbs,{customCarbs=it.filter(Char::isDigit)},label={Text("Carbs (g)")});OutlinedTextField(customFat,{customFat=it.filter(Char::isDigit)},label={Text("Fat (g)")});Text("Meal category",fontWeight=FontWeight.SemiBold);Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("Breakfast","Lunch","Dinner","Snack").forEach{FilterChip(selected=customMeal==it,onClick={customMeal=it},label={Text(it)})}}}},confirmButton={TextButton(onClick={val kcalValue=customKcal.toIntOrNull();if(customName.isNotBlank()&&kcalValue!=null){foods=foods+Food(customName.trim(),kcalValue,customProtein.toIntOrNull()?:0,customCarbs.toIntOrNull()?:0,customFat.toIntOrNull()?:0,customMeal.ifBlank{"Snack"});saveFoods(prefs,foods,diaryDate);customDialog=false;picker=false;customName="";customKcal=""}}){Text("Add food")}},dismissButton={TextButton(onClick={customDialog=false}){Text("Cancel")}})
 if(editingFood!=null)EditFoodDialog(editingFood!!,{editingFood=null},{updated->val list=foods.map{if(it.name==editingFood!!.name&&it.meal==editingFood!!.meal)updated else it};foods=list;saveFoods(prefs,list,diaryDate);editingFood=null})
 if(goalDialog)AlertDialog(onDismissRequest={goalDialog=false},title={Text("Daily calorie goal")},text={OutlinedTextField(goalText,{goalText=it.filter(Char::isDigit)},label={Text("kcal")})},confirmButton={TextButton(onClick={goalText.toIntOrNull()?.let{if(it in 800..6000){goal=it;prefs.edit().putInt("goal",it).apply()}};goalDialog=false}){Text("Save")}})
 if(targetDialog)AlertDialog(onDismissRequest={targetDialog=false},title={Text("Target weight")},text={OutlinedTextField(targetText,{targetText=it},label={Text("kg")})},confirmButton={TextButton(onClick={targetText.toFloatOrNull()?.let{if(it in 30f..300f){target=it;prefs.edit().putFloat("target",it).apply()}};targetDialog=false}){Text("Save")}})
 if(weightDialog)AlertDialog(onDismissRequest={weightDialog=false},title={Text("Current weight")},text={OutlinedTextField(weightText,{weightText=it},label={Text("kg")})},confirmButton={TextButton(onClick={weightText.toFloatOrNull()?.let{if(it in 30f..300f){weight=it;val history=JSONArray(prefs.getString("weight_history","[]")?: "[]");history.put(JSONObject().put("date",LocalDate.now().toString()).put("kg",it));prefs.edit().putFloat("weight",it).putString("weight_history",history.toString()).apply()}};weightDialog=false}){Text("Save")}})
 }
 }
}

@Composable private fun LeanoraSplash(){
 val transition=rememberInfiniteTransition(label="splash-loader")
 val alpha by transition.animateFloat(initialValue=.35f,targetValue=1f,animationSpec=infiniteRepeatable(tween(700),RepeatMode.Reverse),label="loader-alpha")
 Box(Modifier.fillMaxSize().background(Color(0xFF28211D)),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(18.dp)){
 Box(Modifier.size(112.dp).background(Color(0xFFFF7A32),RoundedCornerShape(32.dp)),contentAlignment=Alignment.Center){Icon(Icons.Default.Favorite,null,tint=Color(0xFFFFD7B8),modifier=Modifier.size(56.dp))}
 Text("LEANORA",fontSize=32.sp,fontWeight=FontWeight.ExtraBold,letterSpacing=5.sp,color=Color.White)
 Text("Nourish your goals. Own your journey.",fontSize=14.sp,color=Color(0xFFFFDCC7))
 Spacer(Modifier.height(18.dp));CircularProgressIndicator(color=Color(0xFFFF7A32),modifier=Modifier.size(28.dp),strokeWidth=3.dp)
 Text("Getting things ready…",fontSize=12.sp,color=Color.White.copy(alpha=alpha))
 }}
}
@Composable private fun LeanoraWelcome(onStart:()->Unit){
 val transition=rememberInfiniteTransition(label="leanora-splash")
 val scale by transition.animateFloat(initialValue=.94f,targetValue=1.06f,animationSpec=infiniteRepeatable(tween(1200,easing=FastOutSlowInEasing),RepeatMode.Reverse),label="logo-scale")
 val rotation by transition.animateFloat(initialValue=-4f,targetValue=4f,animationSpec=infiniteRepeatable(tween(1800,easing=FastOutSlowInEasing),RepeatMode.Reverse),label="logo-rotation")
 Box(Modifier.fillMaxSize().background(Color(0xFF28211D)).padding(28.dp),contentAlignment=Alignment.Center){
  Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(18.dp)){
   Box(Modifier.size(128.dp).graphicsLayer{scaleX=scale;scaleY=scale;rotationZ=rotation}.background(Color(0xFFFF7A32),RoundedCornerShape(36.dp)),contentAlignment=Alignment.Center){Icon(Icons.Default.Favorite,null,tint=Mint,modifier=Modifier.size(62.dp))}
   Text("LEANORA",fontSize=34.sp,fontWeight=FontWeight.ExtraBold,letterSpacing=5.sp,color=Color.White)
   Text("Nourish your goals. Own your journey.",fontSize=15.sp,color=Color(0xFFFFDCC7))
   Spacer(Modifier.height(32.dp))
   Button(onClick=onStart,modifier=Modifier.fillMaxWidth().height(56.dp),shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.buttonColors(containerColor=Mint,contentColor=Color(0xFF28211D))){Text("Get started",fontSize=17.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.width(8.dp));Icon(Icons.Default.ArrowForward,null)}
   Text("Your wellness, thoughtfully tracked.",fontSize=12.sp,color=Color(0xFFD2B7A7))
  }
 }
}
@Composable private fun ProfileSetup(name0:String,age0:Int,sex0:String,height0:Int,weight0:Float,target0:Float,activity0:String,goal0:String,onSave:(String,Int,String,Int,Float,Float,String,String)->Unit){
 var name by remember{mutableStateOf(name0)};var age by remember{mutableStateOf(age0.toString())};var sex by remember{mutableStateOf(sex0)};var height by remember{mutableStateOf(height0.toString())};var weight by remember{mutableStateOf(weight0.toString())};var target by remember{mutableStateOf(target0.toString())};var activity by remember{mutableStateOf(activity0)};var goal by remember{mutableStateOf(goal0)}
 val goals=listOf("Lose weight","Lose body fat","Gain weight","Build muscle","Maintain weight");val activities=listOf("Sedentary","Lightly active","Moderately active","Very active")
 LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(top=30.dp,bottom=30.dp)){
 item{Text("LEANORA",color=Green,fontWeight=FontWeight.Bold,letterSpacing=3.sp);Text("Let’s personalize your plan",fontSize=28.sp,fontWeight=FontWeight.Bold,color=Ink);Text("Tell us about yourself to estimate daily calorie needs.",color=Muted)}
 item{OutlinedTextField(name,{name=it},label={Text("Your name")},modifier=Modifier.fillMaxWidth())}
 item{OutlinedTextField(age,{age=it.filter(Char::isDigit)},label={Text("Age")},modifier=Modifier.fillMaxWidth())}
 item{Text("Sex");Row{listOf("Male","Female").forEach{FilterChip(selected=sex==it,onClick={sex=it},label={Text(it)},modifier=Modifier.padding(end=8.dp))}}}
 item{OutlinedTextField(height,{height=it.filter(Char::isDigit)},label={Text("Height (cm)")},modifier=Modifier.fillMaxWidth())}
 item{OutlinedTextField(weight,{weight=it},label={Text("Current weight (kg)")},modifier=Modifier.fillMaxWidth())}
 item{OutlinedTextField(target,{target=it},label={Text("Target weight (kg)")},modifier=Modifier.fillMaxWidth())}
 item{Text("Fitness goal",fontWeight=FontWeight.SemiBold);goals.forEach{FilterChip(selected=goal==it,onClick={goal=it},label={Text(it)},modifier=Modifier.padding(end=4.dp))}}
 item{Text("Activity level",fontWeight=FontWeight.SemiBold);activities.forEach{FilterChip(selected=activity==it,onClick={activity=it},label={Text(it)},modifier=Modifier.padding(end=4.dp))}}
 item{Button(onClick={val a=age.toIntOrNull();val h=height.toIntOrNull();val w=weight.toFloatOrNull();val t=target.toFloatOrNull();if(name.isNotBlank()&&a!=null&&a in 14..100&&h!=null&&h in 100..250&&w!=null&&w in 30f..300f&&t!=null&&t in 30f..300f)onSave(name.trim(),a,sex,h,w,t,activity,goal)},modifier=Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(16.dp)){Text("Create my nutrition plan",fontWeight=FontWeight.Bold)}}
 item{Text("Calorie estimates are a guide, not medical advice.",fontSize=12.sp,color=Muted)}
 }
}
private fun calorieTarget(age:Int,sex:String,height:Int,weight:Float,activity:String,goal:String):Int{
 val bmr=if(sex=="Male")10*weight+6.25*height-5*age+5 else 10*weight+6.25*height-5*age-161
 val factor=when(activity){"Sedentary"->1.2;"Moderately active"->1.55;"Very active"->1.725;else->1.375}
 val maintenance=bmr*factor
 val desired=when(goal){"Lose weight","Lose body fat"->maintenance-400;"Gain weight","Build muscle"->maintenance+250;else->maintenance}
 return desired.toInt().coerceIn(1200,4500)
}

@Composable private fun Dashboard(name:String,food:List<Food>,k:Int,g:Int,p:Int,c:Int,f:Int,waterMl:Int,waterGoal:Int,m:Modifier,add:()->Unit,onWaterChange:(Int)->Unit){
 val remaining=g-k; val progress=(k.toFloat()/g.coerceAtLeast(1)).coerceIn(0f,1f)
 LazyColumn(m.fillMaxSize().padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(top=14.dp,bottom=24.dp)){
 item{Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("TODAY • "+LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d")),fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.2.sp,color=Muted);Text("Hi, "+name.ifBlank{"there"}+" 👋",fontSize=26.sp,fontWeight=FontWeight.Bold,color=Ink);Text("Small steps. Lasting change.",fontSize=13.sp,color=Muted)};Box(Modifier.size(42.dp).background(Lavender,RoundedCornerShape(15.dp)),contentAlignment=Alignment.Center){Icon(Icons.Default.Favorite,null,tint=Green)}}}
 item{Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=Ink)){Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("CALORIES LEFT",fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.1.sp,color=Color.White.copy(alpha=.7f));Row(verticalAlignment=Alignment.Bottom){Text(kotlin.math.abs(remaining).toString(),fontSize=36.sp,fontWeight=FontWeight.ExtraBold,color=Color.White);Text(if(remaining>=0)" kcal left" else " kcal over",fontSize=14.sp,color=Color.White.copy(alpha=.75f),modifier=Modifier.padding(bottom=6.dp))}};Box(Modifier.size(76.dp),contentAlignment=Alignment.Center){CircularProgressIndicator(progress={progress},modifier=Modifier.fillMaxSize(),color=Green,trackColor=Color.White.copy(alpha=.12f),strokeWidth=8.dp);Text("${(progress*100).toInt()}%",fontWeight=FontWeight.Bold,color=Color.White)}};Text("$k eaten  •  $g daily goal",fontSize=13.sp,color=Color.White.copy(alpha=.75f));LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth().height(6.dp),color=Green,trackColor=Color.White.copy(alpha=.12f))}}}
 item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Macro("PROTEIN","${p}g",Modifier.weight(1f));Macro("CARBS","${c}g",Modifier.weight(1f));Macro("FAT","${f}g",Modifier.weight(1f))}}
 item{Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.WaterDrop,null,tint=Color(0xFF2196F3),modifier=Modifier.size(28.dp));Column(Modifier.weight(1f).padding(start=10.dp)){Text("Water intake",fontSize=18.sp,fontWeight=FontWeight.Bold,color=Ink);Text("${waterMl} / ${waterGoal} ml today",fontSize=13.sp,color=Muted)};Text("${(waterMl*100/waterGoal.coerceAtLeast(1)).coerceAtMost(100)}%",fontWeight=FontWeight.Bold,color=Color(0xFF2196F3))};LinearProgressIndicator(progress={(waterMl/waterGoal.coerceAtLeast(1).toFloat()).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(7.dp),color=Color(0xFF2196F3),trackColor=Color(0xFFE8F2FA));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={onWaterChange((waterMl-250).coerceAtLeast(0))},modifier=Modifier.weight(1f),enabled=waterMl>0){Text("−250 ml")};Button(onClick={onWaterChange(waterMl+250)},modifier=Modifier.weight(1f)){Text("+250 ml")};Button(onClick={onWaterChange(waterMl+500)},modifier=Modifier.weight(1f)){Text("+500 ml")}};Text("Hydration target is a customizable reminder, not medical advice.",fontSize=11.sp,color=Muted)}}}
 item{Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Meals & snacks",fontSize=20.sp,fontWeight=FontWeight.Bold,color=Ink);Text("Your day, one meal at a time",fontSize=12.sp,color=Muted)};TextButton(onClick=add){Icon(Icons.Default.Add,null);Text("Add food")}}}
 if(food.isEmpty())item{Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.fillMaxWidth().padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){Icon(Icons.Default.Restaurant,null,tint=Green,modifier=Modifier.size(30.dp));Text("Start with your first meal",fontWeight=FontWeight.Bold,color=Ink);Text("Log food to see calories and macros here.",fontSize=13.sp,color=Muted);Button(onClick=add,shape=RoundedCornerShape(14.dp)){Text("Log food")}}}}
 listOf("Breakfast","Lunch","Dinner","Snack").forEach{meal->item{val mealFood=food.filter{it.meal.equals(meal,true)};Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=12.dp)){Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(34.dp).background(Lavender,RoundedCornerShape(11.dp)),contentAlignment=Alignment.Center){Icon(when(meal){"Breakfast"->Icons.Default.WbSunny;"Lunch"->Icons.Default.LunchDining;"Dinner"->Icons.Default.DinnerDining;else->Icons.Default.Cookie},null,tint=Green)};Column(Modifier.weight(1f).padding(start=10.dp)){Text(meal,fontWeight=FontWeight.Bold,color=Ink);Text(if(mealFood.isEmpty())"Tap + to add" else "${mealFood.size} item${if(mealFood.size==1)"" else "s"}",fontSize=11.sp,color=Muted)};Text("${mealFood.sumOf{it.kcal}} kcal",fontWeight=FontWeight.Bold,color=Ink);IconButton(onClick=add){Icon(Icons.Default.Add,null,tint=Green)}};mealFood.forEach{Row(Modifier.fillMaxWidth().padding(start=44.dp,top=4.dp,bottom=3.dp),verticalAlignment=Alignment.CenterVertically){Text(it.name,Modifier.weight(1f),fontSize=13.sp,color=Muted);Text("${it.kcal} kcal",fontSize=12.sp,color=Muted)}}}}}
 }
}
}
@Composable private fun Diary(food:List<Food>,date:LocalDate,m:Modifier,onDateChange:(LocalDate)->Unit,add:()->Unit,remove:(Int)->Unit,edit:(Int)->Unit){
 var query by remember{mutableStateOf("")}; var mealFilter by remember{mutableStateOf("All")}
 val filtered=food.withIndex().filter{(_,x)->(mealFilter=="All"||x.meal.equals(mealFilter,true))&&x.name.contains(query,true)}
 Column(m.fillMaxSize().padding(horizontal=18.dp,vertical=14.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Food diary",fontSize=30.sp,fontWeight=FontWeight.Bold,color=Ink);Text("Your daily nutrition, at a glance",color=Muted)};IconButton(onClick=add){Icon(Icons.Default.AddCircle,"Add food",tint=Green)}}
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){IconButton(onClick={onDateChange(date.minusDays(1))}){Icon(Icons.Default.ChevronLeft,"Previous day")};Column(horizontalAlignment=Alignment.CenterHorizontally){Text(if(date==LocalDate.now())"Today" else date.format(java.time.format.DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")),fontWeight=FontWeight.Bold,color=Ink);if(date!=LocalDate.now())TextButton(onClick={onDateChange(LocalDate.now())},contentPadding=PaddingValues(horizontal=8.dp,vertical=0.dp)){Text("Back to today",fontSize=12.sp)}};IconButton(enabled=date.isBefore(LocalDate.now()),onClick={onDateChange(date.plusDays(1))}){Icon(Icons.Default.ChevronRight,"Next day")}}
  Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Green)){Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(if(date==LocalDate.now())"Logged today" else "Logged on "+date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM")),color=Color.White.copy(alpha=.8f));Text("${food.size} food items",fontSize=22.sp,fontWeight=FontWeight.Bold,color=Color.White)};Text(food.sumOf{it.kcal}.toString()+" kcal",fontSize=24.sp,fontWeight=FontWeight.Bold,color=Color.White)}}
  OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth(),singleLine=true,leadingIcon={Icon(Icons.Default.Search,"Search")},placeholder={Text("Search logged foods")},shape=RoundedCornerShape(16.dp))
  Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("All","Breakfast","Lunch","Dinner","Snack").forEach{FilterChip(selected=mealFilter==it,onClick={mealFilter=it},label={Text(it)})}}
  if(filtered.isEmpty())Box(Modifier.fillMaxWidth().weight(1f),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.RestaurantMenu,null,tint=Muted);Text(if(food.isEmpty())"No foods logged yet" else "No matching foods",fontWeight=FontWeight.SemiBold,color=Ink);Text("Add a meal to start tracking.",color=Muted);Button(onClick=add){Text("Add food")}}}
  else LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=18.dp)){items(filtered.size){j->val (index,item)=filtered[j];Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(item.name,fontSize=17.sp,fontWeight=FontWeight.SemiBold,color=Ink);Text(item.meal,color=Muted,fontSize=13.sp)};Text(item.kcal.toString()+" kcal",fontSize=18.sp,fontWeight=FontWeight.Bold,color=Green)};Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){Macro("P",item.protein.toString()+"g",Modifier.weight(1f));Macro("C",item.carbs.toString()+"g",Modifier.weight(1f));Macro("F",item.fat.toString()+"g",Modifier.weight(1f))};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){TextButton(onClick={edit(index)}){Icon(Icons.Default.Edit,null);Text(" Edit")};TextButton(onClick={remove(index)}){Icon(Icons.Default.DeleteOutline,null);Text(" Delete")}}}}}}
 }
}
@Composable private fun EditFoodDialog(food:Food,onDismiss:()->Unit,onSave:(Food)->Unit){var name by remember{mutableStateOf(food.name)};var kcal by remember{mutableStateOf(food.kcal.toString())};var protein by remember{mutableStateOf(food.protein.toString())};var carbs by remember{mutableStateOf(food.carbs.toString())};var fat by remember{mutableStateOf(food.fat.toString())};var meal by remember{mutableStateOf(food.meal)};AlertDialog(onDismissRequest=onDismiss,title={Text("Edit food entry")},text={Column(verticalArrangement=Arrangement.spacedBy(6.dp)){OutlinedTextField(name,{name=it},label={Text("Food name")});OutlinedTextField(kcal,{kcal=it.filter(Char::isDigit)},label={Text("Calories")});OutlinedTextField(protein,{protein=it.filter(Char::isDigit)},label={Text("Protein (g)")});OutlinedTextField(carbs,{carbs=it.filter(Char::isDigit)},label={Text("Carbs (g)")});OutlinedTextField(fat,{fat=it.filter(Char::isDigit)},label={Text("Fat (g)")});Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("Breakfast","Lunch","Dinner","Snack").forEach{FilterChip(selected=meal==it,onClick={meal=it},label={Text(it)})}}}},confirmButton={TextButton(onClick={val n=kcal.toIntOrNull();if(n!=null)onSave(food.copy(name=name,kcal=n,protein=protein.toIntOrNull()?:0,carbs=carbs.toIntOrNull()?:0,fat=fat.toIntOrNull()?:0,meal=meal))}){Text("Save")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})}

@Composable private fun Progress(w:Float,t:Float,historyJson:String,m:Modifier,edit:()->Unit){
 val points=remember(historyJson){try{val arr=JSONArray(historyJson);(0 until arr.length()).map{val o=arr.getJSONObject(it);o.optDouble("kg",w.toDouble()).toFloat()}.takeLast(7)}catch(_:Exception){emptyList<Float>()}}
 val history=remember(historyJson){try{val arr=JSONArray(historyJson);(0 until arr.length()).map{val o=arr.getJSONObject(it);o.optString("date")+" • "+String.format(Locale.US,"%.1f",o.optDouble("kg"))+" kg"}.reversed().take(8)}catch(_:Exception){emptyList<String>()}}
 Column(m.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  Text("Progress",fontSize=28.sp,fontWeight=FontWeight.Bold,color=Ink)
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Card(Modifier.weight(1f),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.padding(14.dp)){Text("Current",fontSize=12.sp,color=Muted);Text(String.format(Locale.US,"%.1f kg",w),fontSize=22.sp,fontWeight=FontWeight.Bold,color=Ink)}};Card(Modifier.weight(1f),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.padding(14.dp)){Text("Target",fontSize=12.sp,color=Muted);Text(String.format(Locale.US,"%.1f kg",t),fontSize=22.sp,fontWeight=FontWeight.Bold,color=Green)}}}
  Info("Remaining to target",String.format(Locale.US,"%.1f kg",kotlin.math.abs(w-t)))
  Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Weight trend",fontWeight=FontWeight.Bold,color=Ink);Text(if(points.size<2)"Log at least two measurements to see your trend." else "Last ${points.size} measurements",fontSize=12.sp,color=Muted);if(points.isNotEmpty())WeightTrendChart(points,Modifier.fillMaxWidth().height(150.dp));if(points.isNotEmpty())Text("Min ${String.format(Locale.US,"%.1f",points.minOrNull())} kg  •  Max ${String.format(Locale.US,"%.1f",points.maxOrNull())} kg",fontSize=12.sp,color=Muted)}}
  Button(onClick=edit,modifier=Modifier.fillMaxWidth()){Text("Add weight measurement")}
  Info("Recent measurements",history.joinToString("\n").ifBlank{"No measurements yet. Add your first weight."})
 }
}
@Composable private fun WeightTrendChart(values:List<Float>,modifier:Modifier=Modifier){
 Canvas(modifier){if(values.isNotEmpty()){val minV=(values.minOrNull()?:0f)-1f;val maxV=(values.maxOrNull()?:1f)+1f;val range=(maxV-minV).coerceAtLeast(1f);val left=12f;val right=size.width-12f;val top=12f;val bottom=size.height-12f
 for(i in 0..3){val y=top+(bottom-top)*i/3f;drawLine(color=Color(0xFFEDE4DC),start=androidx.compose.ui.geometry.Offset(left,y),end=androidx.compose.ui.geometry.Offset(right,y),strokeWidth=1f)}
 val pts=values.mapIndexed{index,value->androidx.compose.ui.geometry.Offset(if(values.size==1)(left+right)/2 else left+(right-left)*index/(values.size-1),bottom-(value-minV)/range*(bottom-top))}
 pts.zipWithNext().forEach{(a,b)->drawLine(color=Green,start=a,end=b,strokeWidth=4f,cap=androidx.compose.ui.graphics.StrokeCap.Round)}
 pts.forEach{drawCircle(color=Green,radius=5f,center=it);drawCircle(color=Color.White,radius=2f,center=it)}
 }}
}
@Composable private fun Coach(k:Int,g:Int,p:Int,m:Modifier,api:String){
 val scope=rememberCoroutineScope();val context=LocalContext.current;val tts=remember{TextToSpeech(context){}};DisposableEffect(tts){onDispose{tts.shutdown()}};var question by remember{mutableStateOf("")};var answer by remember{mutableStateOf("Hi! Ask me about your food, calories, or nutrition.")};var loading by remember{mutableStateOf(false)}
 Column(m.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
 Text("Leanora Coach",fontSize=28.sp,fontWeight=FontWeight.Bold,color=Ink)
 Info("Today","Calories: "+k+" / "+g+" kcal • Protein: "+p+" g")
 Info("Coach",answer)
 OutlinedButton(onClick={tts.speak(answer,TextToSpeech.QUEUE_FLUSH,null,"leanora-coach")},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.VolumeUp,null);Spacer(Modifier.width(8.dp));Text("Read coach response aloud")}
 OutlinedTextField(question,{question=it},modifier=Modifier.fillMaxWidth(),label={Text("Ask a nutrition question")})
 Button(enabled=!loading&&question.isNotBlank(),onClick={loading=true;scope.launch{answer=withContext(Dispatchers.IO){askLeanora(api,question,k,g,p)};loading=false}},modifier=Modifier.fillMaxWidth()){Text(if(loading)"Thinking..." else "Ask AI Coach")}
 Text("AI estimates are educational, not medical advice.",color=Muted,fontSize=12.sp)
 }
}
@Composable private fun Settings(g:Int,w:Float,t:Float,waterGoal:Int,m:Modifier,goal:()->Unit,weight:()->Unit,onWaterGoalChange:(Int)->Unit,dark:Boolean,onDark:(Boolean)->Unit,backend:String,onBackend:(String)->Unit,target:()->Unit,editProfile:()->Unit){
 val scope=rememberCoroutineScope();var apiChecking by remember{mutableStateOf(false)};var apiStatus by remember{mutableStateOf("Not tested yet")};var apiOk by remember{mutableStateOf<Boolean?>(null)};var waterGoalText by remember{mutableStateOf(waterGoal.toString())};var waterGoalDialog by remember{mutableStateOf(false)}
 Column(m.fillMaxSize().padding(horizontal=18.dp,vertical=16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  Text("Settings",fontSize=30.sp,fontWeight=FontWeight.Bold,color=Ink);Text("Personalize your Leanora experience",color=Muted)
  Card(shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Green)){Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("YOUR PROFILE",fontSize=12.sp,letterSpacing=1.5.sp,color=Color.White.copy(alpha=.8f));Text("Nutrition & goals",fontSize=21.sp,fontWeight=FontWeight.Bold,color=Color.White);Text("Keep your details up to date",color=Color.White.copy(alpha=.85f))};Icon(Icons.Default.PersonOutline,null,tint=Color.White,modifier=Modifier.size(34.dp))}}
  SettingsSection("Profile & goals",Icons.Default.Person){SettingsAction(Icons.Default.Edit,"Edit personal profile","Name, age, activity and goal",editProfile);SettingsAction(Icons.Default.LocalFireDepartment,"Daily calorie goal","${g} kcal • Tap to adjust",goal);SettingsAction(Icons.Default.MonitorWeight,"Current weight","${w} kg • Update measurement",weight);SettingsAction(Icons.Default.Flag,"Target weight","${t} kg • Set your goal",target)}
  SettingsSection("Hydration",Icons.Default.WaterDrop){SettingsAction(Icons.Default.WaterDrop,"Daily water target","${waterGoal} ml • Tap to change",{waterGoalText=waterGoal.toString();waterGoalDialog=true})}
  SettingsSection("Appearance",Icons.Default.Palette){Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.DarkMode,null,tint=Green);Column(Modifier.weight(1f).padding(start=12.dp)){Text("Dark mode",fontWeight=FontWeight.SemiBold,color=Ink);Text("Use a darker color scheme",fontSize=12.sp,color=Muted)};Switch(checked=dark,onCheckedChange=onDark)}}
  SettingsSection("Connectivity",Icons.Default.Cloud){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Backend API URL",fontWeight=FontWeight.SemiBold,color=Ink);OutlinedTextField(value=backend,onValueChange=onBackend,modifier=Modifier.fillMaxWidth(),placeholder={Text("https://your-service.onrender.com")},singleLine=true,shape=RoundedCornerShape(14.dp),leadingIcon={Icon(Icons.Default.Link,null)});Button(enabled=!apiChecking&&backend.isNotBlank(),onClick={apiChecking=true;apiOk=null;apiStatus="Checking server…";scope.launch{val result=withContext(Dispatchers.IO){try{val conn=(URL(backend.trim().trimEnd('/')).openConnection() as java.net.HttpURLConnection);conn.requestMethod="GET";conn.connectTimeout=10000;conn.readTimeout=10000;val code=conn.responseCode;conn.disconnect();code}catch(e:Exception){-1}};apiChecking=false;apiOk=result in 200..499;apiStatus=if(result in 200..499)"Server reachable (HTTP $result)" else "Connection failed • check URL/network"}} ,modifier=Modifier.fillMaxWidth()){Text(if(apiChecking)"Testing connection…" else "Test backend connection")};Text(apiStatus,color=when(apiOk){true->Color(0xFF248A57);false->Color(0xFFB3261E);null->Muted},fontSize=13.sp)}}
  if (waterGoalDialog) {
    AlertDialog(
     onDismissRequest = { waterGoalDialog = false },
     title = { Text("Daily water target") },
     text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
       Text("Set a daily hydration goal in millilitres.")
       OutlinedTextField(
        value = waterGoalText,
        onValueChange = { waterGoalText = it.filter(Char::isDigit).take(5) },
        label = { Text("Goal (ml)") },
        singleLine = true
       )
      }
     },
     confirmButton = {
      TextButton(onClick = {
       val goalValue = waterGoalText.toIntOrNull()
       if (goalValue != null && goalValue in 500..10000) onWaterGoalChange(goalValue)
       waterGoalDialog = false
      }) { Text("Save") }
     },
     dismissButton = {
      TextButton(onClick = { waterGoalDialog = false }) { Text("Cancel") }
     }
    )
   }
  SettingsSection("Privacy & app",Icons.Default.Security){Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Lock,null,tint=Green);Column(Modifier.weight(1f).padding(start=12.dp)){Text("On-device privacy",fontWeight=FontWeight.SemiBold,color=Ink);Text("Food logs and settings are stored locally on this device.",fontSize=13.sp,color=Muted)}};Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Info,null,tint=Green);Text("Leanora • Android",Modifier.weight(1f).padding(start=12.dp),color=Ink);Text("v0.3",color=Muted)}}
 }
}
@Composable private fun SettingsSection(title:String,icon:androidx.compose.ui.graphics.vector.ImageVector,content:@Composable ColumnScope.()->Unit){Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Column{Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=Green);Text(title,Modifier.padding(start=10.dp),fontWeight=FontWeight.Bold,color=Ink)};HorizontalDivider(color=Cream);content()}}}
@Composable private fun SettingsAction(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,subtitle:String,onClick:()->Unit){TextButton(onClick=onClick,modifier=Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=Green);Column(Modifier.weight(1f).padding(start=12.dp)){Text(title,fontWeight=FontWeight.SemiBold,color=Ink);Text(subtitle,fontSize=12.sp,color=Muted)};Icon(Icons.Default.ChevronRight,null,tint=Muted)}}}
@Composable private fun Macro(title:String,value:String,m:Modifier){Card(m,shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){Column(Modifier.padding(12.dp)){Text(title,color=Muted,fontSize=12.sp);Text(value,color=Ink,fontWeight=FontWeight.Bold)}}}
@Composable private fun FoodCard(x:Food,m:Modifier=Modifier){Card(m,shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){Text(x.name,color=Ink,fontWeight=FontWeight.SemiBold,fontSize=16.sp);Text(x.meal,color=Muted,fontSize=12.sp);Text("P ${x.protein}g  •  C ${x.carbs}g  •  F ${x.fat}g",fontSize=12.sp,color=Muted)};Column(horizontalAlignment=Alignment.End){Text(x.kcal.toString(),color=Green,fontWeight=FontWeight.Bold,fontSize=18.sp);Text("kcal",color=Muted,fontSize=11.sp)}}}}
@Composable private fun Info(title:String,body:String){Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){Text(title,color=Ink,fontWeight=FontWeight.SemiBold);Text(body,color=Muted)}}}
private fun foodKey(date:LocalDate) = "foods_" + date.toString()
private fun saveFoods(p:android.content.SharedPreferences,foods:List<Food>,date:LocalDate=LocalDate.now()){val a=JSONArray();foods.forEach{a.put(JSONObject().put("name",it.name).put("kcal",it.kcal).put("protein",it.protein).put("carbs",it.carbs).put("fat",it.fat).put("meal",it.meal))};p.edit().putString(foodKey(date),a.toString()).apply()}
private fun loadFoods(p:android.content.SharedPreferences,date:LocalDate=LocalDate.now()):List<Food>{val raw=p.getString(foodKey(date),null)?:return emptyList();return try{val a=JSONArray(raw);(0 until a.length()).map{val o=a.getJSONObject(it);Food(o.getString("name"),o.getInt("kcal"),o.getInt("protein"),o.getInt("carbs"),o.getInt("fat"),o.getString("meal"))}}catch(_:Exception){emptyList()}}

private fun lookupFoodBarcode(code:String):Food{
 return try{
  val conn=(URL("https://world.openfoodfacts.org/api/v2/product/"+code+".json").openConnection() as java.net.HttpURLConnection)
  conn.connectTimeout=10000;conn.readTimeout=10000;conn.setRequestProperty("User-Agent","Leanora Android nutrition tracker")
  val json=JSONObject(conn.inputStream.bufferedReader().use{it.readText()});conn.disconnect()
  val product=json.optJSONObject("product")?:JSONObject();val n=product.optJSONObject("nutriments")?:JSONObject()
  val title=product.optString("product_name").ifBlank{"Scanned product "+code}
  val kcal=n.optDouble("energy-kcal_100g",n.optDouble("energy-kcal",0.0)).toInt().coerceAtLeast(0)
  Food(title+" (100 g)",kcal,n.optDouble("proteins_100g",0.0).toInt().coerceAtLeast(0),n.optDouble("carbohydrates_100g",0.0).toInt().coerceAtLeast(0),n.optDouble("fat_100g",0.0).toInt().coerceAtLeast(0),"Snack")
 }catch(_:Exception){Food("Barcode "+code+" (nutrition unavailable)",0,0,0,0,"Snack")}
}

private suspend fun askLeanora(base:String,question:String,kcal:Int,goal:Int,protein:Int):String{
 if(base.isBlank())return "Please set your deployed backend URL in Settings to activate AI Coach."
 return try{withContext(Dispatchers.IO){val connection=(URL(base.trimEnd('/')+"/v1/coach").openConnection() as java.net.HttpURLConnection);connection.requestMethod="POST";connection.connectTimeout=15000;connection.readTimeout=30000;connection.doOutput=true;connection.setRequestProperty("Content-Type","application/json");val payload=JSONObject().put("message",question).put("language","English").put("calorie_goal",goal).put("calories_consumed",kcal).put("protein_consumed_g",protein).toString();connection.outputStream.use{it.write(payload.toByteArray())};val code=connection.responseCode;val response=(if(code in 200..299)connection.inputStream else connection.errorStream).bufferedReader().use{it.readText()};connection.disconnect();if(code in 200..299)JSONObject(response).optString("answer","No answer returned.")else "AI server error "+code}}catch(_:Exception){"Could not connect. Verify the backend URL and network."}
}
private fun estimateFoodPhoto(base:String,bitmap:Bitmap):Food{
 if(base.isBlank())return Food("Photo estimate unavailable: set backend URL",0,0,0,0,"Meal")
 return try{
 val boundary="LeanoraBoundary"+System.currentTimeMillis();val bytes=java.io.ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.JPEG,85,bytes)
 val conn=(URL(base.trimEnd('/')+"/v1/food-estimate").openConnection() as java.net.HttpURLConnection);conn.requestMethod="POST";conn.doOutput=true;conn.connectTimeout=20000;conn.readTimeout=45000;conn.setRequestProperty("Content-Type","multipart/form-data; boundary="+boundary)
 conn.outputStream.use{out->out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"meal\"\r\n\r\nMeal\r\n").toByteArray());out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"image\"; filename=\"meal.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n").toByteArray());out.write(bytes.toByteArray());out.write(("\r\n--"+boundary+"--\r\n").toByteArray())}
 val status=conn.responseCode;val stream=if(status in 200..299)conn.inputStream else conn.errorStream;val response=stream.bufferedReader().use{it.readText()};conn.disconnect()
 if(status !in 200..299) return Food("Food photo estimate failed",0,0,0,0,"Meal")
 val obj=JSONObject(response);Food(obj.optString("food_name","Estimated meal")+" ("+obj.optInt("portion_g",0)+" g)",obj.optInt("calories_kcal",0),obj.optInt("protein_g",0),obj.optInt("carbs_g",0),obj.optInt("fat_g",0),"Meal")
 }catch(_:Exception){Food("Photo estimate failed • check backend",0,0,0,0,"Meal")}
}
