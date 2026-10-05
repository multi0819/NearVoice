package com.multi0819.nearvoice
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONObject
class WatchAckListener:WearableListenerService(){
 override fun onMessageReceived(event:MessageEvent){if(event.path!="/nearvoice/ack"||event.data.size>2000)return
  runCatching{val j=JSONObject(event.data.toString(Charsets.UTF_8));WatchBridge.acknowledge(event.sourceNodeId,j.getString("key"),j.optBoolean("accepted",false))}
 }
}
