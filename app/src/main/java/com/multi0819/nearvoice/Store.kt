package com.multi0819.nearvoice
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AppState(val enabled:Boolean=false,val reservations:List<Reservation> = emptyList(),val home:Place?=null,val work:Place?=null)
object Codec {
 fun place(p:Place?) = p?.let{JSONObject().put("label",it.label).put("lat",it.latitude).put("lon",it.longitude)}
 fun place(j:JSONObject?) = j?.let{Place(it.optString("label"),it.optDouble("lat"),it.optDouble("lon"))}?.takeIf{Rules.validPlace(it)}
 fun strings(a:JSONArray?):Set<String> = if(a==null)emptySet() else (0 until a.length()).map{a.getString(it)}.toSet()
 fun json(r:Reservation):JSONObject = JSONObject().put("id",r.id).put("title",r.title).put("message",r.message).put("enabled",r.enabled)
  .put("trigger",r.trigger).put("place",place(r.place)).put("radius",r.radius).put("dates",JSONArray(r.dates.toList())).put("repeat",r.repeat)
  .put("weekdays",JSONArray(r.weekdays.toList())).put("time",r.time).put("times",JSONArray(r.times.toList())).put("voice",r.voice).put("vibration",r.vibration)
  .put("soundUri",r.soundUri).put("completed",JSONArray(r.completed.toList())).put("revision",r.revision)
 fun reservation(j:JSONObject) = Reservation(id=j.getString("id"),title=j.optString("title","예약"),message=j.optString("message"),
  enabled=j.optBoolean("enabled",true),trigger=j.optString("trigger","TIME"),place=place(j.optJSONObject("place")),radius=j.optDouble("radius",300.0),
  dates=strings(j.optJSONArray("dates")),repeat=j.optString("repeat","DATES"),weekdays=stringsAsInts(j.optJSONArray("weekdays")),
  time=j.optString("time","09:00"),times=strings(j.optJSONArray("times")),voice=j.optBoolean("voice",true),vibration=j.optBoolean("vibration",true),
  soundUri=if(j.isNull("soundUri"))null else j.getString("soundUri"),completed=strings(j.optJSONArray("completed")).toMutableSet(),revision=j.optLong("revision",0))
 private fun stringsAsInts(a:JSONArray?)=if(a==null)emptySet() else (0 until a.length()).map{a.getInt(it)}.toSet()
}
object Store {
 fun prefs(c:Context)=c.getSharedPreferences("nearvoice",Context.MODE_PRIVATE)
 @Synchronized fun read(c:Context):AppState {
  val raw=prefs(c).getString("state",null)?:return AppState()
  return try { val j=JSONObject(raw);val a=j.optJSONArray("reservations")?:JSONArray()
   AppState(j.optBoolean("enabled"), (0 until a.length()).map{Codec.reservation(a.getJSONObject(it))},Codec.place(j.optJSONObject("home")),Codec.place(j.optJSONObject("work")))
  } catch(e:Exception){prefs(c).edit().putString("error","저장 데이터를 읽지 못했습니다. 기존 데이터를 보존했습니다.").apply();AppState()}
 }
 @Synchronized fun write(c:Context,s:AppState) {
  val j=JSONObject().put("enabled",s.enabled).put("reservations",JSONArray(s.reservations.map{Codec.json(it)})).put("home",Codec.place(s.home)).put("work",Codec.place(s.work))
  check(prefs(c).edit().putString("state",j.toString()).commit()){ "설정을 저장할 공간이 없습니다." }
 }
 @Synchronized fun upsert(c:Context,r:Reservation,rearm:Boolean=false){ val s=read(c);write(c,s.copy(reservations=s.reservations.filter{it.id!=r.id}+Rules.mergeSaved(r,s.reservations.find{it.id==r.id},rearm)));TimeScheduler.reconcile(c) }
 @Synchronized fun delete(c:Context,id:String){val s=read(c);write(c,s.copy(reservations=s.reservations.filter{it.id!=id}));TimeScheduler.reconcile(c)}
 @Synchronized fun enable(c:Context,on:Boolean){write(c,read(c).copy(enabled=on));TimeScheduler.reconcile(c)}
 @Synchronized fun complete(c:Context,id:String,condition:String,date:String){val s=read(c);val r=s.reservations.find{it.id==id}?:return
  if(condition=="TIME"||r.repeat in setOf("DATES","ONCE"))r.completed.add("$condition:$date")
  write(c,s);TimeScheduler.reconcile(c)
 }
 @Synchronized fun setPlace(c:Context,home:Boolean,p:Place){val s=read(c);write(c,if(home)s.copy(home=p)else s.copy(work=p))}
 fun error(c:Context,message:String){prefs(c).edit().putString("error",message).apply()}
}
