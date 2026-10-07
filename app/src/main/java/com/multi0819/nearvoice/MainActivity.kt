package com.multi0819.nearvoice
import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.widget.*
import java.time.*
class MainActivity:Activity(){
 private val handler=Handler(Looper.getMainLooper());private var statusView:TextView?=null;private var nextView:TextView?=null;private var warningsView:TextView?=null
 private val locationViews=mutableMapOf<String,TextView>()
 var subpage=false
 var placeCallback:((Place)->Unit)?=null;var soundCallback:((String?)->Unit)?=null
 private val update=object:Runnable{override fun run(){updateStatus();handler.postDelayed(this,2500)}}
 override fun onCreate(b:Bundle?){super.onCreate(b);Notices.channels(this);dashboard();receiveMap(intent);if(Build.VERSION.SDK_INT>=33)onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT){navigateBack()}}
 override fun onNewIntent(i:Intent){super.onNewIntent(i);setIntent(i);receiveMap(i)}
 private fun receiveMap(i:Intent){if(i.action!=Intent.ACTION_SEND||i.type!="text/plain")return
  val shared=i.getStringExtra(Intent.EXTRA_TEXT)?.take(12000)?:return
  if(MapsShare.url(shared)==null){toast("구글 지도 장소의 공유 링크를 선택하세요.");return}
  if(placeCallback==null)placeCallback={p->openEditor(null,p)}
  startActivityForResult(Intent(this,PlacePicker::class.java).putExtra("shared_map",shared),10)
  i.action=null
 }
 override fun onResume(){super.onResume();TimeScheduler.reconcile(this);if(Store.read(this).enabled&&LocationService.permitted(this)&&Store.read(this).reservations.any{it.enabled&&it.trigger!="TIME"})runCatching{LocationService.start(this)};handler.post(update);updateStatus()}
 override fun onPause(){handler.removeCallbacks(update);super.onPause()}
 fun toast(s:String){Toast.makeText(this,s,Toast.LENGTH_LONG).show()}
 fun change(action:()->Unit){try{action()}catch(e:Exception){toast(e.message?:"저장에 실패했습니다.")}}
 fun dashboard(){locationViews.clear();placeCallback=null;subpage=false;statusView=null;nextView=null;warningsView=null
  val c=Ui.page(this,"");val state=Store.read(this)
  val header=Ui.column(this).apply{background=Ui.shape(android.graphics.Color.rgb(20,38,56),android.graphics.Color.rgb(46,73,94))}
  header.addView(Ui.label(this,"NearVoice",27f,Ui.accent).apply{typeface=android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL)})
  header.addView(Ui.label(this,"도착과 시간을 기억하는 음성 알림 · v${packageManager.getPackageInfo(packageName,0).versionName}",12f,Ui.muted))
  val toggle=Ui.toggle(this,"전체 알림",state.enabled);header.addView(toggle)
  toggle.setOnCheckedChangeListener{_,on->change{if(on){if(!checkPermissions()){toggle.isChecked=false;return@change};Store.enable(this,true);if(Store.read(this).reservations.any{it.enabled&&it.trigger!="TIME"})LocationService.start(this)}else{Store.enable(this,false);LocationService.stop(this);stopService(Intent(this,PlaybackService::class.java))};updateStatus()}}
  val actions=Ui.row(this)
  Ui.weighted(actions,Ui.coloredButton(this,"+ 예약 추가",android.graphics.Color.rgb(234,199,118),android.graphics.Color.rgb(26,34,45)){openEditor(null)})
  Ui.weighted(actions,Ui.coloredButton(this,"설정",android.graphics.Color.rgb(46,78,113),android.graphics.Color.rgb(225,237,252)){settings()})
  header.addView(actions);c.addView(header);c.addView(Ui.space(this,18))
  warningsView=Ui.label(this,"",13f,android.graphics.Color.rgb(255,184,107));c.addView(warningsView)
  if(state.enabled&&!LocationService.running&&state.reservations.any{it.enabled&&it.trigger!="TIME"})c.addView(Ui.button(this,"위치 감지 다시 시작"){if(checkPermissions()){try{LocationService.start(this);updateStatus()}catch(e:RuntimeException){toast("위치 권한을 확인하세요.")}}})
  c.addView(Ui.label(this,"예약 목록 · ${state.reservations.size}개",18f))
  if(state.reservations.isEmpty())c.addView(Ui.label(this,"목적지나 시간을 등록하면 메시지를 읽어드립니다.",15f,Ui.muted))
  state.reservations.forEach{r->
   val card=Ui.column(this).apply{background=Ui.shape();minimumHeight=Ui.dp(this@MainActivity,88)}
   val row=Ui.row(this)
   val message=Ui.label(this,r.message.ifBlank{r.title},17f).apply{setLineSpacing(Ui.dp(this@MainActivity,3).toFloat(),1f);setOnClickListener{showReservationDetails(r.id)};isFocusable=true;contentDescription="${r.message.ifBlank{r.title}} · 세부조건 보기"}
   row.addView(message,LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f))
   val on=Ui.toggle(this,"",r.enabled).apply{contentDescription="${r.message.ifBlank{r.title}} 알림 켜기 또는 끄기"}
   row.addView(on);card.addView(row)
   card.setOnClickListener{showReservationDetails(r.id)}
   on.setOnCheckedChangeListener{_,v->change{val current=Store.read(this).reservations.find{it.id==r.id}?:return@change;Store.upsert(this,current.copy(enabled=v,revision=System.currentTimeMillis()));if(Store.read(this).enabled&&v&&current.trigger!="TIME"&&checkPermissions())LocationService.start(this)}}
   c.addView(card);c.addView(Ui.space(this,12))
  };updateStatus()
 }
 private fun showReservationDetails(id:String){
  val r=Store.read(this).reservations.find{it.id==id}?:return
  val mode=when(r.trigger){"TIME"->"시간";"LOCATION"->"위치";else->"위치 + 시간"}
  val days=listOf("월","화","수","목","금","토","일")
  val repeat=when(r.repeat){"DAILY"->"매일";"WEEKDAYS"->r.weekdays.sorted().mapNotNull{days.getOrNull(it-1)}.joinToString(" · ");else->r.dates.sorted().joinToString(", ")}
  val details=buildList{
   add("알림 조건  ·  $mode");add("날짜 / 반복  ·  $repeat")
   if(r.trigger!="LOCATION")add("시간  ·  ${r.time}")
   if(r.trigger!="TIME")r.place?.let{add("목적지  ·  ${it.label}");add("알림 반경  ·  ${r.radius.toInt()}m")}
   add("음성 읽기  ·  ${if(r.voice)"ON"else"OFF"}   /   진동  ·  ${if(r.vibration)"ON"else"OFF"}")
   add("알림음  ·  ${if(r.soundUri==null)"없음"else"선택됨"}")
  }.joinToString("\n\n")
  val content=Ui.column(this);content.addView(Ui.label(this,details,14f,Ui.text))
  val scroll=ScrollView(this).apply{addView(content);isFillViewport=false}
  val dialog=AlertDialog.Builder(this).setTitle(r.message.ifBlank{r.title}).setView(scroll).setPositiveButton("닫기",null).create()
  val actions=Ui.row(this)
  Ui.weighted(actions,Ui.button(this,"수정"){dialog.dismiss();openEditor(r)})
  Ui.weighted(actions,Ui.button(this,"테스트"){PlaybackService.test(this,r)})
  Ui.weighted(actions,Ui.button(this,"삭제"){dialog.dismiss();AlertDialog.Builder(this).setTitle("예약 삭제").setMessage("${r.message.ifBlank{r.title}} 예약을 삭제할까요?").setPositiveButton("삭제"){_,_->change{Store.delete(this,r.id);dashboard()}}.setNegativeButton("취소",null).show()})
  content.addView(Ui.space(this,12));content.addView(actions)
  dialog.setOnShowListener{dialog.window?.setBackgroundDrawable(Ui.shape());dialog.window?.setLayout((resources.displayMetrics.widthPixels*.92).toInt(),LinearLayout.LayoutParams.WRAP_CONTENT);dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Ui.accent)}
  dialog.show()
 }
 private fun updateStatus(){
  val warning=Rules.healthWarnings(getSystemService(NotificationManager::class.java).areNotificationsEnabled(),getSystemService(android.media.AudioManager::class.java).getStreamVolume(android.media.AudioManager.STREAM_ALARM),Store.prefs(this).getString("error",null)).joinToString("\n")
  warningsView?.apply{text=warning;visibility=if(warning.isBlank())android.view.View.GONE else android.view.View.VISIBLE}
 }
 fun checkPermissions(requireLocation:Boolean=Store.read(this).reservations.any{it.enabled&&it.trigger!="TIME"}):Boolean {val needed=mutableListOf<String>();if(requireLocation&&!LocationService.permitted(this))needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)needed.add(Manifest.permission.POST_NOTIFICATIONS)
  if(needed.isNotEmpty()){if(Manifest.permission.ACCESS_FINE_LOCATION in needed)needed.add(Manifest.permission.ACCESS_COARSE_LOCATION);requestPermissions(needed.toTypedArray(),1);toast("권한을 허용한 뒤 전체 알림을 켜주세요.");return false}
  if(!TimeScheduler.exactAllowed(this)){startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:$packageName")));toast("정확한 알람을 허용한 뒤 다시 켜주세요.");return false};return true
 }
 fun settings(){subpage=true;statusView=null;nextView=null;warningsView=null;val c=Ui.page(this,"설정");c.addView(Ui.button(this,"‹ 예약 목록"){dashboard()});val s=Store.read(this)
  for(home in listOf(true,false)){val p=if(home)s.home else s.work;val title=if(home)"집"else"회사";c.addView(Ui.label(this,"$title · ${p?.label?:"주소 미등록"}",17f));c.addView(Ui.button(this,"$title 주소 등록 / 수정"){pickPlace(p){picked->change{Store.setPlace(this,home,picked);settings()}}})}
  c.addView(Ui.button(this,"위치 감지 시간대 · ${if(ScanSettings.restricted(this))"제한 ON"else"제한 OFF"}"){scanWindowSettings()})
  val watch=Ui.toggle(this,"워치 모드 · 화면 OFF 시 알림 전달",Store.prefs(this).getBoolean("watch_mode",false));c.addView(watch)
  watch.setOnCheckedChangeListener{_,on->Store.prefs(this).edit().putBoolean("watch_mode",on).apply()}
  c.addView(Ui.label(this,"화면이 꺼져 있으면 휴대폰 직접 재생을 멈추고 워치 앱으로 메시지를 보냅니다. 워치용 NearVoice를 설치하고 음성 수신 ON을 켜세요. 연결이 없으면 문자 알림으로 전달합니다. Galaxy Wearable에서 NearVoice 알림과 휴대전화 알림 끄기를 켜세요. 워치가 연결되지 않아도 화면 OFF에서는 음성을 읽지 않습니다.",13f,Ui.muted))
  c.addView(Ui.label(this,"권한과 알림",18f));c.addView(Ui.label(this,"위치: ${if(LocationService.permitted(this))"허용"else"필요"}\n정확한 알람: ${if(TimeScheduler.exactAllowed(this))"허용"else"필요"}",14f,Ui.muted))
  c.addView(Ui.button(this,"필요한 권한 허용"){checkPermissions()});c.addView(Ui.button(this,"앱 권한 / 배터리 설정"){startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:$packageName")))})
  c.addView(Ui.button(this,"한국어 음성 테스트"){PlaybackService.test(this,Reservation(title="음성 테스트",message="니어보이스 음성 알림이 정상적으로 작동합니다."))})
  c.addView(Ui.button(this,"휴대폰 음성 합성 설정"){try{startActivity(Intent("com.android.settings.TTS_SETTINGS"))}catch(_:Exception){startActivity(Intent(Settings.ACTION_SETTINGS))}})
  c.addView(Ui.label(this,Store.prefs(this).getString("watch_status","워치 음성 전송 이력 없음")?:"",13f,Ui.accent))
  val error=Store.prefs(this).getString("error",null);warningsView=Ui.label(this,error?:"",14f,Ui.muted);c.addView(warningsView);if(error!=null){c.addView(Ui.button(this,"안내 지우기"){Store.prefs(this).edit().remove("error").apply();settings()})}
  c.addView(Ui.label(this,"화면이 꺼져 있어도 위치 감지 중 알림이 표시됩니다. 강제 종료 후에는 앱을 다시 여세요. 재부팅 후 위치 감지는 다시 켜주세요. 무음·방해금지 및 알림 볼륨은 휴대폰 설정을 따릅니다.",13f,Ui.muted))
  c.addView(Ui.label(this,"지도와 주소 검색 시 네트워크를 사용합니다. 메시지와 예약은 이 기기에 저장됩니다. 지도: © OpenStreetMap contributors / Leaflet.",12f,Ui.muted))
 }
 fun pickPlace(initial:Place?,openGoogle:Boolean=false,callback:(Place)->Unit){placeCallback=callback;startActivityForResult(Intent(this,PlacePicker::class.java).putExtra("place",Codec.place(initial)?.toString()).putExtra("open_google",openGoogle),10)}
 @Deprecated("Legacy result API") override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data)
  if(resultCode!=RESULT_OK)return
  if(requestCode==10){runCatching{Codec.place(org.json.JSONObject(data?.getStringExtra("place")?:""))}.getOrNull()?.let{val callback=placeCallback;placeCallback=null;callback?.invoke(it)}}
  if(requestCode==11){val uri=if(Build.VERSION.SDK_INT>=33)data?.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI,Uri::class.java)else @Suppress("DEPRECATION") data?.getParcelableExtra<Uri>(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI);soundCallback?.invoke(uri?.toString())}
 }
 private fun navigateBack(){if(subpage)dashboard()else finish()}
 @Deprecated("Legacy back API") override fun onBackPressed(){navigateBack()}
}
