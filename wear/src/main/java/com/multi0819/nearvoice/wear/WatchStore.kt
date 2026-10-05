package com.multi0819.nearvoice.wear
import android.content.Context
import org.json.JSONObject
data class Speech(val key:String,val title:String,val message:String,val voice:Boolean=true,val vibration:Boolean=true,val sentAt:Long=System.currentTimeMillis()) {
 fun json()=JSONObject().put("key",key).put("title",title).put("message",message).put("voice",voice).put("vibration",vibration).put("sentAt",sentAt).toString()
 companion object {fun parse(raw:String):Speech?=runCatching{val j=JSONObject(raw);Speech(j.getString("key"),j.optString("title","예약"),j.getString("message"),j.optBoolean("voice",true),j.optBoolean("vibration",true),j.getLong("sentAt"))}.getOrNull()?.takeIf{it.key.length in 1..250&&SpeechPolicy.accepts(it.sentAt,System.currentTimeMillis(),it.message)}}
}
object WatchStore {
 fun prefs(c:Context)=c.getSharedPreferences("voice",Context.MODE_PRIVATE)
 fun status(c:Context,s:String){prefs(c).edit().putString("status",s).apply()}
 fun last(c:Context)=prefs(c).getString("last",null)?.let{runCatching{val j=JSONObject(it);Speech(j.getString("key"),j.optString("title"),j.getString("message"),j.optBoolean("voice",true),j.optBoolean("vibration",true))}.getOrNull()}
 fun record(c:Context,s:Speech){prefs(c).edit().putString("last",s.json()).apply()}
 @Synchronized fun forget(c:Context,key:String){val p=prefs(c);p.edit().putString("seen",p.getString("seen","")!!.split('\n').filter{it!=key}.joinToString("\n")).commit()}
 @Synchronized fun reserve(c:Context,s:Speech):Boolean {
  val p=prefs(c);val keys=p.getString("seen","")!!.split('\n').filter{it.isNotBlank()};if(s.key in keys)return false
  return p.edit().putString("seen",(keys+s.key).takeLast(100).joinToString("\n")).putString("last",s.json()).commit()
 }
}
