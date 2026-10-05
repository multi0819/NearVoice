package com.multi0819.nearvoice.wear
import android.content.Intent
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit
class VoiceListener:WearableListenerService(){
 override fun onMessageReceived(event:MessageEvent){if(event.path!="/nearvoice/speak"||event.data.size>24000)return
  val s=Speech.parse(event.data.toString(Charsets.UTF_8))?:return
  val active=WatchStore.prefs(this).getBoolean("enabled",false)&&VoiceService.running
  WatchStore.record(this,s)
  val fresh=if(active)WatchStore.reserve(this,s)else true
  if(fresh)WatchNotices.show(this,s)
  var accepted=active
  if(!active){WatchStore.status(this,"메시지 수신됨 · 앱에서 음성 수신 ON을 켜세요.")}
  else if(fresh){try{startForegroundService(Intent(this,VoiceService::class.java).putExtra("speech",s.json()))}catch(_:RuntimeException){accepted=false;WatchStore.forget(this,s.key);WatchStore.status(this,"자동 재생 시작 제한 · 마지막 메시지 읽기를 누르세요.")}}
  runCatching{val data=JSONObject().put("key",s.key).put("accepted",accepted).toString().toByteArray(Charsets.UTF_8);Tasks.await(Wearable.getMessageClient(this).sendMessage(event.sourceNodeId,"/nearvoice/ack",data),5,TimeUnit.SECONDS)}
 }
}
