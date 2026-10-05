package com.multi0819.nearvoice
import android.app.*
import android.content.*
import android.os.Build
object Notices {
 fun channels(c:Context){val n=c.getSystemService(NotificationManager::class.java)
  n.createNotificationChannel(NotificationChannel("monitor","위치 감지 상태",NotificationManager.IMPORTANCE_LOW))
  n.createNotificationChannel(NotificationChannel("playback","메시지 읽기",NotificationManager.IMPORTANCE_LOW))
  n.createNotificationChannel(NotificationChannel("alerts","예약 및 설정 안내",NotificationManager.IMPORTANCE_DEFAULT).apply{setSound(null,null);enableVibration(false)})
 }
 fun build(c:Context,channel:String,title:String,message:String,ongoing:Boolean=false):Notification {
  channels(c)
  val open=PendingIntent.getActivity(c,0,Intent(c,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val builder=Notification.Builder(c,channel).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(message)
   .setStyle(Notification.BigTextStyle().bigText(message)).setContentIntent(open).setOngoing(ongoing).setAutoCancel(!ongoing)
  if(channel=="monitor")builder.addAction(Notification.Action.Builder(android.R.drawable.ic_media_pause,"전체 알림 끄기",PendingIntent.getBroadcast(c,999,Intent(c,StopReceiver::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)).build())
  return builder.build()
 }
 fun alert(c:Context,title:String,message:String,id:Int=303){try{c.getSystemService(NotificationManager::class.java).notify(id,build(c,"alerts",title,message))}catch(_:SecurityException){}}
}

class StopReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){Store.enable(c,false);LocationService.stop(c);c.stopService(Intent(c,PlaybackService::class.java))}}
