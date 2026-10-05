package com.multi0819.nearvoice
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject
import java.util.concurrent.*
object WatchBridge {
 private val worker=Executors.newSingleThreadExecutor()
 private val replies=ConcurrentHashMap<String,Pair<String,CompletableFuture<Boolean>>>()
 fun acknowledge(node:String,key:String,accepted:Boolean){replies[key]?.takeIf{it.first==node}?.second?.complete(accepted)}
 fun send(c:Context,r:Reservation,key:String,done:(Boolean)->Unit){val app=c.applicationContext
  worker.execute{
   val success=runCatching{
    val nodes=Tasks.await(Wearable.getCapabilityClient(app).getCapability("nearvoice_voice_v1",CapabilityClient.FILTER_REACHABLE),10,TimeUnit.SECONDS).nodes
    val node=nodes.sortedWith(compareByDescending<com.google.android.gms.wearable.Node>{it.isNearby}.thenBy{it.id}).firstOrNull()?:error("워치 앱 연결 없음")
    val state=Store.read(app);check(state.enabled&&state.reservations.any{it.id==r.id&&it.enabled&&it.revision==r.revision}){"예약이 취소되거나 수정됨"}
    val reply=CompletableFuture<Boolean>();replies[key]=node.id to reply
    val payload=JSONObject().put("key",key).put("title",r.title).put("message",r.message).put("voice",r.voice).put("vibration",r.vibration).put("sentAt",System.currentTimeMillis()).toString().toByteArray(Charsets.UTF_8)
    Tasks.await(Wearable.getMessageClient(app).sendMessage(node.id,"/nearvoice/speak",payload),5,TimeUnit.SECONDS)
    reply.get(8,TimeUnit.SECONDS)
   }.getOrDefault(false)
   replies.remove(key)
   app.getSharedPreferences("nearvoice",Context.MODE_PRIVATE).edit().putString("watch_status",if(success)"워치 수신 확인됨 · 재생 결과는 워치에서 확인"else"워치 수신 응답 없음 · 문자 알림으로 전달").apply()
   Handler(Looper.getMainLooper()).post{done(success)}
  }
 }
}
