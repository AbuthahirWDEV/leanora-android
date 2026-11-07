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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private val Cream=Color(0xFFFFF8F1); private val Orange=Color(0xFFD45F2C); private val Ink=Color(0xFF29231F); private val Muted=Color(0xFF756D66); private val Mint=Color(0xFFFFE0CA); private val Lavender=Color(0xFFF8E7DA)
data class Food(val name:String,val kcal:Int,val protein:Int,val carbs:Int,val fat:Int,val meal:String)
private val catalog=listOf(Food("Idli (2)",130,4,26,1,"Breakfast"),Food("Plain dosa",170,4,30,4,"Breakfast"),Food("Chapati",120,3,22,3,"Lunch"),Food("Cooked rice (150 g)",195,4,42,0,"Lunch"),Food("Chicken breast (100 g)",165,31,0,4,"Lunch"),Food("Boiled egg",78,6,1,5,"Snack"),Food("Banana",105,1,27,0,"Snack"),Food("Paneer (100 g)",265,18,4,20,"Dinner"),Food("Curd (100 g)",61,3,5,3,"Dinner"))
class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{LeanoraApp(applicationContext)}}}

private enum class CameraMode { FOOD_PHOTO, BARCODE }

@Composable private fun FoodDetailDialog(food:Food,onDismiss:()->Unit,onCopy:()->Unit,onEdit:()->Unit,onDelete:()->Unit){
 Dialog(onDismissRequest=onDismiss,properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)){
  Surface(modifier=Modifier.fillMaxWidth(.94f).widthIn(max=480.dp),shape=RoundedCornerShape(28.dp),color=MaterialTheme.colorScheme.surface,tonalElevation=8.dp){
   Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
    Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(food.name,fontSize=24.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface);Text(food.meal,color=MaterialTheme.colorScheme.onSurfaceVariant)};IconButton(onClick=onDismiss){Icon(Icons.Default.Close,"Close food details")}}
    Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer),shape=RoundedCornerShape(20.dp)){Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("ENERGY",fontSize=11.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurfaceVariant,letterSpacing=1.sp);Text("${food.kcal} kcal",fontSize=30.sp,fontWeight=FontWeight.ExtraBold,color=MaterialTheme.colorScheme.onSurface)};Icon(Icons.Default.LocalFireDepartment,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(34.dp))}}
    Text("Nutrition per logged portion",fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onSurface)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){DetailMacro("Protein","${food.protein} g",Modifier.weight(1f));DetailMacro("Carbohydrates","${food.carbs} g",Modifier.weight(1f));DetailMacro("Fat","${food.fat} g",Modifier.weight(1f))}
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()){
     OutlinedButton(onClick=onDelete,modifier=Modifier.weight(1f)){Icon(Icons.Default.DeleteOutline,null);Spacer(Modifier.width(6.dp));Text("Delete")}
     OutlinedButton(onClick=onEdit,modifier=Modifier.weight(1f)){Icon(Icons.Default.Edit,null);Spacer(Modifier.width(6.dp));Text("Edit")}
     Button(onClick=onCopy,modifier=Modifier.weight(1f)){Icon(Icons.Default.ContentCopy,null);Spacer(Modifier.width(6.dp));Text("Copy")}
    }
   }
  }
 }
}

@Composable private fun DetailMacro(label:String,value:String,modifier:Modifier=Modifier){
 Card(modifier,shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)){Column(Modifier.padding(12.dp)){Text(label,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(value,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface)}}
}

@Composable private fun CameraCaptureDialog(mode:CameraMode,onClose:()->Unit,onBarcode:(String)->Unit,onPhoto:(Bitmap)->Unit){
 val context=LocalContext.current
 var cameraAllowed by remember{mutableStateOf(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)}
 val permissionRequest=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){cameraAllowed=it}
 LaunchedEffect(Unit){if(!cameraAllowed)permissionRequest.launch(Manifest.permission.CAMERA)}
 if(cameraAllowed)CameraPreviewDialog(mode,onClose,onBarcode,onPhoto)
 else AlertDialog(onDismissRequest=onClose,title={Text("Camera access needed")},text={Text("Allow camera access to scan food barcodes or take a meal photo.")},confirmButton={TextButton(onClick={permissionRequest.launch(Manifest.permission.CAMERA)}){Text("Allow camera")}},dismissButton={TextButton(onClick=onClose){Text("Cancel")}})
}

@Composable private fun CameraPreviewDialog(mode:CameraMode,onClose:()->Unit,onBarcode:(String)->Unit,onPhoto:(Bitmap)->Unit){
 val context=LocalContext.current
 val lifecycleOwner=LocalLifecycleOwner.current
 val mainExecutor=remember{ContextCompat.getMainExecutor(context)}
 val analyzerExecutor=remember{Executors.newSingleThreadExecutor()}
 var imageCapture by remember{mutableStateOf<ImageCapture?>(null)}
 var cameraError by remember{mutableStateOf<String?>(null)}
 var scanning by remember{mutableStateOf(true)}
 val scanner=remember(mode){if(mode==CameraMode.BARCODE){val options=com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_EAN_13,Barcode.FORMAT_EAN_8,Barcode.FORMAT_UPC_A,Barcode.FORMAT_UPC_E).build();BarcodeScanning.getClient(options)}else null}
 val callbackBarcode=rememberUpdatedState(onBarcode)
 val callbackPhoto=rememberUpdatedState(onPhoto)
 val scanGate=remember{AtomicBoolean(false)}
 DisposableEffect(lifecycleOwner,mode){
  onDispose{runCatching{ProcessCameraProvider.getInstance(context).addListener({runCatching{ProcessCameraProvider.getInstance(context).get().unbindAll()}},mainExecutor)};scanner?.close();analyzerExecutor.shutdown()}
 }
 Dialog(onDismissRequest=onClose,properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)){
  Surface(modifier=Modifier.fillMaxSize().padding(14.dp),shape=RoundedCornerShape(28.dp),color=Color(0xFF191817)){
   Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
    Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(if(mode==CameraMode.BARCODE)"Scan product barcode" else "Food photo estimate",fontSize=22.sp,fontWeight=FontWeight.Bold,color=Color.White);Text(if(mode==CameraMode.BARCODE)"Point at a UPC or EAN label" else "Fit the full meal in the frame",fontSize=13.sp,color=Color(0xFFE3D8CF))};IconButton(onClick=onClose){Icon(Icons.Default.Close,"Close camera",tint=Color.White)}}
    Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){
     AndroidView(modifier=Modifier.fillMaxSize(),factory={androidContext->
      val view=PreviewView(androidContext)
      val providerFuture=ProcessCameraProvider.getInstance(context)
      providerFuture.addListener({
       runCatching{
        val provider=providerFuture.get();val preview=Preview.Builder().build().also{it.surfaceProvider=view.surfaceProvider};val capture=ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();imageCapture=capture
        if(mode==CameraMode.BARCODE&&scanner!=null){val analysis=ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();analysis.setAnalyzer(analyzerExecutor){proxy->val media=proxy.image;if(media==null){proxy.close()}else{scanner.process(InputImage.fromMediaImage(media,proxy.imageInfo.rotationDegrees)).addOnSuccessListener{codes->val found=codes.firstOrNull{code->val raw=code.rawValue.orEmpty();code.format in setOf(Barcode.FORMAT_EAN_13,Barcode.FORMAT_EAN_8,Barcode.FORMAT_UPC_A,Barcode.FORMAT_UPC_E)&&raw.length in setOf(8,12,13,14)&&raw.all(Char::isDigit)};if(found!=null&&scanGate.compareAndSet(false,true)){scanning=false;callbackBarcode.value(found.rawValue.orEmpty())}}.addOnCompleteListener{proxy.close()}}};provider.unbindAll();provider.bindToLifecycle(lifecycleOwner,CameraSelector.DEFAULT_BACK_CAMERA,preview,capture,analysis)}else{provider.unbindAll();provider.bindToLifecycle(lifecycleOwner,CameraSelector.DEFAULT_BACK_CAMERA,preview,capture)}
       }.onFailure{cameraError=it.localizedMessage?:"Unable to start the camera"}
      },mainExecutor)
      view.implementationMode=PreviewView.ImplementationMode.COMPATIBLE;view.scaleType=PreviewView.ScaleType.FILL_CENTER;view
     })
     if(mode==CameraMode.BARCODE){Canvas(Modifier.fillMaxWidth(.78f).height(150.dp)){val color=Color(0xFFFFA36F);val stroke=5.dp.toPx();val w=size.width;val h=size.height;val corner=28.dp.toPx();listOf(androidx.compose.ui.geometry.Offset(0f,0f),androidx.compose.ui.geometry.Offset(w,0f),androidx.compose.ui.geometry.Offset(0f,h),androidx.compose.ui.geometry.Offset(w,h)).forEachIndexed{index,point->val sx=if(index%2==0)1f else -1f;val sy=if(index<2)1f else -1f;drawLine(color,point,androidx.compose.ui.geometry.Offset(point.x+sx*corner,point.y),stroke,cap=androidx.compose.ui.graphics.StrokeCap.Round);drawLine(color,point,androidx.compose.ui.geometry.Offset(point.x,point.y+sy*corner),stroke,cap=androidx.compose.ui.graphics.StrokeCap.Round)}}}
     cameraError?.let{Text(it,color=Color.White,modifier=Modifier.align(Alignment.Center).background(Color(0xAA000000),RoundedCornerShape(12.dp)).padding(12.dp))}
    }
    if(mode==CameraMode.FOOD_PHOTO)Button(onClick={val capture=imageCapture;if(capture==null){cameraError="Camera is starting. Try again in a moment."}else{val file=File(context.cacheDir,"leanora-food-${System.currentTimeMillis()}.jpg");val output=ImageCapture.OutputFileOptions.Builder(file).build();capture.takePicture(output,mainExecutor,object:ImageCapture.OnImageSavedCallback{override fun onImageSaved(result:ImageCapture.OutputFileResults){val bitmap=BitmapFactory.decodeFile(file.absolutePath);file.delete();if(bitmap!=null)callbackPhoto.value(bitmap)else cameraError="Could not read the captured photo."};override fun onError(exception:ImageCaptureException){file.delete();cameraError=exception.localizedMessage?:"Photo capture failed."}})}},modifier=Modifier.fillMaxWidth().height(54.dp)){Icon(Icons.Default.CameraAlt,null);Spacer(Modifier.width(8.dp));Text("Capture food photo")}
    else Text(if(scanning)"Scanning for a food product barcode…" else "Barcode found",color=Color.White,modifier=Modifier.align(Alignment.CenterHorizontally))
    Text(if(mode==CameraMode.BARCODE)"Scans product UPC/EAN codes only. No QR codes or links." else "Your photo is used for this estimate and is not saved to your gallery.",fontSize=12.sp,color=Color(0xFFD0C5BC))
   }
  }
 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun LeanoraApp(ctx:Context){
 val prefs=remember{ctx.getSharedPreferences("leanora",Context.MODE_PRIVATE)}; val scope=rememberCoroutineScope()
 var diaryDate by remember{mutableStateOf(LocalDate.now())}; var foods by remember{mutableStateOf(loadFoods(prefs,diaryDate))}; var goal by remember{mutableIntStateOf(prefs.getInt("goal",1850))}; var weight by remember{mutableFloatStateOf(prefs.getFloat("weight",78f))}; var target by remember{mutableFloatStateOf(prefs.getFloat("target",72f))}
 var onboarded by remember{mutableStateOf(prefs.getBoolean("onboarded",false))}; var welcome by remember{mutableStateOf(!prefs.getBoolean("welcome_seen",false))}; var splash by remember{mutableStateOf(true)}
 LaunchedEffect(Unit){delay(1800);splash=false}
 var profileName by remember{mutableStateOf(prefs.getString("profile_name","") ?: "")}
  var firstUseDate by remember{mutableStateOf(prefs.getString("first_use_date",null)?.let{runCatching{LocalDate.parse(it)}.getOrNull()} ?: LocalDate.of(2000,1,1))}
 var profileAge by remember{mutableIntStateOf(prefs.getInt("profile_age",26))}
 var profileSex by remember{mutableStateOf(prefs.getString("profile_sex","Male") ?: "Male")}
 var profileHeight by remember{mutableIntStateOf(prefs.getInt("profile_height",173))}
 var activityLevel by remember{mutableStateOf(prefs.getString("activity","Lightly active") ?: "Lightly active")}
 var fitnessGoal by remember{mutableStateOf(prefs.getString("fitness_goal","Lose weight") ?: "Lose weight")}
  var waterMl by remember{mutableIntStateOf(prefs.getInt("water_"+diaryDate.toString(),0))}; LaunchedEffect(diaryDate){waterMl=prefs.getInt("water_"+diaryDate.toString(),0)}; var waterGoal by remember{mutableIntStateOf(prefs.getInt("water_goal",2000))}; var tab by remember{mutableIntStateOf(0)}; var dark by remember{mutableStateOf(prefs.getBoolean("dark",false))}; var backendUrl by remember{mutableStateOf(prefs.getString("backend","https://leanora-android.onrender.com") ?: "https://leanora-android.onrender.com")}; var voiceResult by remember{mutableStateOf("")}; val scanContext=LocalContext.current; val voiceLauncher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){ result -> if(result.resultCode==android.app.Activity.RESULT_OK){ voiceResult=result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty(); if(voiceResult.isNotBlank()){ scope.launch{ val food=withContext(Dispatchers.IO){estimateFoodText(backendUrl,voiceResult)}; if(food.kcal>0){foods=foods+food;saveFoods(prefs,foods,diaryDate);Toast.makeText(ctx,"Voice food logged: "+food.name,Toast.LENGTH_LONG).show()}else Toast.makeText(ctx,food.name,Toast.LENGTH_LONG).show() } } } }; var picker by remember{mutableStateOf(false)}; var cameraMode by remember{mutableStateOf<CameraMode?>(null)}; var calendarOpen by remember{mutableStateOf(false)}; var selectedFoodIndex by remember{mutableIntStateOf(-1)}; var deleteFoodIndex by remember{mutableIntStateOf(-1)}; var barcodeDialog by remember{mutableStateOf(false)}; var barcodeText by remember{mutableStateOf("")}; var barcodeLoading by remember{mutableStateOf(false)}; var foodSearch by remember{mutableStateOf("")}; var showFavoritesOnly by remember{mutableStateOf(false)}; var favoriteFoods by remember{mutableStateOf(prefs.getStringSet("favorite_foods",emptySet())?.toSet()?:emptySet())}; var portionDialogFood by remember{mutableStateOf<Food?>(null)}; var portionText by remember{mutableStateOf("1")}; var editingFood by remember{mutableStateOf<Food?>(null)}; var editingFoodIndex by remember{mutableIntStateOf(-1)}; var customDialog by remember{mutableStateOf(false)}; var customName by remember{mutableStateOf("")}; var customKcal by remember{mutableStateOf("")}; var customProtein by remember{mutableStateOf("0")}; var customCarbs by remember{mutableStateOf("0")}; var customFat by remember{mutableStateOf("0")}; var customMeal by remember{mutableStateOf("Snack")}; var goalDialog by remember{mutableStateOf(false)}; var weightDialog by remember{mutableStateOf(false)}; var targetDialog by remember{mutableStateOf(false)}; var targetText by remember{mutableStateOf(target.toString())}; var goalText by remember{mutableStateOf(goal.toString())}; var weightText by remember{mutableStateOf(weight.toString())}
 val kcal=foods.sumOf{it.kcal}; val p=foods.sumOf{it.protein}; val c=foods.sumOf{it.carbs}; val f=foods.sumOf{it.fat}
  MaterialTheme(colorScheme=if(dark)darkColorScheme(primary=Color(0xFFFFA36F),secondary=Mint,background=Color(0xFF191817),surface=Color(0xFF292623),onSurface=Color(0xFFFFF8F1),onSurfaceVariant=Color(0xFFD0C5BC),primaryContainer=Color(0xFF5A2D1B),onPrimaryContainer=Color(0xFFFFE0CA))else lightColorScheme(primary=Orange,secondary=Mint,background=Cream,surface=Color.White,onSurface=Ink,onSurfaceVariant=Muted,primaryContainer=Lavender,onPrimaryContainer=Ink)){
  if(splash){ LeanoraSplash() } else if(welcome){ LeanoraWelcome{welcome=false;prefs.edit().putBoolean("welcome_seen",true).apply()} } else if(!onboarded){ ProfileSetup(profileName,profileAge,profileSex,profileHeight,weight,target,activityLevel,fitnessGoal,{name,age,sex,height,w,t,activity,goalName-> profileName=name;profileAge=age;profileSex=sex;profileHeight=height;weight=w;target=t;activityLevel=activity;fitnessGoal=goalName;goal=calorieTarget(age,sex,height,w,activity,goalName);if(prefs.getString("first_use_date",null)==null){firstUseDate=LocalDate.now();prefs.edit().putString("first_use_date",firstUseDate.toString()).apply()};prefs.edit().putBoolean("onboarded",true).putString("profile_name",name).putInt("profile_age",age).putString("profile_sex",sex).putInt("profile_height",height).putFloat("weight",w).putFloat("target",t).putString("activity",activity).putString("fitness_goal",goalName).putInt("goal",goal).apply();onboarded=true }) } else { Scaffold(containerColor=if(dark)Color(0xFF191817)else Cream,bottomBar={NavigationBar(containerColor=if(dark)Color(0xFF211F1D)else Color.White){NavigationBarItem(tab==0,{tab=0},icon={Icon(Icons.Default.Home,null)},label={Text("Today")});NavigationBarItem(tab==1,{tab=1},icon={Icon(Icons.Default.Restaurant,null)},label={Text("Food")});NavigationBarItem(tab==2,{tab=2},icon={Icon(Icons.Default.ShowChart,null)},label={Text("Progress")});NavigationBarItem(tab==3,{tab=3},icon={Icon(Icons.Default.SmartToy,null)},label={Text("Coach")});NavigationBarItem(tab==4,{tab=4},icon={Icon(Icons.Default.Settings,null)},label={Text("Settings")})}},floatingActionButton={if(tab<2)FloatingActionButton(onClick={foodSearch="";showFavoritesOnly=false;picker=true},containerColor=Orange){Icon(Icons.Default.Add,"Add food",tint=Color.White)}}){pad->
  when(tab){0->Dashboard(profileName,foods,kcal,goal,p,c,f,waterMl,waterGoal,Modifier.padding(pad),{foodSearch="";showFavoritesOnly=false;picker=true},{amount->waterMl=amount;prefs.edit().putInt("water_"+diaryDate.toString(),amount).apply()});1->Diary(foods,diaryDate,Modifier.padding(pad),{date->diaryDate=date;foods=loadFoods(prefs,date)},{foodSearch="";showFavoritesOnly=false;picker=true},{i->selectedFoodIndex=i},{calendarOpen=true});2->Progress(weight,target,prefs.getString("weight_history","[]") ?: "[]",Modifier.padding(pad)){weightText=weight.toString();weightDialog=true};3->Coach(kcal,goal,p,Modifier.padding(pad),backendUrl);else->Settings(goal,weight,target,waterGoal,Modifier.padding(pad),{goalText=goal.toString();goalDialog=true},{weightText=weight.toString();weightDialog=true},{waterGoal=it;prefs.edit().putInt("water_goal",it).apply()},dark,{dark=it;prefs.edit().putBoolean("dark",it).apply()},backendUrl,{backendUrl=it;prefs.edit().putString("backend",it).apply()},{targetText=target.toString();targetDialog=true},{onboarded=false;prefs.edit().putBoolean("onboarded",false).apply()},{if(!prefs.getBoolean("sample_data_seeded",false)){val today=LocalDate.now();saveFoods(prefs,listOf(Food("Demo • Oat bowl",320,12,45,8,"Breakfast"),Food("Demo • Yogurt",140,15,12,2,"Snack")),today.minusDays(1));saveFoods(prefs,listOf(Food("Demo • Idli",130,4,26,1,"Breakfast"),Food("Demo • Paneer",265,18,4,20,"Dinner")),today.minusDays(2));val history=JSONArray(prefs.getString("weight_history","[]") ?: "[]").put(JSONObject().put("date",today.minusDays(2).toString()).put("kg",weight+0.7f)).put(JSONObject().put("date",today.minusDays(1).toString()).put("kg",weight+0.3f));firstUseDate=today.minusDays(2);prefs.edit().putBoolean("sample_data_seeded",true).putString("first_use_date",firstUseDate.toString()).putString("weight_history",history.toString()).apply();Toast.makeText(ctx,"Two days of sample diary data added",Toast.LENGTH_LONG).show()}else Toast.makeText(ctx,"Sample diary data is already loaded",Toast.LENGTH_SHORT).show()})}
 }
  if(selectedFoodIndex in foods.indices){val index=selectedFoodIndex;FoodDetailDialog(foods[index],onDismiss={selectedFoodIndex=-1},onCopy={val updated=foods.toMutableList().also{it.add(index+1,foods[index].copy())};foods=updated;saveFoods(prefs,updated,diaryDate);selectedFoodIndex=-1;Toast.makeText(ctx,"Food copied",Toast.LENGTH_SHORT).show()},onEdit={editingFood=foods[index];editingFoodIndex=index;selectedFoodIndex=-1},onDelete={deleteFoodIndex=index;selectedFoodIndex=-1})}
  if(deleteFoodIndex in foods.indices){val index=deleteFoodIndex;AlertDialog(onDismissRequest={deleteFoodIndex=-1},title={Text("Delete food entry?")},text={Text("Delete ${foods[index].name} from this day’s diary?")},confirmButton={TextButton(onClick={val updated=foods.toMutableList().also{it.removeAt(index)};foods=updated;saveFoods(prefs,updated,diaryDate);deleteFoodIndex=-1}){Text("Delete")}},dismissButton={TextButton(onClick={deleteFoodIndex=-1}){Text("Keep entry")}})}
  if(calendarOpen){val state=rememberDatePickerState(initialSelectedDateMillis=toUtcMillis(diaryDate),yearRange=firstUseDate.year..LocalDate.now().year,selectableDates=object:SelectableDates{override fun isSelectableDate(utcTimeMillis:Long):Boolean{val date=Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate();return !date.isBefore(firstUseDate)&&!date.isAfter(LocalDate.now())};override fun isSelectableYear(year:Int):Boolean=year in firstUseDate.year..LocalDate.now().year});DatePickerDialog(onDismissRequest={calendarOpen=false},confirmButton={TextButton(enabled=state.selectedDateMillis!=null,onClick={state.selectedDateMillis?.let{val date=Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate();diaryDate=date;foods=loadFoods(prefs,date)};calendarOpen=false}){Text("View day")}},dismissButton={TextButton(onClick={calendarOpen=false}){Text("Cancel")}}){DatePicker(state,showModeToggle=false,title={Text("Choose a diary date")})}}
  if(cameraMode!=null){CameraCaptureDialog(mode=cameraMode!!,onClose={cameraMode=null},onBarcode={code->cameraMode=null;barcodeText=code;barcodeLoading=true;scope.launch{val result=withContext(Dispatchers.IO){lookupFoodBarcode(code)};if(result.kcal>0){foods=foods+result;saveFoods(prefs,foods,diaryDate);Toast.makeText(ctx,"Added ${result.name}",Toast.LENGTH_LONG).show()}else Toast.makeText(ctx,"No nutrition found. Try manual entry.",Toast.LENGTH_LONG).show();barcodeLoading=false}},onPhoto={bitmap->cameraMode=null;scope.launch{val result=withContext(Dispatchers.IO){estimateFoodPhoto(backendUrl,bitmap)};if(result.kcal>0){foods=foods+result;saveFoods(prefs,foods,diaryDate);Toast.makeText(ctx,"Food estimate added: ${result.name}",Toast.LENGTH_LONG).show()}else Toast.makeText(ctx,result.name,Toast.LENGTH_LONG).show()}})}
 if (picker) {
  AlertDialog(
   onDismissRequest = { picker = false },
   title = { Text("Add food") },
   text = {
     Column(Modifier.heightIn(max=560.dp).verticalScroll(rememberScrollState())) {
     Button(onClick={customDialog=true},modifier=Modifier.fillMaxWidth()){Text("Custom food / portion")}
      OutlinedButton(onClick={picker=false;cameraMode=CameraMode.BARCODE},modifier=Modifier.fillMaxWidth()) {
      Icon(Icons.Default.QrCodeScanner, null)
      Spacer(Modifier.width(8.dp))
      Text("Scan product barcode")
     }
      OutlinedButton(onClick={barcodeDialog=true},modifier=Modifier.fillMaxWidth()){Text("Enter barcode manually")}

      Button(onClick = { picker=false;cameraMode=CameraMode.FOOD_PHOTO }, modifier = Modifier.fillMaxWidth()) { Text("Camera • estimate food") }
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
     OutlinedTextField(value=foodSearch,onValueChange={foodSearch=it},modifier=Modifier.fillMaxWidth(),singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)},placeholder={Text("Search recent and catalog foods")})
     Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selected=!showFavoritesOnly,onClick={showFavoritesOnly=false},label={Text("All / Recent")});FilterChip(selected=showFavoritesOnly,onClick={showFavoritesOnly=true},label={Text("★ Favorites")})}
     val foodOptions=(foods.asReversed()+catalog).distinctBy{it.name}.filter{it.name.contains(foodSearch,ignoreCase=true)&&(!showFavoritesOnly||it.name in favoriteFoods)}
      Column {
       foodOptions.forEach { food ->
       Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
        TextButton(onClick={portionDialogFood=food;portionText="1";picker=false;foodSearch="";showFavoritesOnly=false},modifier=Modifier.weight(1f)) {
         Column(Modifier.fillMaxWidth()) { Text(food.name,color=MaterialTheme.colorScheme.onSurface,fontWeight=FontWeight.SemiBold);Text(food.kcal.toString()+" kcal • P"+food.protein+" C"+food.carbs+" F"+food.fat+"g",color=MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        IconButton(onClick={val updated=favoriteFoods.toMutableSet();if(food.name in updated)updated.remove(food.name)else updated.add(food.name);favoriteFoods=updated;prefs.edit().putStringSet("favorite_foods",updated).apply()}){Icon(if(food.name in favoriteFoods)Icons.Default.Star else Icons.Default.StarBorder,contentDescription=if(food.name in favoriteFoods)"Remove favorite" else "Add favorite",tint=Orange)}
       }
      }
     }
    }
   },
   confirmButton = { TextButton(onClick = { picker = false }) { Text("Close") } }
  )
 }

 if(portionDialogFood!=null)AlertDialog(onDismissRequest={portionDialogFood=null},title={Text("Choose portion")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text(portionDialogFood!!.name);Text("Enter servings (1 = listed serving). Calories and macros scale proportionally.");OutlinedTextField(value=portionText,onValueChange={portionText=it.filter{ch->ch.isDigit()||ch=='.'}.take(5)},label={Text("Servings / quantity")},singleLine=true)}},confirmButton={TextButton(onClick={val multiplier=portionText.toFloatOrNull();val base=portionDialogFood;if(multiplier!=null&&multiplier in .1f..20f&&base!=null){val added=Food(if(multiplier==1f)base.name else base.name+" × "+String.format(Locale.US,"%.1f",multiplier),(base.kcal*multiplier).toInt(),(base.protein*multiplier).toInt(),(base.carbs*multiplier).toInt(),(base.fat*multiplier).toInt(),base.meal);foods=foods+added;saveFoods(prefs,foods,diaryDate);portionDialogFood=null;Toast.makeText(ctx,"Portion added",Toast.LENGTH_SHORT).show()}}){Text("Add portion")}},dismissButton={TextButton(onClick={portionDialogFood=null}){Text("Cancel")}})
 if(barcodeDialog)AlertDialog(onDismissRequest={if(!barcodeLoading)barcodeDialog=false},title={Text("Barcode nutrition lookup")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Enter the barcode number printed below the product barcode. Nutrition data is retrieved from Open Food Facts.");OutlinedTextField(value=barcodeText,onValueChange={barcodeText=it.filter(Char::isDigit).take(20)},label={Text("Barcode number")},singleLine=true,modifier=Modifier.fillMaxWidth());if(barcodeLoading)LinearProgressIndicator(Modifier.fillMaxWidth())}},confirmButton={TextButton(enabled=barcodeText.length>=8&&!barcodeLoading,onClick={barcodeLoading=true;scope.launch{val result=withContext(Dispatchers.IO){lookupFoodBarcode(barcodeText)};if(result.kcal>0){foods=foods+result;saveFoods(prefs,foods,diaryDate);picker=false;Toast.makeText(ctx,"Added "+result.name,Toast.LENGTH_LONG).show();barcodeDialog=false;barcodeText=""}else Toast.makeText(ctx,"No nutrition data found. Try entering food manually.",Toast.LENGTH_LONG).show();barcodeLoading=false}}){Text(if(barcodeLoading)"Looking up…" else "Look up & add")}},dismissButton={TextButton(enabled=!barcodeLoading,onClick={barcodeDialog=false}){Text("Cancel")}})
 if(customDialog)AlertDialog(onDismissRequest={customDialog=false},title={Text("Custom food entry")},text={FormDialogColumn{OutlinedTextField(customName,{customName=it},label={Text("Food name")},singleLine=true,modifier=Modifier.fillMaxWidth());OutlinedTextField(customKcal,{customKcal=it.filter(Char::isDigit)},label={Text("Calories (kcal)")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth());OutlinedTextField(customProtein,{customProtein=it.filter(Char::isDigit)},label={Text("Protein (g)")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth());OutlinedTextField(customCarbs,{customCarbs=it.filter(Char::isDigit)},label={Text("Carbs (g)")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth());OutlinedTextField(customFat,{customFat=it.filter(Char::isDigit)},label={Text("Fat (g)")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth());MealSelector(customMeal){customMeal=it}}},confirmButton={TextButton(onClick={val kcalValue=customKcal.toIntOrNull();if(customName.isNotBlank()&&kcalValue!=null){foods=foods+Food(customName.trim(),kcalValue,customProtein.toIntOrNull()?:0,customCarbs.toIntOrNull()?:0,customFat.toIntOrNull()?:0,customMeal.ifBlank{"Snack"});saveFoods(prefs,foods,diaryDate);customDialog=false;picker=false;customName="";customKcal=""}}){Text("Add food")}},dismissButton={TextButton(onClick={customDialog=false}){Text("Cancel")}})
 if(editingFood!=null)EditFoodDialog(editingFood!!,{editingFood=null;editingFoodIndex=-1},{updated->val list=foods.toMutableList();if(editingFoodIndex in list.indices)list[editingFoodIndex]=updated;foods=list;saveFoods(prefs,list,diaryDate);editingFood=null;editingFoodIndex=-1})
 if(goalDialog)AlertDialog(onDismissRequest={goalDialog=false},title={Text("Daily calorie goal")},text={OutlinedTextField(goalText,{goalText=it.filter(Char::isDigit)},label={Text("kcal")})},confirmButton={TextButton(onClick={goalText.toIntOrNull()?.let{if(it in 800..6000){goal=it;prefs.edit().putInt("goal",it).apply()}};goalDialog=false}){Text("Save")}})
 if(targetDialog)AlertDialog(onDismissRequest={targetDialog=false},title={Text("Target weight")},text={OutlinedTextField(targetText,{targetText=it},label={Text("kg")})},confirmButton={TextButton(onClick={targetText.toFloatOrNull()?.let{if(it in 30f..300f){target=it;prefs.edit().putFloat("target",it).apply()}};targetDialog=false}){Text("Save")}})
 if(weightDialog)AlertDialog(onDismissRequest={weightDialog=false},title={Text("Current weight")},text={OutlinedTextField(weightText,{weightText=it},label={Text("kg")})},confirmButton={TextButton(onClick={weightText.toFloatOrNull()?.let{if(it in 30f..300f){weight=it;val history=JSONArray(prefs.getString("weight_history","[]")?: "[]");val today=LocalDate.now().toString();val existing=(0 until history.length()).firstOrNull{history.optJSONObject(it)?.optString("date")==today};if(existing!=null)history.put(existing,JSONObject().put("date",today).put("kg",it))else history.put(JSONObject().put("date",today).put("kg",it));prefs.edit().putFloat("weight",it).putString("weight_history",history.toString()).apply()}};weightDialog=false}){Text("Save")}})
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
 item{Text("LEANORA",color=Orange,fontWeight=FontWeight.Bold,letterSpacing=3.sp);Text("Let’s personalize your plan",fontSize=28.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface);Text("Tell us about yourself to estimate daily calorie needs.",color=MaterialTheme.colorScheme.onSurfaceVariant)}
 item{OutlinedTextField(name,{name=it},label={Text("Your name")},modifier=Modifier.fillMaxWidth())}
 item{OutlinedTextField(age,{age=it.filter(Char::isDigit)},label={Text("Age")},modifier=Modifier.fillMaxWidth())}
 item{Text("Sex");Row{listOf("Male","Female").forEach{FilterChip(selected=sex==it,onClick={sex=it},label={Text(it)},modifier=Modifier.padding(end=8.dp))}}}
 item{OutlinedTextField(height,{height=it.filter(Char::isDigit)},label={Text("Height (cm)")},modifier=Modifier.fillMaxWidth())}
 item{OutlinedTextField(weight,{weight=it},label={Text("Current weight (kg)")},modifier=Modifier.fillMaxWidth())}
 item{OutlinedTextField(target,{target=it},label={Text("Target weight (kg)")},modifier=Modifier.fillMaxWidth())}
 item{Text("Fitness goal",fontWeight=FontWeight.SemiBold);goals.forEach{FilterChip(selected=goal==it,onClick={goal=it},label={Text(it)},modifier=Modifier.padding(end=4.dp))}}
 item{Text("Activity level",fontWeight=FontWeight.SemiBold);activities.forEach{FilterChip(selected=activity==it,onClick={activity=it},label={Text(it)},modifier=Modifier.padding(end=4.dp))}}
 item{Button(onClick={val a=age.toIntOrNull();val h=height.toIntOrNull();val w=weight.toFloatOrNull();val t=target.toFloatOrNull();if(name.isNotBlank()&&a!=null&&a in 14..100&&h!=null&&h in 100..250&&w!=null&&w in 30f..300f&&t!=null&&t in 30f..300f)onSave(name.trim(),a,sex,h,w,t,activity,goal)},modifier=Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(16.dp)){Text("Create my nutrition plan",fontWeight=FontWeight.Bold)}}
 item{Text("Calorie estimates are a guide, not medical advice.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
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
 item{Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("TODAY • "+LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d")),fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.2.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text("Hi, "+name.ifBlank{"there"}+" 👋",fontSize=26.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface);Text("Small steps. Lasting change.",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Box(Modifier.size(42.dp).background(Lavender,RoundedCornerShape(15.dp)),contentAlignment=Alignment.Center){Icon(Icons.Default.Favorite,null,tint=Orange)}}}
 item{Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=Ink)){Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("CALORIES LEFT",fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.1.sp,color=Color.White.copy(alpha=.7f));Row(verticalAlignment=Alignment.Bottom){Text(kotlin.math.abs(remaining).toString(),fontSize=36.sp,fontWeight=FontWeight.ExtraBold,color=Color.White);Text(if(remaining>=0)" kcal left" else " kcal over",fontSize=14.sp,color=Color.White.copy(alpha=.75f),modifier=Modifier.padding(bottom=6.dp))}};Box(Modifier.size(76.dp),contentAlignment=Alignment.Center){CircularProgressIndicator(progress={progress},modifier=Modifier.fillMaxSize(),color=Orange,trackColor=Color.White.copy(alpha=.12f),strokeWidth=8.dp);Text("${(progress*100).toInt()}%",fontWeight=FontWeight.Bold,color=Color.White)}};Text("$k eaten  •  $g daily goal",fontSize=13.sp,color=Color.White.copy(alpha=.75f));LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth().height(6.dp),color=Orange,trackColor=Color.White.copy(alpha=.12f))}}}
 item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Macro("PROTEIN","${p}g",Modifier.weight(1f));Macro("CARBS","${c}g",Modifier.weight(1f));Macro("FAT","${f}g",Modifier.weight(1f))}}
 item{Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.WaterDrop,null,tint=Color(0xFF2196F3),modifier=Modifier.size(28.dp));Column(Modifier.weight(1f).padding(start=10.dp)){Text("Water intake",fontSize=18.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface);Text("${waterMl} / ${waterGoal} ml logged",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text("${(waterMl*100/waterGoal.coerceAtLeast(1)).coerceAtMost(100)}%",fontWeight=FontWeight.Bold,color=Color(0xFF2196F3))};LinearProgressIndicator(progress={(waterMl/waterGoal.coerceAtLeast(1).toFloat()).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(7.dp),color=Color(0xFF2196F3),trackColor=Color(0xFFE8F2FA));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={onWaterChange((waterMl-250).coerceAtLeast(0))},modifier=Modifier.weight(1f),enabled=waterMl>0){Text("−250 ml")};Button(onClick={onWaterChange(waterMl+250)},modifier=Modifier.weight(1f)){Text("+250 ml")};Button(onClick={onWaterChange(waterMl+500)},modifier=Modifier.weight(1f)){Text("+500 ml")}};Text("Hydration target is a customizable reminder, not medical advice.",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
 item{Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Meals & snacks",fontSize=20.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface);Text("Your day, one meal at a time",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};TextButton(onClick=add){Icon(Icons.Default.Add,null);Text("Add food")}}}
 if(food.isEmpty())item{Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){Column(Modifier.fillMaxWidth().padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){Icon(Icons.Default.Restaurant,null,tint=Orange,modifier=Modifier.size(30.dp));Text("Start with your first meal",fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface);Text("Log food to see calories and macros here.",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Button(onClick=add,shape=RoundedCornerShape(14.dp)){Text("Log food")}}}}
  food.groupBy{it.meal}.entries.sortedBy{listOf("Breakfast","Lunch","Dinner","Snack").indexOf(it.key).let{n->if(n<0)4 else n}}.forEach{(meal,mealFood)->item{Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){Column(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=10.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(meal,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface);Text("${mealFood.size} item${if(mealFood.size==1)"" else "s"}",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text("${mealFood.sumOf{it.kcal}} kcal",fontWeight=FontWeight.Bold,color=Orange);IconButton(onClick=add){Icon(Icons.Default.Add,"Add food",tint=Orange)}};mealFood.forEach{Row(Modifier.fillMaxWidth().padding(start=4.dp,top=3.dp,bottom=3.dp),verticalAlignment=Alignment.CenterVertically){Text(it.name,Modifier.weight(1f),fontSize=14.sp,color=MaterialTheme.colorScheme.onSurface);Text("${it.kcal} kcal",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}}}
 }
}
@Composable private fun Diary(food:List<Food>,date:LocalDate,m:Modifier,onDateChange:(LocalDate)->Unit,add:()->Unit,onOpenFood:(Int)->Unit,onCalendar:()->Unit){
 var query by remember{mutableStateOf("")}; var mealFilter by remember{mutableStateOf("All")}
 val filtered=food.withIndex().filter{(_,x)->(mealFilter=="All"||x.meal.equals(mealFilter,true))&&x.name.contains(query,true)}
 Column(m.fillMaxSize().padding(horizontal=18.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Food diary",fontSize=28.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface);Text("Your daily nutrition, at a glance",color=MaterialTheme.colorScheme.onSurfaceVariant)};IconButton(onClick=add){Icon(Icons.Default.Add,"Add food",tint=Orange)}}
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){IconButton(onClick={onDateChange(date.minusDays(1))}){Icon(Icons.Default.ChevronLeft,"Previous day")};Column(Modifier.weight(1f).clickable(onClick=onCalendar),horizontalAlignment=Alignment.CenterHorizontally){Text(if(date==LocalDate.now())"Today" else date.format(java.time.format.DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")),fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface);Text("${date.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))}",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};IconButton(enabled=date.isBefore(LocalDate.now()),onClick={if(date.isBefore(LocalDate.now()))onDateChange(date.plusDays(1))}){Icon(Icons.Default.ChevronRight,"Next day")};IconButton(onClick=onCalendar){Icon(Icons.Default.CalendarMonth,"Choose date",tint=Orange)}}
  Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.primaryContainer){Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){Text("${food.size} item${if(food.size==1)"" else "s"} logged",Modifier.weight(1f),fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onSurface);Text("${food.sumOf{it.kcal}} kcal",fontWeight=FontWeight.Bold,color=Orange)}}
  OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth(),singleLine=true,leadingIcon={Icon(Icons.Default.Search,"Search")},placeholder={Text("Search logged foods")},shape=RoundedCornerShape(16.dp))
  Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("All","Breakfast","Lunch","Dinner","Snack").forEach{FilterChip(selected=mealFilter==it,onClick={mealFilter=it},label={Text(it)})}}
  if(filtered.isEmpty())Box(Modifier.fillMaxWidth().weight(1f),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){Icon(Icons.Default.RestaurantMenu,null,tint=MaterialTheme.colorScheme.onSurfaceVariant);Text(if(food.isEmpty())"No foods logged yet" else "No matching foods",fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onSurface);Text("Add a meal to start tracking.",color=MaterialTheme.colorScheme.onSurfaceVariant);Button(onClick=add){Text("Add food")}}}
  else LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=18.dp)){items(filtered.size){j->val (index,item)=filtered[j];Card(modifier=Modifier.fillMaxWidth().clickable{onOpenFood(index)},shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){Row(Modifier.fillMaxWidth().heightIn(min=68.dp).padding(horizontal=15.dp,vertical=11.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){Text(item.name,fontSize=16.sp,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onSurface,maxLines=1);Text(item.meal+" • Tap for details",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=12.sp)};Text("${item.kcal} kcal",fontSize=16.sp,fontWeight=FontWeight.Bold,color=Orange);Icon(Icons.Default.ChevronRight,null,tint=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(start=4.dp))}}}}
 }
}
@Composable private fun EditFoodDialog(food:Food,onDismiss:()->Unit,onSave:(Food)->Unit){
 var name by remember{mutableStateOf(food.name)};var kcal by remember{mutableStateOf(food.kcal.toString())};var protein by remember{mutableStateOf(food.protein.toString())};var carbs by remember{mutableStateOf(food.carbs.toString())};var fat by remember{mutableStateOf(food.fat.toString())};var meal by remember{mutableStateOf(food.meal)}
 AlertDialog(onDismissRequest=onDismiss,title={Text("Edit food entry")},text={FormDialogColumn{
  OutlinedTextField(name,{name=it},label={Text("Food name")},singleLine=true,modifier=Modifier.fillMaxWidth())
  OutlinedTextField(kcal,{kcal=it.filter(Char::isDigit)},label={Text("Calories")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())
  OutlinedTextField(protein,{protein=it.filter(Char::isDigit)},label={Text("Protein (g)")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())
  OutlinedTextField(carbs,{carbs=it.filter(Char::isDigit)},label={Text("Carbs (g)")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())
  OutlinedTextField(fat,{fat=it.filter(Char::isDigit)},label={Text("Fat (g)")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())
  MealSelector(meal){meal=it}
 }},confirmButton={TextButton(onClick={val n=kcal.toIntOrNull();if(n!=null)onSave(food.copy(name=name,kcal=n,protein=protein.toIntOrNull()?:0,carbs=carbs.toIntOrNull()?:0,fat=fat.toIntOrNull()?:0,meal=meal))}){Text("Save")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}

@Composable private fun FormDialogColumn(content:@Composable ColumnScope.()->Unit){
 val maxHeight=(LocalContext.current.resources.configuration.screenHeightDp-176).coerceAtLeast(144).dp
 Column(Modifier.fillMaxWidth().heightIn(max=maxHeight).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp),content=content)
}

@Composable private fun MealSelector(meal:String,onSelect:(String)->Unit){
 var expanded by remember{mutableStateOf(false)}
 Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
  Text("Meal",fontWeight=FontWeight.Medium,color=MaterialTheme.colorScheme.onSurfaceVariant)
  Box{
   OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()){
    Text(meal,modifier=Modifier.weight(1f));Icon(Icons.Default.ArrowDropDown,"Choose meal")
   }
    DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}){
    listOf("Breakfast","Lunch","Dinner","Snack").forEach{option->DropdownMenuItem(text={Text(option)},onClick={onSelect(option);expanded=false})}
   }
  }
 }
}

@Composable private fun Progress(w:Float,t:Float,historyJson:String,m:Modifier,edit:()->Unit){
 val points=remember(historyJson){try{val arr=JSONArray(historyJson);(0 until arr.length()).map{val o=arr.getJSONObject(it);o.optDouble("kg",w.toDouble()).toFloat()}.takeLast(7)}catch(_:Exception){emptyList<Float>()}}
 val history=remember(historyJson){try{val arr=JSONArray(historyJson);(0 until arr.length()).map{val o=arr.getJSONObject(it);o.optString("date")+" • "+String.format(Locale.US,"%.1f",o.optDouble("kg"))+" kg"}.reversed().take(8)}catch(_:Exception){emptyList<String>()}}
 Column(m.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  Text("Progress",fontSize=28.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface)
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Card(Modifier.weight(1f),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){Column(Modifier.padding(14.dp)){Text("Current",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(String.format(Locale.US,"%.1f kg",w),fontSize=22.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface)}};Card(Modifier.weight(1f),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){Column(Modifier.padding(14.dp)){Text("Target",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(String.format(Locale.US,"%.1f kg",t),fontSize=22.sp,fontWeight=FontWeight.Bold,color=Orange)}}}
  Info("Remaining to target",String.format(Locale.US,"%.1f kg",kotlin.math.abs(w-t)))
  Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Weight trend",fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface);Text(if(points.size<2)"Log at least two measurements to see your trend." else "Last ${points.size} measurements",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);if(points.isNotEmpty())WeightTrendChart(points,Modifier.fillMaxWidth().height(150.dp));if(points.isNotEmpty())Text("Min ${String.format(Locale.US,"%.1f",points.minOrNull())} kg  •  Max ${String.format(Locale.US,"%.1f",points.maxOrNull())} kg",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
  Button(onClick=edit,modifier=Modifier.fillMaxWidth()){Text("Add weight measurement")}
  Info("Recent measurements",history.joinToString("\n").ifBlank{"No measurements yet. Add your first weight."})
 }
}
@Composable private fun WeightTrendChart(values:List<Float>,modifier:Modifier=Modifier){
 Canvas(modifier){if(values.isNotEmpty()){val minV=(values.minOrNull()?:0f)-1f;val maxV=(values.maxOrNull()?:1f)+1f;val range=(maxV-minV).coerceAtLeast(1f);val left=12f;val right=size.width-12f;val top=12f;val bottom=size.height-12f
 for(i in 0..3){val y=top+(bottom-top)*i/3f;drawLine(color=Color(0xFFEDE4DC),start=androidx.compose.ui.geometry.Offset(left,y),end=androidx.compose.ui.geometry.Offset(right,y),strokeWidth=1f)}
 val pts=values.mapIndexed{index,value->androidx.compose.ui.geometry.Offset(if(values.size==1)(left+right)/2 else left+(right-left)*index/(values.size-1),bottom-(value-minV)/range*(bottom-top))}
 pts.zipWithNext().forEach{(a,b)->drawLine(color=Orange,start=a,end=b,strokeWidth=4f,cap=androidx.compose.ui.graphics.StrokeCap.Round)}
 pts.forEach{drawCircle(color=Orange,radius=5f,center=it);drawCircle(color=Color.White,radius=2f,center=it)}
 }}
}
@Composable private fun Coach(k:Int,g:Int,p:Int,m:Modifier,api:String){
 val scope=rememberCoroutineScope();val context=LocalContext.current;val tts=remember{TextToSpeech(context){}};DisposableEffect(tts){onDispose{tts.shutdown()}};var question by remember{mutableStateOf("")};var answer by remember{mutableStateOf("Hi! Ask me about your food, calories, or nutrition.")};var loading by remember{mutableStateOf(false)}
 Column(m.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
 Text("Leanora Coach",fontSize=28.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface)
 Info("Today","Calories: "+k+" / "+g+" kcal • Protein: "+p+" g")
 Info("Coach",answer)
 OutlinedButton(onClick={tts.speak(answer,TextToSpeech.QUEUE_FLUSH,null,"leanora-coach")},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.VolumeUp,null);Spacer(Modifier.width(8.dp));Text("Read coach response aloud")}
 OutlinedTextField(question,{question=it},modifier=Modifier.fillMaxWidth(),label={Text("Ask a nutrition question")})
 Button(enabled=!loading&&question.isNotBlank(),onClick={loading=true;scope.launch{answer=withContext(Dispatchers.IO){askLeanora(api,question,k,g,p)};loading=false}},modifier=Modifier.fillMaxWidth()){Text(if(loading)"Thinking..." else "Ask AI Coach")}
 Text("AI estimates are educational, not medical advice.",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=12.sp)
 }
}
@Composable private fun Settings(g:Int,w:Float,t:Float,waterGoal:Int,m:Modifier,goal:()->Unit,weight:()->Unit,onWaterGoalChange:(Int)->Unit,dark:Boolean,onDark:(Boolean)->Unit,backend:String,onBackend:(String)->Unit,target:()->Unit,editProfile:()->Unit,seedDemo:()->Unit){
 val scope=rememberCoroutineScope();var apiChecking by remember{mutableStateOf(false)};var apiStatus by remember{mutableStateOf("Not tested yet")};var apiOk by remember{mutableStateOf<Boolean?>(null)};var waterGoalText by remember{mutableStateOf(waterGoal.toString())};var waterGoalDialog by remember{mutableStateOf(false)}
  Column(m.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=18.dp,vertical=16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  Text("Settings",fontSize=30.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface);Text("Personalize your Leanora experience",color=MaterialTheme.colorScheme.onSurfaceVariant)
  Card(shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Orange)){Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("YOUR PROFILE",fontSize=12.sp,letterSpacing=1.5.sp,color=Color.White.copy(alpha=.8f));Text("Nutrition & goals",fontSize=21.sp,fontWeight=FontWeight.Bold,color=Color.White);Text("Keep your details up to date",color=Color.White.copy(alpha=.85f))};Icon(Icons.Default.PersonOutline,null,tint=Color.White,modifier=Modifier.size(34.dp))}}
  SettingsSection("Profile & goals",Icons.Default.Person){SettingsAction(Icons.Default.Edit,"Edit personal profile","Name, age, activity and goal",editProfile);SettingsAction(Icons.Default.LocalFireDepartment,"Daily calorie goal","${g} kcal • Tap to adjust",goal);SettingsAction(Icons.Default.MonitorWeight,"Current weight","${w} kg • Update measurement",weight);SettingsAction(Icons.Default.Flag,"Target weight","${t} kg • Set your goal",target)}
  SettingsSection("Hydration",Icons.Default.WaterDrop){SettingsAction(Icons.Default.WaterDrop,"Daily water target","${waterGoal} ml • Tap to change",{waterGoalText=waterGoal.toString();waterGoalDialog=true})}
   SettingsSection("Appearance",Icons.Default.Palette){Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.DarkMode,null,tint=Orange);Column(Modifier.weight(1f).padding(start=12.dp)){Text("Dark mode",fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onSurface);Text("Use a darker color scheme",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=dark,onCheckedChange=onDark,modifier=Modifier.semantics{contentDescription="Dark mode switch"})}}
  SettingsSection("Connectivity",Icons.Default.Cloud){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Backend API URL",fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onSurface);OutlinedTextField(value=backend,onValueChange=onBackend,modifier=Modifier.fillMaxWidth(),placeholder={Text("https://your-service.onrender.com")},singleLine=true,shape=RoundedCornerShape(14.dp),leadingIcon={Icon(Icons.Default.Link,null)});Button(enabled=!apiChecking&&backend.isNotBlank(),onClick={apiChecking=true;apiOk=null;apiStatus="Checking server…";scope.launch{val result=withContext(Dispatchers.IO){try{val conn=(URL(backend.trim().trimEnd('/')).openConnection() as java.net.HttpURLConnection);conn.requestMethod="GET";conn.connectTimeout=10000;conn.readTimeout=10000;val code=conn.responseCode;conn.disconnect();code}catch(e:Exception){-1}};apiChecking=false;apiOk=result in 200..499;apiStatus=if(result in 200..499)"Server reachable (HTTP $result)" else "Connection failed • check URL/network"}} ,modifier=Modifier.fillMaxWidth()){Text(if(apiChecking)"Testing connection…" else "Test backend connection")};Text(apiStatus,color=when(apiOk){true->Color(0xFF248A57);false->Color(0xFFB3261E);null->Muted},fontSize=13.sp)}}
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
   if(LocalContext.current.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0)SettingsSection("Developer testing",Icons.Default.Science){Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text("Add two days of example diary and weight entries to try date navigation.",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Button(onClick=seedDemo,modifier=Modifier.fillMaxWidth()){Text("Add 2-day sample data")}}}
   SettingsSection("Privacy & app",Icons.Default.Security){Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Lock,null,tint=Orange);Column(Modifier.weight(1f).padding(start=12.dp)){Text("On-device privacy",fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onSurface);Text("Food logs and settings are stored locally on this device.",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}};Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Info,null,tint=Orange);Text("Leanora • Android",Modifier.weight(1f).padding(start=12.dp),color=MaterialTheme.colorScheme.onSurface);Text("v0.3",color=MaterialTheme.colorScheme.onSurfaceVariant)}}
 }
}
@Composable private fun SettingsSection(title:String,icon:androidx.compose.ui.graphics.vector.ImageVector,content:@Composable ColumnScope.()->Unit){Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){Column{Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=Orange);Text(title,Modifier.padding(start=10.dp),fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface)};HorizontalDivider(color=Cream);content()}}}
@Composable private fun SettingsAction(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,subtitle:String,onClick:()->Unit){TextButton(onClick=onClick,modifier=Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=Orange);Column(Modifier.weight(1f).padding(start=12.dp)){Text(title,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onSurface);Text(subtitle,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Icon(Icons.Default.ChevronRight,null,tint=Muted)}}}
@Composable private fun Macro(title:String,value:String,m:Modifier){Card(m,shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){Column(Modifier.padding(12.dp)){Text(title,color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=12.sp);Text(value,color=MaterialTheme.colorScheme.onSurface,fontWeight=FontWeight.Bold)}}}
@Composable private fun FoodCard(x:Food,m:Modifier=Modifier){Card(m,shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){Text(x.name,color=MaterialTheme.colorScheme.onSurface,fontWeight=FontWeight.SemiBold,fontSize=16.sp);Text(x.meal,color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=12.sp);Text("P ${x.protein}g  •  C ${x.carbs}g  •  F ${x.fat}g",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Column(horizontalAlignment=Alignment.End){Text(x.kcal.toString(),color=Orange,fontWeight=FontWeight.Bold,fontSize=18.sp);Text("kcal",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=11.sp)}}}}
@Composable private fun Info(title:String,body:String){Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){Text(title,color=MaterialTheme.colorScheme.onSurface,fontWeight=FontWeight.SemiBold);Text(body,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
private fun foodKey(date:LocalDate) = "foods_" + date.toString()
private fun toUtcMillis(date:LocalDate):Long=date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
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
private fun estimateFoodText(base:String,description:String):Food{
 if(base.isBlank())return Food("Voice nutrition estimate unavailable: set backend URL",0,0,0,0,"Snack")
 return try{
  val connection=(URL(base.trimEnd('/')+"/v1/food-text-estimate").openConnection() as java.net.HttpURLConnection)
  connection.requestMethod="POST";connection.connectTimeout=15000;connection.readTimeout=30000;connection.doOutput=true;connection.setRequestProperty("Content-Type","application/json")
  val payload=JSONObject().put("description",description.take(1000)).toString()
  connection.outputStream.use{it.write(payload.toByteArray(Charsets.UTF_8))}
  val code=connection.responseCode;val response=(if(code in 200..299)connection.inputStream else connection.errorStream).bufferedReader().use{it.readText()};connection.disconnect()
  if(code !in 200..299)return Food("Voice estimate failed • check backend",0,0,0,0,"Snack")
  val obj=JSONObject(response);val kcal=obj.optInt("calories_kcal",0).coerceAtLeast(0)
  if(kcal<=0)return Food("Could not estimate nutrition for: "+description.take(50),0,0,0,0,"Snack")
  Food(obj.optString("food_name",description.take(60))+" • "+obj.optString("portion","estimated portion"),kcal,obj.optInt("protein_g",0).coerceAtLeast(0),obj.optInt("carbs_g",0).coerceAtLeast(0),obj.optInt("fat_g",0).coerceAtLeast(0),obj.optString("meal","Snack").let{if(it in listOf("Breakfast","Lunch","Dinner","Snack"))it else "Snack"})
 }catch(_:Exception){Food("Voice nutrition estimate failed • check backend",0,0,0,0,"Snack")}
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
