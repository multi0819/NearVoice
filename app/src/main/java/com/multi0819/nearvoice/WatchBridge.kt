package com.multi0819.nearvoice
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
object WatchBridge {
 private val worker=Executors.newSingleThreadExecutor()
 fun send(c:Context,r:Reservation,key:String,done:(Boolean)->Unit){val app=c.applicationContext
  worker.execute{
   val success=runCatching{
    val nodes=Tasks.await(Wearable.getCapabilityClient(app).getCapability("nearvoice_voice_v1",CapabilityClient.FILTER_REACHABLE),10,TimeUnit.SECONDS).nodes
    val node=nodes.sortedWith(compareByDescending<com.google.android.gms.wearable.Node>{it.isNearby}.thenBy{it.id}).firstOrNull()?:error("워치 앱 연결 없음")
    val payload=JSONObject().put("key",key).put("title",r.title).put("message",r.message).put("voice",r.voice).put("vibration",r.vibration).put("sentAt",System.currentTimeMillis()).toString().toByteArray(Charsets.UTF_8)
    Tasks.await(Wearable.getMessageClient(app).sendMessage(node.id,"/nearvoice/speak",payload),5,TimeUnit.SECONDS)
    true
   }.getOrDefault(false)
   app.getSharedPreferences("nearvoice",Context.MODE_PRIVATE).edit().putString("watch_status",if(success)"워치 앱으로 전송됨 · 실제 재생은 워치에서 확인"else"워치 앱 연결 없음 · 문자 알림으로 전달").apply()
   Handler(Looper.getMainLooper()).post{done(success)}
  }
 }
}
