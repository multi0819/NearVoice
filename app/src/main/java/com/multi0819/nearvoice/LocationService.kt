package com.multi0819.nearvoice
import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.location.*
import android.os.*
import java.time.LocalDate
@android.annotation.SuppressLint("MissingPermission")
class LocationService:Service(),LocationListener {
 private lateinit var manager:LocationManager
 private var tracker=EntryTracker()
 private var scanning=false
 private val refreshReceiver=object:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){refreshDetection()}}
 companion object {
  @Volatile var running=false
  @Volatile var latest:Location?=null
  @Volatile var status="위치 감지 OFF"
  fun permitted(c:Context)=c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
  fun start(c:Context){c.startForegroundService(Intent(c,LocationService::class.java))}
  fun stop(c:Context){c.stopService(Intent(c,LocationService::class.java))}
 }
 override fun onBind(i:Intent?)=null
 override fun onCreate(){super.onCreate();manager=getSystemService(LocationManager::class.java);registerRefreshReceiver()}
 @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag")
 private fun registerRefreshReceiver(){
  // API 33+ has NOT_EXPORTED. Earlier releases are protected by our signature permission.
  val permission="$packageName.SCAN_INTERNAL"
  if(Build.VERSION.SDK_INT>=33)registerReceiver(refreshReceiver,IntentFilter(ScanSettings.ACTION),permission,null,Context.RECEIVER_NOT_EXPORTED)
  else @Suppress("DEPRECATION") registerReceiver(refreshReceiver,IntentFilter(ScanSettings.ACTION),permission,null)
 }
 override fun onStartCommand(i:Intent?,flags:Int,startId:Int):Int {
  if(!Store.read(this).enabled||!permitted(this)||Store.read(this).reservations.none{it.enabled&&it.trigger!="TIME"}){stopSelf();return START_NOT_STICKY}
  startForeground(101,Notices.build(this,"monitor","NearVoice · 위치 감지 준비","예약 날짜와 감지 시간대를 확인합니다.",true))
  running=true;refreshDetection();return START_STICKY
 }
 private fun refreshDetection(){
  if(!Store.read(this).enabled||!permitted(this)||Store.read(this).reservations.none{it.enabled&&it.trigger!="TIME"}){stopSelf();return}
  try{
   val active=ScanSettings.active(this)
   if(!active){if(scanning)manager.removeUpdates(this);scanning=false;tracker=EntryTracker();status="감지 시간대 / 예약 날짜 대기";getSystemService(NotificationManager::class.java).notify(101,Notices.build(this,"monitor","NearVoice · 위치 감지 쉬는 중","예약 날짜의 감지 구간에서 자동으로 재개합니다.",true));return}
   if(!scanning){manager.removeUpdates(this);for(p in listOf(LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER))if(p in manager.allProviders)manager.requestLocationUpdates(p,15000L,0f,this);scanning=true}
   status="위치 수신 대기";getSystemService(NotificationManager::class.java).notify(101,Notices.build(this,"monitor","NearVoice · 감지 ON","목적지 접근을 확인하고 있습니다.",true))
   val recent=(listOfNotNull(latest)+listOf(LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER).mapNotNull{p->runCatching{manager.getLastKnownLocation(p)}.getOrNull()}).filter{it.hasAccuracy()&&Rules.locationUsable(SystemClock.elapsedRealtimeNanos()-it.elapsedRealtimeNanos,it.accuracy.toDouble())}.maxByOrNull{it.elapsedRealtimeNanos}
   if(recent!=null&&(if(Build.VERSION.SDK_INT>=28)manager.isLocationEnabled else manager.isProviderEnabled(LocationManager.GPS_PROVIDER)||manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)))onLocationChanged(recent)
  }catch(_:SecurityException){status="위치 권한 필요";Store.error(this,status);stopSelf()}
 }
 override fun onLocationChanged(l:Location){
  val s=Store.read(this);if(!s.enabled){stopSelf();return}
  if(!ScanSettings.active(this)){refreshDetection();return}
  if(!l.hasAccuracy()||!Rules.locationUsable(SystemClock.elapsedRealtimeNanos()-l.elapsedRealtimeNanos,l.accuracy.toDouble()))return
  latest=l;status="감지 중 · 정확도 ±${l.accuracy.toInt()}m"
  val today=LocalDate.now()
  val currentKeys=s.reservations.map{"${it.id}:${it.revision}"}.toSet();tracker.retain(currentKeys)
  s.reservations.forEach{r->val p=r.place?:return@forEach;if(!r.enabled||r.trigger=="TIME")return@forEach
   val result=FloatArray(1);Location.distanceBetween(l.latitude,l.longitude,p.latitude,p.longitude,result)
   val e=tracker.update(r.id,r.revision,result[0].toDouble(),r.radius,today.toString())
   Store.prefs(this).edit().putLong("location_check_revision_${r.id}",r.revision).putString("location_check_${r.id}","${java.time.LocalTime.now().withNano(0)} · 거리 ${result[0].toInt()}m / 반경 ${r.radius.toInt()}m\n${Rules.locationDiagnostic(r,today,result[0].toDouble(),e)}").apply()
   if(e.fire&&Rules.locationAllowed(r,today))PlaybackService.enqueue(this,r,"LOCATION",today.toString())
  }
 }
 override fun onProviderDisabled(provider:String){status="위치 신호 대기"}
 override fun onProviderEnabled(provider:String){status="위치 수신 대기"}
 @Deprecated("Legacy callback") override fun onStatusChanged(provider:String?,status:Int,extras:Bundle?){}
 override fun onDestroy(){unregisterReceiver(refreshReceiver);manager.removeUpdates(this);running=false;status="위치 감지 OFF";super.onDestroy()}
}
