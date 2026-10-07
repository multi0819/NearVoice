package com.multi0819.nearvoice
import android.app.*
import android.content.*
import android.net.Uri
import android.os.Build
import java.time.*
@android.annotation.SuppressLint("MissingPermission")
object TimeScheduler {
 fun exactAllowed(c:Context)=Build.VERSION.SDK_INT<31||c.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
 private fun pending(c:Context,uri:String,flags:Int,revision:Long=0,date:String=""):PendingIntent? = PendingIntent.getBroadcast(c,0,
  Intent(c,AlarmReceiver::class.java).setAction("TIME").setData(Uri.parse(uri)).putExtra("revision",revision).putExtra("date",date),flags or PendingIntent.FLAG_IMMUTABLE)
 @Synchronized fun reconcile(c:Context){val p=Store.prefs(c);val am=c.getSystemService(AlarmManager::class.java)
  p.getStringSet("scheduled",emptySet())!!.forEach{pending(c,it,PendingIntent.FLAG_NO_CREATE)?.let{pi->am.cancel(pi);pi.cancel()}}
  val state=Store.read(c);val keys=mutableSetOf<String>()
  if(state.enabled && exactAllowed(c))state.reservations.forEach{r->Rules.nextTime(r,ZonedDateTime.now())?.let{t->
   val uri="nearvoice://time/${r.id}";val pi=pending(c,uri,PendingIntent.FLAG_UPDATE_CURRENT,r.revision,"${t.toLocalDate()}@${t.toLocalTime()}")!!
   try{am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,t.toInstant().toEpochMilli(),pi);keys.add(uri)}catch(_:SecurityException){Store.error(c,"정확한 알람 권한이 필요합니다.")}
  }}
  val boundaryIntent=Intent(c,AlarmReceiver::class.java).setAction("LOCATION_WINDOW").setData(Uri.parse("nearvoice://scan-window"))
  val boundary=PendingIntent.getBroadcast(c,701,boundaryIntent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  am.cancel(boundary)
  if(state.enabled&&state.reservations.any{it.enabled&&it.trigger!="TIME"}&&exactAllowed(c)){
   val next=ScanWindows.nextBoundary(ScanSettings.restricted(c),ScanSettings.windows(c),ZonedDateTime.now())
   try{am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.toInstant().toEpochMilli(),boundary)}catch(_:SecurityException){Store.error(c,"감지 시간대 자동 재개에 정확한 알람 권한이 필요합니다.")}
  }
  c.sendBroadcast(Intent(ScanSettings.ACTION).setPackage(c.packageName))
  p.edit().putStringSet("scheduled",keys).apply()
 }
}
class AlarmReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){
 if(i.action=="LOCATION_WINDOW"){TimeScheduler.reconcile(c);return}
 if(i.action=="android.intent.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"){TimeScheduler.reconcile(c);return}
 val id=i.data?.lastPathSegment?:return;val date=i.getStringExtra("date")?:return;val s=Store.read(c);val r=s.reservations.find{it.id==id}
 if(date.substringBefore("@")!=LocalDate.now().toString()){TimeScheduler.reconcile(c);return}
 if(Rules.canDeliver(r,s.enabled,"TIME",date,i.getLongExtra("revision",-1)))PlaybackService.enqueue(c,r!!,"TIME",date)
 TimeScheduler.reconcile(c)
}}
class BootReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){TimeScheduler.reconcile(c)
 if(Store.read(c).enabled){Notices.alert(c,"NearVoice 예약 복구","시간 예약을 복구했습니다. 위치 감지를 다시 시작하려면 앱을 여세요.")}
}}
