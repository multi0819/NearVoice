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
 private val tracker=EntryTracker()
 companion object {
  @Volatile var running=false
  @Volatile var latest:Location?=null
  @Volatile var status="위치 감지 OFF"
  fun permitted(c:Context)=c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
  fun start(c:Context){c.startForegroundService(Intent(c,LocationService::class.java))}
  fun stop(c:Context){c.stopService(Intent(c,LocationService::class.java))}
 }
 override fun onBind(i:Intent?)=null
 override fun onCreate(){super.onCreate();manager=getSystemService(LocationManager::class.java)}
 override fun onStartCommand(i:Intent?,flags:Int,startId:Int):Int {
  if(!Store.read(this).enabled||!permitted(this)){stopSelf();return START_NOT_STICKY}
  try {startForeground(101,Notices.build(this,"monitor","NearVoice · 감지 ON","목적지 접근을 확인하고 있습니다.",true))
   running=true;manager.removeUpdates(this)
   for(p in listOf(LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER))if(p in manager.allProviders)manager.requestLocationUpdates(p,15000L,10f,this)
   status=if(if(Build.VERSION.SDK_INT>=28)manager.isLocationEnabled else manager.isProviderEnabled(LocationManager.GPS_PROVIDER)||manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))"위치 수신 대기"else"기기 위치 설정 OFF"
  }catch(_:SecurityException){status="위치 권한 필요";Store.error(this,status);stopSelf()}
  return START_STICKY
 }
 override fun onLocationChanged(l:Location){
  val s=Store.read(this);if(!s.enabled){stopSelf();return}
  if(SystemClock.elapsedRealtimeNanos()-l.elapsedRealtimeNanos>120_000_000_000L||!l.hasAccuracy()||l.accuracy>100f)return
  latest=l;status="감지 중 · 정확도 ±${l.accuracy.toInt()}m"
  val today=LocalDate.now()
  val currentKeys=s.reservations.map{"${it.id}:${it.revision}"}.toSet();tracker.retain(currentKeys)
  s.reservations.forEach{r->val p=r.place?:return@forEach;if(!r.enabled||r.trigger=="TIME")return@forEach
   val result=FloatArray(1);Location.distanceBetween(l.latitude,l.longitude,p.latitude,p.longitude,result)
   val e=tracker.update(r.id,r.revision,result[0].toDouble(),r.radius,today.toString())
   if(e.fire&&Rules.locationAllowed(r,today))PlaybackService.enqueue(this,r,"LOCATION",today.toString())
  }
 }
 override fun onProviderDisabled(provider:String){status="위치 신호 대기"}
 override fun onProviderEnabled(provider:String){status="위치 수신 대기"}
 @Deprecated("Legacy callback") override fun onStatusChanged(provider:String?,status:Int,extras:Bundle?){}
 override fun onDestroy(){manager.removeUpdates(this);running=false;status="위치 감지 OFF";super.onDestroy()}
}
