package com.multi0819.nearvoice.wear
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.*
import android.widget.*
class WatchActivity:Activity(){
 private val handler=Handler(Looper.getMainLooper());private lateinit var status:TextView
 private val refresh=object:Runnable{override fun run(){status.text="${if(VoiceService.running)"● 음성 수신 ON"else"○ 음성 수신 OFF"}\n${WatchStore.prefs(this@WatchActivity).getString("status","한국어 음성 테스트 후 수신을 켜세요.")}";handler.postDelayed(this,2000)}}
 override fun onCreate(b:Bundle?){super.onCreate(b)
  val scroll=ScrollView(this);val page=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(28,38,28,38);setBackgroundColor(Color.rgb(10,20,30))};scroll.addView(page);setContentView(scroll)
  fun label(s:String)=TextView(this).apply{text=s;setTextColor(Color.WHITE);textSize=15f;setPadding(0,8,0,8);page.addView(this)}
  fun button(s:String,f:()->Unit){page.addView(Button(this).apply{text=s;setOnClickListener{f()}})}
  label("NearVoice 워치 · 1.0.4");status=label("")
  button("음성 수신 ON"){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),1);return@button};startReceiver(null)}
  button("수신 OFF / 읽기 중지"){WatchStore.prefs(this).edit().putBoolean("enabled",false).apply();stopService(Intent(this,VoiceService::class.java));WatchStore.status(this,"음성 수신을 껐습니다.")}
  button("한국어 음성 테스트"){startReceiver(Speech("test:${System.currentTimeMillis()}","음성 테스트","워치 음성 알림이 정상적으로 작동합니다."))}
  button("마지막 메시지 읽기"){WatchStore.last(this)?.let{startReceiver(it.copy(key="manual:${System.currentTimeMillis()}"))}?:Toast.makeText(this,"수신한 메시지가 없습니다.",Toast.LENGTH_SHORT).show()}
  label("휴대폰의 워치 모드를 켜세요. 음성 수신 ON 상태에서 화면을 꺼도 메시지를 기다립니다. 워치 미디어 음량과 한국어 음성 데이터를 확인하세요.")
  button("음성 합성 설정"){try{startActivity(Intent("com.android.settings.TTS_SETTINGS"))}catch(_:RuntimeException){Toast.makeText(this,"워치 설정에서 음성 합성을 확인하세요.",Toast.LENGTH_LONG).show()}}
 }
 private fun startReceiver(s:Speech?){try{WatchStore.prefs(this).edit().putBoolean("enabled",true).apply();startForegroundService(Intent(this,VoiceService::class.java).apply{if(s!=null)putExtra("speech",s.json())})}catch(e:RuntimeException){WatchStore.status(this,"수신 시작 실패 · 앱을 다시 열어주세요.")}}
 override fun onResume(){super.onResume();handler.post(refresh)}
 override fun onPause(){handler.removeCallbacks(refresh);super.onPause()}
}
