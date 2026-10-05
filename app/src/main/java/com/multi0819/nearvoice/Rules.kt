package com.multi0819.nearvoice
import java.time.*
object Rules {
 fun locationDiagnostic(r:Reservation,date:LocalDate,distance:Double,e:Entry):String = when {
  !r.enabled->"예약 OFF"
  r.trigger=="TIME"->"시간 예약"
  r.repeat in setOf("DATES","ONCE")&&"LOCATION:$date" in r.completed->"오늘 위치 알림 실행 기록 있음"
  !locationAllowed(r,date)->"오늘은 선택한 날짜/요일이 아님"
  !distance.isFinite()->"거리 확인 불가"
  e.fire->"반경 안 · 알림 요청"
  e.inside->"반경 안 · 이미 진입 처리됨"
  else->"반경 밖"
 }
 fun locationUsable(ageNanos:Long,accuracy:Double):Boolean = ageNanos in 0L..120_000_000_000L&&accuracy.isFinite()&&accuracy in 0.0..100.0
 fun notificationOnly(watchMode:Boolean,interactive:Boolean,isTest:Boolean):Boolean = watchMode&&!interactive&&!isTest
 fun nextTime(r:Reservation,after:ZonedDateTime):ZonedDateTime? {
  if(!r.enabled || r.trigger=="LOCATION")return null
  val time=runCatching{LocalTime.parse(r.time)}.getOrNull()?:return null
  val candidates=if(r.repeat=="DATES"||r.repeat=="ONCE")r.dates.mapNotNull{runCatching{LocalDate.parse(it)}.getOrNull()}.sorted()
   else (0..8).map{after.toLocalDate().plusDays(it.toLong())}
  return candidates.asSequence().filter{r.repeat!="WEEKDAYS"||it.dayOfWeek.value in r.weekdays}
   .filter{"TIME:$it" !in r.completed}.map{it.atTime(time).atZone(after.zone)}.firstOrNull{it.isAfter(after)}
 }
 fun locationAllowed(r:Reservation,date:LocalDate):Boolean {
  if(!r.enabled||r.trigger=="TIME"||(r.repeat in setOf("DATES","ONCE")&&"LOCATION:$date" in r.completed))return false
  return when(r.repeat){"DATES","ONCE"->date.toString() in r.dates;"WEEKDAYS"->date.dayOfWeek.value in r.weekdays;else->true}
 }
 fun entry(distance:Double,radius:Double,wasInside:Boolean):Entry {
  if(!distance.isFinite()||distance<0)return Entry(wasInside,false)
  if(wasInside)return Entry(distance<=radius+maxOf(50.0,radius*0.2),false)
  return Entry(distance<=radius,distance<=radius)
 }
 fun eventKey(id:String,condition:String,date:String,revision:Long=0)="$id|$condition|$date|$revision"
 fun toggleDate(selected:Set<String>,date:String,today:LocalDate):Set<String> {
  if(date in selected)return selected-date
  if(runCatching{LocalDate.parse(date).isBefore(today)}.getOrDefault(true))return selected
  return selected+date
 }
 fun validate(r:Reservation):String? = when {
  r.message.length>3900->"메시지는 3900자 이내로 입력하세요."
  r.message.isBlank()->"읽을 메시지를 입력하세요."
  !r.radius.isFinite()||r.radius<100||r.radius>50000->"알림 반경은 100~50000m로 입력하세요."
  r.trigger!="TIME"&&(r.place==null||!validPlace(r.place))->"목적지를 선택하세요."
  (r.repeat=="DATES"||r.repeat=="ONCE")&&r.dates.isEmpty()->"달력에서 날짜를 선택하세요."
  r.repeat=="WEEKDAYS"&&r.weekdays.isEmpty()->"반복 요일을 선택하세요."
  runCatching{LocalTime.parse(r.time)}.isFailure->"시간을 확인하세요."
  !r.voice&&!r.vibration&&r.soundUri==null->"알림 방식을 하나 이상 선택하세요."
  else->null
 }
 fun canDeliver(r:Reservation?,enabled:Boolean,condition:String,date:String,revision:Long):Boolean {
  if(r==null||!enabled||!r.enabled||r.revision!=revision||((condition=="TIME"||r.repeat in setOf("DATES","ONCE"))&&"$condition:$date" in r.completed))return false
  val d=runCatching{LocalDate.parse(date)}.getOrNull()?:return false
  if(condition=="LOCATION")return locationAllowed(r,d)
  if(condition!="TIME"||r.trigger=="LOCATION")return false
  return when(r.repeat){"DATES","ONCE"->date in r.dates;"WEEKDAYS"->d.dayOfWeek.value in r.weekdays;else->true}
 }
 fun mergeSaved(incoming:Reservation,current:Reservation?,rearm:Boolean=false):Reservation = incoming.copy(completed=if(rearm)mutableSetOf() else (incoming.completed+(current?.completed?:emptySet())).toMutableSet())
 fun timeScheduleValid(r:Reservation,now:ZonedDateTime,isNew:Boolean):Boolean {
  if(!isNew||r.trigger!="TIME"||r.repeat !in setOf("DATES","ONCE"))return true
  return nextTime(r.copy(enabled=true,completed=mutableSetOf()),now)!=null
 }
 fun healthWarnings(notificationsAllowed:Boolean,alarmVolume:Int,error:String?):List<String> = buildList {
  if(!notificationsAllowed)add("알림 권한 / 시스템 알림 설정 필요")
  if(alarmVolume==0)add("알람 볼륨 0 · 소리가 들리지 않을 수 있음")
  if(!error.isNullOrBlank())add(error)
 }
 fun validPlace(p:Place)=p.latitude.isFinite()&&p.longitude.isFinite()&&p.latitude in -90.0..90.0&&p.longitude in -180.0..180.0
}

class EventQueue {
 private val queue=java.util.ArrayDeque<String>()
 private val pending=mutableSetOf<String>()
 @Synchronized fun add(key:String):Boolean { if(!pending.add(key))return false;queue.add(key);return true }
 @Synchronized fun poll():String?=queue.poll()
 @Synchronized fun done(key:String){pending.remove(key)}
}

class EntryTracker {
 private val inside=mutableMapOf<String,Boolean>()
 fun update(id:String,revision:Long,distance:Double,radius:Double,date:String):Entry {
  // The date affects eligibility, never physical occupancy.
  val key="$id:$revision";val result=Rules.entry(distance,radius,inside[key]?:false);inside[key]=result.inside;return result
 }
 fun retain(keys:Set<String>){inside.keys.retainAll(keys)}
}
