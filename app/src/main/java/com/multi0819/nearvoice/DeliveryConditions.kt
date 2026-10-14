package com.multi0819.nearvoice
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import java.time.ZonedDateTime

object DeliveryConditions {
 fun allowed(c:Context,snapshot:Reservation,condition:String,date:String):Boolean {
  val state=Store.read(c)
  val r=state.reservations.find{it.id==snapshot.id}?:return false
  if(r.revision!=snapshot.revision)return false
  var distance=Double.NaN
  var fresh=false
  var active=false
  if(r.trigger=="BOTH"){
   val manager=c.getSystemService(LocationManager::class.java)
   val locationEnabled=if(Build.VERSION.SDK_INT>=28)manager.isLocationEnabled else manager.isProviderEnabled(LocationManager.GPS_PROVIDER)||manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
   active=LocationService.running&&LocationService.permitted(c)&&locationEnabled&&ScanSettings.active(c)
   val l=LocationService.latest
   val p=r.place
   if(active&&l!=null&&p!=null&&l.hasAccuracy()){
    fresh=Rules.locationUsable(SystemClock.elapsedRealtimeNanos()-l.elapsedRealtimeNanos,l.accuracy.toDouble())
    val result=FloatArray(1)
    Location.distanceBetween(l.latitude,l.longitude,p.latitude,p.longitude,result)
    distance=result[0].toDouble()
   }
  }
  return Rules.deliveryAllowed(r,state.enabled,condition,date,snapshot.revision,ZonedDateTime.now(),distance,active,fresh)
 }
}
