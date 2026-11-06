package com.leanora.app
import android.content.Context
import android.graphics.Bitmap
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
import android.widget.Toast
import com.google.android.gms.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.net.URL
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
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

private val Cream=Color(0xFFF7F5EF); private val Green=Color(0xFF176B52); private val Ink=Color(0xFF1C2924); private val Muted=Color(0xFF718078)
data class Food(val name:String,val kcal:Int,val protein:Int,val carbs:Int,val fat:Int,val meal:String)
private val catalog=listOf(Food("Idli (2)",130,4,26,1,"Breakfast"),Food("Plain dosa",170,4,30,4,"Breakfast"),Food("Chapati",120,3,22,3,"Lunch"),Food("Cooked rice (150 g)",195,4,42,0,"Lunch"),Food("Chicken breast (100 g)",165,31,0,4,"Lunch"),Food("Boiled egg",78,6,1,5,"Snack"),Food("Banana",105,1,27,0,"Snack"),Food("Paneer (100 g)",265,18,4,20,"Dinner"),Food("Curd (100 g)",61,3,5,3,"Dinner"))
class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{LeanoraApp(applicationContext)}}}
@Composable fun LeanoraApp(ctx:Context){
 val prefs=remember{ctx.getSharedPreferences("leanora",Context.MODE_PRIVATE)}; val scope=rememberCoroutineScope()
 var foods by remember{mutableStateOf(loadFoods(prefs))}; var goal by remember{mutableIntStateOf(prefs.getInt("goal",1850))}; var weight by remember{mutableFloatStateOf(prefs.getFloat("weight",78f))}; var target by remember{mutableFloatStateOf(prefs.getFloat("target",72f))}
 var tab by remember{mutableIntStateOf(0)}; var dark by remember{mutableStateOf(prefs.getBoolean("dark",false))}; var backendUrl by remember{mutableStateOf(prefs.getString("backend","") ?: "")}; var voiceResult by remember{mutableStateOf("")}; val cameraLauncher=rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()){ bitmap -> if(bitmap!=null){scope.launch{val food=withContext(Dispatchers.IO){estimateFoodPhoto(backendUrl,bitmap)};foods=foods+food;saveFoods(prefs,foods);Toast.makeText(ctx,"Food estimate added: "+food.name,Toast.LENGTH_LONG).show()}} }; val voiceLauncher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){ result -> if(result.resultCode==android.app.Activity.RESULT_OK){ voiceResult=result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty(); if(voiceResult.isNotBlank()){ val matched=catalog.firstOrNull{voiceResult.contains(it.name.substringBefore("("),ignoreCase=true)||it.name.substringBefore("(").contains(voiceResult,ignoreCase=true)}; val food=matched ?: Food(voiceResult.take(60),200,8,25,7,"Snack"); foods=foods+food; saveFoods(prefs,foods) } } }; var picker by remember{mutableStateOf(false)}; var goalDialog by remember{mutableStateOf(false)}; var weightDialog by remember{mutableStateOf(false)}; var targetDialog by remember{mutableStateOf(false)}; var targetText by remember{mutableStateOf(target.toString())}; var goalText by remember{mutableStateOf(goal.toString())}; var weightText by remember{mutableStateOf(weight.toString())}
 val kcal=foods.sumOf{it.kcal}; val p=foods.sumOf{it.protein}; val c=foods.sumOf{it.carbs}; val f=foods.sumOf{it.fat}
 MaterialTheme(colorScheme=if(dark)darkColorScheme(primary=Color(0xFF69C6A4),background=Color(0xFF101815),surface=Color(0xFFF0F3F0),onSurface=Ink)else lightColorScheme(primary=Green,background=Cream,surface=Color.White)){
 Scaffold(containerColor=if(dark)Color(0xFF101815)else Cream,bottomBar={NavigationBar(containerColor=if(dark)Color(0xFF1A2420)else Color.White){NavigationBarItem(tab==0,{tab=0},icon={Icon(Icons.Default.Home,null)},label={Text("Today")});NavigationBarItem(tab==1,{tab=1},icon={Icon(Icons.Default.Restaurant,null)},label={Text("Food")});NavigationBarItem(tab==2,{tab=2},icon={Icon(Icons.Default.ShowChart,null)},label={Text("Progress")});NavigationBarItem(tab==3,{tab=3},icon={Icon(Icons.Default.SmartToy,null)},label={Text("Coach")});NavigationBarItem(tab==4,{tab=4},icon={Icon(Icons.Default.Settings,null)},label={Text("Settings")})}},floatingActionButton={if(tab<2)FloatingActionButton(onClick={picker=true},containerColor=Green){Icon(Icons.Default.Add,"Add food",tint=Color.White)}}){pad->
 when(tab){0->Dashboard(foods,kcal,goal,p,c,f,Modifier.padding(pad)){picker=true};1->Diary(foods,Modifier.padding(pad),{picker=true},{i->foods=foods.toMutableList().also{it.removeAt(i)};saveFoods(prefs,foods)});2->Progress(weight,target,prefs.getString("weight_history","[]") ?: "[]",Modifier.padding(pad)){weightText=weight.toString();weightDialog=true};3->Coach(kcal,goal,p,Modifier.padding(pad),backendUrl);else->Settings(goal,weight,target,Modifier.padding(pad),{goalText=goal.toString();goalDialog=true},{weightText=weight.toString();weightDialog=true},dark,{dark=it;prefs.edit().putBoolean("dark",it).apply()},backendUrl,{backendUrl=it;prefs.edit().putString("backend",it).apply()},{targetText=target.toString();targetDialog=true})}
 }
 if(picker)AlertDialog(onDismissRequest={picker=false},title={Text("Add food")},text={Column{Button(onClick={cameraLauncher.launch(null)},modifier=Modifier.fillMaxWidth()){Text("Camera • estimate food")};Button(onClick={val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-IN").putExtra(RecognizerIntent.EXTRA_PROMPT,"Say food name and quantity");voiceLauncher.launch(intent)},modifier=Modifier.fillMaxWidth()){Text("Voice food logging")};LazyColumn(Modifier.heightIn(max=350.dp)){items(catalog){food->TextButton(onClick={foods=foods+food;saveFoods(prefs,foods);picker=false},modifier=Modifier.fillMaxWidth()){Column(Modifier.fillMaxWidth()){Text(food.name,color=Ink,fontWeight=FontWeight.SemiBold);Text(food.kcal.toString()+" kcal • P"+food.protein+" C"+food.carbs+" F"+food.fat+"g",color=Muted)}}}}}}},confirmButton={TextButton(onClick={picker=false}){Text("Close")}})
 if(goalDialog)AlertDialog(onDismissRequest={goalDialog=false},title={Text("Daily calorie goal")},text={OutlinedTextField(goalText,{goalText=it.filter(Char::isDigit)},label={Text("kcal")})},confirmButton={TextButton(onClick={goalText.toIntOrNull()?.let{if(it in 800..6000){goal=it;prefs.edit().putInt("goal",it).apply()}};goalDialog=false}){Text("Save")}})
 if(targetDialog)AlertDialog(onDismissRequest={targetDialog=false},title={Text("Target weight")},text={OutlinedTextField(targetText,{targetText=it},label={Text("kg")})},confirmButton={TextButton(onClick={targetText.toFloatOrNull()?.let{if(it in 30f..300f){target=it;prefs.edit().putFloat("target",it).apply()}};targetDialog=false}){Text("Save")}})\n if(weightDialog)AlertDialog(onDismissRequest={weightDialog=false},title={Text("Current weight")},text={OutlinedTextField(weightText,{weightText=it},label={Text("kg")})},confirmButton={TextButton(onClick={weightText.toFloatOrNull()?.let{if(it in 30f..300f){weight=it;val history=JSONArray(prefs.getString("weight_history","[]")?: "[]");history.put(JSONObject().put("date",LocalDate.now().toString()).put("kg",it));prefs.edit().putFloat("weight",it).putString("weight_history",history.toString()).apply()}};weightDialog=false}){Text("Save")}})
 }
}
@Composable private fun Dashboard(food:List<Food>,k:Int,g:Int,p:Int,c:Int,f:Int,m:Modifier,add:()->Unit){LazyColumn(m.fillMaxSize().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(top=20.dp,bottom=20.dp)){item{Text("LEANORA",color=Green,fontWeight=FontWeight.Bold,letterSpacing=3.sp);Text("Your wellness,\nin balance.",fontSize=30.sp,fontWeight=FontWeight.Bold,color=Ink);Text("Daily nutrition overview",color=Muted)};item{Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=Green)){Column(Modifier.fillMaxWidth().padding(22.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Calories remaining",color=Color.White.copy(alpha=.8f));Text((g-k).coerceAtLeast(0).toString()+" kcal",fontSize=34.sp,fontWeight=FontWeight.Bold,color=Color.White);Text(k.toString()+" / "+g+" kcal consumed",color=Color.White);LinearProgressIndicator(progress={(k.toFloat()/g).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth(),color=Color.White,trackColor=Color.White.copy(alpha=.2f))}}};item{Text("Macros today",fontSize=20.sp,fontWeight=FontWeight.SemiBold,color=Ink);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Macro("Protein",p.toString()+"g",Modifier.weight(1f));Macro("Carbs",c.toString()+"g",Modifier.weight(1f));Macro("Fat",f.toString()+"g",Modifier.weight(1f))}};item{Row(verticalAlignment=Alignment.CenterVertically){Text("Today's meals",fontSize=20.sp,fontWeight=FontWeight.SemiBold,color=Ink);Spacer(Modifier.weight(1f));TextButton(onClick=add){Text("Add +")}}};if(food.isEmpty())item{Info("Nothing logged yet","Add food to start tracking nutrition.")};items(food.takeLast(5).reversed()){FoodCard(it)}}}
@Composable private fun Diary(food:List<Food>,m:Modifier,add:()->Unit,remove:(Int)->Unit){Column(m.fillMaxSize().padding(16.dp)){Text("Food diary",fontSize=28.sp,fontWeight=FontWeight.Bold,color=Ink);Text("Track meals throughout the day",color=Muted);Button(onClick=add,modifier=Modifier.fillMaxWidth()){Text("Add food")};LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(food.size){i->Row(verticalAlignment=Alignment.CenterVertically){FoodCard(food[i],Modifier.weight(1f));TextButton(onClick={remove(i)}){Text("Remove")}}}}}}
@Composable private fun Progress(w:Float,t:Float,historyJson:String,m:Modifier,edit:()->Unit){val history=remember(historyJson){try{val arr=JSONArray(historyJson);(0 until arr.length()).map{val o=arr.getJSONObject(it);o.optString("date")+" • "+o.optDouble("kg")+" kg"}.reversed().take(8)}catch(_:Exception){emptyList<String>()}};Column(m.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){Text("Progress",fontSize=28.sp,fontWeight=FontWeight.Bold,color=Ink);Info("Current weight",w.toString()+" kg");Info("Target weight",t.toString()+" kg");Info("Remaining to target",String.format(Locale.US,"%.1f kg",kotlin.math.abs(w-t)));Button(onClick=edit){Text("Add weight measurement")};Info("Recent measurements",history.joinToString("\\n").ifBlank{"No measurements yet. Add your first weight."})}}
@Composable private fun Coach(k:Int,g:Int,p:Int,m:Modifier,api:String){
 val scope=rememberCoroutineScope();val context=LocalContext.current;val tts=remember{TextToSpeech(context){status->if(status==TextToSpeech.SUCCESS)ttsLanguageSetup()}};DisposableEffect(tts){onDispose{tts.shutdown()}};var question by remember{mutableStateOf("")};var answer by remember{mutableStateOf("Hi! Ask me about your food, calories, or nutrition.")};var loading by remember{mutableStateOf(false)}
 Column(m.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
 Text("Leanora Coach",fontSize=28.sp,fontWeight=FontWeight.Bold,color=Ink)
 Info("Today","Calories: "+k+" / "+g+" kcal • Protein: "+p+" g")
 Info("Coach",answer)
 OutlinedTextField(question,{question=it},modifier=Modifier.fillMaxWidth(),label={Text("Ask a nutrition question")})
 Button(enabled=!loading&&question.isNotBlank(),onClick={loading=true;scope.launch{answer=withContext(Dispatchers.IO){askLeanora(api,question,k,g,p)};loading=false}},modifier=Modifier.fillMaxWidth()){Text(if(loading)"Thinking..." else "Ask AI Coach")}
 Text("AI estimates are educational, not medical advice.",color=Muted,fontSize=12.sp)
 }
}
@Composable private fun Settings(g:Int,w:Float,t:Float,m:Modifier,goal:()->Unit,weight:()->Unit){Column(m.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("Settings",fontSize=28.sp,fontWeight=FontWeight.Bold,color=Ink);TextButton(onClick=goal){Text("Daily calorie goal: "+g+" kcal")};TextButton(onClick=weight){Text("Current weight: "+w+" kg")};Info("Target weight",t.toString()+" kg");Info("Privacy","Food logs and settings are stored locally on this device.");Info("Leanora","Android • 0.1.0")}}
@Composable private fun Macro(title:String,value:String,m:Modifier){Card(m,shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){Column(Modifier.padding(12.dp)){Text(title,color=Muted,fontSize=12.sp);Text(value,color=Ink,fontWeight=FontWeight.Bold)}}}
@Composable private fun FoodCard(x:Food,m:Modifier=Modifier){Card(m,shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(x.name,color=Ink,fontWeight=FontWeight.SemiBold);Text(x.meal+" • P"+x.protein+" C"+x.carbs+" F"+x.fat+"g",fontSize=11.sp,color=Muted)};Text(x.kcal.toString()+" kcal",color=Green,fontWeight=FontWeight.Bold)}}}
@Composable private fun Info(title:String,body:String){Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){Text(title,color=Ink,fontWeight=FontWeight.SemiBold);Text(body,color=Muted)}}}
private fun todayKey() = "foods_" + LocalDate.now().toString()
private fun saveFoods(p:android.content.SharedPreferences,foods:List<Food>){val a=JSONArray();foods.forEach{a.put(JSONObject().put("name",it.name).put("kcal",it.kcal).put("protein",it.protein).put("carbs",it.carbs).put("fat",it.fat).put("meal",it.meal))};p.edit().putString(todayKey(),a.toString()).apply()}
private fun loadFoods(p:android.content.SharedPreferences):List<Food>{val raw=p.getString(todayKey(),null)?:return emptyList();return try{val a=JSONArray(raw);(0 until a.length()).map{val o=a.getJSONObject(it);Food(o.getString("name"),o.getInt("kcal"),o.getInt("protein"),o.getInt("carbs"),o.getInt("fat"),o.getString("meal"))}}catch(_:Exception){emptyList()}}

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

private fun ttsLanguageSetup(){}\nprivate suspend fun askLeanora(base:String,question:String,kcal:Int,goal:Int,protein:Int):String{
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
