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
 var subpage=false
 var placeCallback:((Place)->Unit)?=null;var soundCallback:((String?)->Unit)?=null
 private val update=object:Runnable{override fun run(){updateStatus();handler.postDelayed(this,2500)}}
 override fun onCreate(b:Bundle?){super.onCreate(b);Notices.channels(this);dashboard();if(Build.VERSION.SDK_INT>=33)onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT){navigateBack()}}
 override fun onResume(){super.onResume();TimeScheduler.reconcile(this);handler.post(update);updateStatus()}
 override fun onPause(){handler.removeCallbacks(update);super.onPause()}
 fun toast(s:String){Toast.makeText(this,s,Toast.LENGTH_LONG).show()}
 fun change(action:()->Unit){try{action()}catch(e:Exception){toast(e.message?:"저장에 실패했습니다.")}}
 fun dashboard(){subpage=false;statusView=null;nextView=null;val c=Ui.page(this,"NearVoice");c.addView(Ui.label(this,"PROXIMITY / SCHEDULE",12f,Ui.muted))
  val state=Store.read(this);val toggle=Ui.toggle(this,"전체 알림 ON / OFF",state.enabled);c.addView(toggle)
  toggle.setOnCheckedChangeListener{_,on->change{if(on){if(!checkPermissions()){toggle.isChecked=false;return@change};Store.enable(this,true);if(Store.read(this).reservations.any{it.enabled&&it.trigger!="TIME"})LocationService.start(this)}else{Store.enable(this,false);LocationService.stop(this);stopService(Intent(this,PlaybackService::class.java))};updateStatus()}}
  statusView=Ui.label(this,"",14f,Ui.accent);c.addView(statusView);nextView=Ui.label(this,"",14f,Ui.muted);c.addView(nextView);warningsView=Ui.label(this,"",13f,android.graphics.Color.rgb(255,184,107));c.addView(warningsView)
  val actions=Ui.row(this);Ui.weighted(actions,Ui.button(this,"+ 예약 추가"){openEditor(null)});Ui.weighted(actions,Ui.button(this,"설정 / 집·회사"){settings()});c.addView(actions)
  if(state.enabled&&!LocationService.running&&state.reservations.any{it.enabled&&it.trigger!="TIME"})c.addView(Ui.button(this,"위치 감지 다시 시작"){if(checkPermissions()){try{LocationService.start(this);updateStatus()}catch(e:RuntimeException){toast("위치 권한을 확인하세요.")}}})
  c.addView(Ui.space(this));c.addView(Ui.label(this,"예약 목록 · ${state.reservations.size}개",18f))
  if(state.reservations.isEmpty())c.addView(Ui.label(this,"목적지나 시간을 등록하면 메시지를 읽어드립니다.",15f,Ui.muted))
  state.reservations.forEach{r->val card=Ui.column(this);card.background=Ui.shape();val on=Ui.toggle(this,r.title,r.enabled);card.addView(on)
   on.setOnCheckedChangeListener{_,v->change{Store.upsert(this,r.copy(enabled=v,revision=System.currentTimeMillis()));if(Store.read(this).enabled&&v&&r.trigger!="TIME"&&checkPermissions())LocationService.start(this)}}
   card.addView(Ui.label(this,r.message,17f));val mode=when(r.trigger){"TIME"->"시간";"LOCATION"->"위치";else->"위치 + 시간"}
   val repeat=when(r.repeat){"DAILY"->"매일";"WEEKDAYS"->"요일 ${r.weekdays.sorted().joinToString(",")}";else->"${r.dates.size}개 날짜"}
   card.addView(Ui.label(this,"$mode · $repeat · ${r.time}\n${r.place?.label?:"시간 예약"}${if(r.place!=null)" · ${r.radius.toInt()}m" else ""}",13f,Ui.muted))
   val completed=r.completed.size;if(completed>0)card.addView(Ui.label(this,"실행 기록 ${completed}건",12f,Ui.muted))
   val buttons=Ui.row(this);Ui.weighted(buttons,Ui.button(this,"수정"){openEditor(r)});Ui.weighted(buttons,Ui.button(this,"테스트"){PlaybackService.test(this,r)})
   Ui.weighted(buttons,Ui.button(this,"삭제"){AlertDialog.Builder(this).setTitle("예약 삭제").setMessage("${r.title} 예약을 삭제할까요?").setPositiveButton("삭제"){_,_->change{Store.delete(this,r.id);dashboard()}}.setNegativeButton("취소",null).show()});card.addView(buttons);c.addView(card);c.addView(Ui.space(this))
  };updateStatus()
 }
 private fun updateStatus(){val s=Store.read(this);val l=LocationService.latest
  statusView?.text=if(!s.enabled)"● 전체 알림 OFF" else "● ${if(LocationService.running)LocationService.status else "위치 감지 대기 / 시간 예약 ON"}${if(l!=null)"\n%.5f, %.5f".format(l.latitude,l.longitude)else ""}"
  warningsView?.text=Rules.healthWarnings(getSystemService(NotificationManager::class.java).areNotificationsEnabled(),getSystemService(android.media.AudioManager::class.java).getStreamVolume(android.media.AudioManager.STREAM_ALARM),Store.prefs(this).getString("error",null)).joinToString("\n")
  val next=s.reservations.mapNotNull{r->Rules.nextTime(r,ZonedDateTime.now())?.let{r.title to it}}.minByOrNull{it.second.toInstant()}
  nextView?.text=if(!s.enabled)"예약을 켜면 감지를 시작합니다."else if(!TimeScheduler.exactAllowed(this))"정확한 알람 권한 필요 · 설정에서 허용" else next?.let{"다음 시간 알림 · ${it.first}\n${it.second.toLocalDate()} ${it.second.toLocalTime()}"}?:"다음 시간 알림 없음"
 }
 fun checkPermissions(requireLocation:Boolean=Store.read(this).reservations.any{it.enabled&&it.trigger!="TIME"}):Boolean {val needed=mutableListOf<String>();if(requireLocation&&!LocationService.permitted(this))needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)needed.add(Manifest.permission.POST_NOTIFICATIONS)
  if(needed.isNotEmpty()){if(Manifest.permission.ACCESS_FINE_LOCATION in needed)needed.add(Manifest.permission.ACCESS_COARSE_LOCATION);requestPermissions(needed.toTypedArray(),1);toast("권한을 허용한 뒤 전체 알림을 켜주세요.");return false}
  if(!TimeScheduler.exactAllowed(this)){startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:$packageName")));toast("정확한 알람을 허용한 뒤 다시 켜주세요.");return false};return true
 }
 fun settings(){subpage=true;statusView=null;nextView=null;warningsView=null;val c=Ui.page(this,"설정");c.addView(Ui.button(this,"‹ 예약 목록"){dashboard()});val s=Store.read(this)
  for(home in listOf(true,false)){val p=if(home)s.home else s.work;val title=if(home)"집"else"회사";c.addView(Ui.label(this,"$title · ${p?.label?:"주소 미등록"}",17f));c.addView(Ui.button(this,"$title 주소 등록 / 수정"){pickPlace(p){picked->change{Store.setPlace(this,home,picked);settings()}}})}
  c.addView(Ui.label(this,"권한과 알림",18f));c.addView(Ui.label(this,"위치: ${if(LocationService.permitted(this))"허용"else"필요"}\n정확한 알람: ${if(TimeScheduler.exactAllowed(this))"허용"else"필요"}",14f,Ui.muted))
  c.addView(Ui.button(this,"필요한 권한 허용"){checkPermissions()});c.addView(Ui.button(this,"앱 권한 / 배터리 설정"){startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:$packageName")))})
  c.addView(Ui.button(this,"한국어 음성 테스트"){PlaybackService.test(this,Reservation(title="음성 테스트",message="니어보이스 음성 알림이 정상적으로 작동합니다."))})
  c.addView(Ui.button(this,"휴대폰 음성 합성 설정"){try{startActivity(Intent("com.android.settings.TTS_SETTINGS"))}catch(_:Exception){startActivity(Intent(Settings.ACTION_SETTINGS))}})
  val error=Store.prefs(this).getString("error",null);warningsView=Ui.label(this,error?:"",14f,Ui.muted);c.addView(warningsView);if(error!=null){c.addView(Ui.button(this,"안내 지우기"){Store.prefs(this).edit().remove("error").apply();settings()})}
  c.addView(Ui.label(this,"화면이 꺼져 있어도 위치 감지 중 알림이 표시됩니다. 강제 종료 후에는 앱을 다시 여세요. 재부팅 후 위치 감지는 다시 켜주세요. 무음·방해금지 및 알림 볼륨은 휴대폰 설정을 따릅니다.",13f,Ui.muted))
  c.addView(Ui.label(this,"지도와 주소 검색 시 네트워크를 사용합니다. 메시지와 예약은 이 기기에 저장됩니다. 지도: © OpenStreetMap contributors / Leaflet.",12f,Ui.muted))
 }
 fun pickPlace(initial:Place?,callback:(Place)->Unit){placeCallback=callback;startActivityForResult(Intent(this,PlacePicker::class.java).putExtra("place",Codec.place(initial)?.toString()),10)}
 @Deprecated("Legacy result API") override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data)
  if(resultCode!=RESULT_OK)return
  if(requestCode==10){runCatching{Codec.place(org.json.JSONObject(data?.getStringExtra("place")?:""))}.getOrNull()?.let{placeCallback?.invoke(it)}}
  if(requestCode==11){val uri=if(Build.VERSION.SDK_INT>=33)data?.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI,Uri::class.java)else @Suppress("DEPRECATION") data?.getParcelableExtra<Uri>(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI);soundCallback?.invoke(uri?.toString())}
 }
 private fun navigateBack(){if(subpage)dashboard()else finish()}
 @Deprecated("Legacy back API") override fun onBackPressed(){navigateBack()}
}
