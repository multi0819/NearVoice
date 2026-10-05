package com.multi0819.nearvoice.wear
import android.content.Intent
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
class VoiceListener:WearableListenerService(){
 override fun onMessageReceived(event:MessageEvent){if(event.path!="/nearvoice/speak"||event.data.size>24000)return
  val s=Speech.parse(event.data.toString(Charsets.UTF_8))?:return
  if(!WatchStore.reserve(this,s))return
  WatchNotices.show(this,s)
  if(!WatchStore.prefs(this).getBoolean("enabled",false)){WatchStore.status(this,"메시지 수신됨 · 음성 수신 ON을 켜세요.");return}
  try{startForegroundService(Intent(this,VoiceService::class.java).putExtra("speech",s.json()))}
  catch(_:RuntimeException){WatchStore.status(this,"자동 재생 시작 제한 · 앱에서 마지막 메시지 읽기를 누르세요.")}
 }
}
