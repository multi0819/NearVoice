package com.multi0819.nearvoice.wear
import android.app.*
import android.content.*
object WatchNotices {
 fun build(c:Context,title:String,text:String,ongoing:Boolean=false,vibration:Boolean=false):Notification {
  val manager=c.getSystemService(NotificationManager::class.java)
  manager.createNotificationChannel(NotificationChannel("receiver","음성 수신 상태",NotificationManager.IMPORTANCE_LOW))
  val messageChannel=if(vibration)"message_vibrate_v3"else"message_silent_v3"
  manager.createNotificationChannel(NotificationChannel(messageChannel,if(vibration)"예약 메시지 · 진동 및 화면 표시"else"예약 메시지 · 무음",NotificationManager.IMPORTANCE_HIGH).apply{setSound(null,null);enableVibration(vibration);if(vibration)vibrationPattern=longArrayOf(0,250,150,250)})
  val open=PendingIntent.getActivity(c,0,Intent(c,WatchActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  return Notification.Builder(c,if(ongoing)"receiver"else messageChannel).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(text).setStyle(Notification.BigTextStyle().bigText(text)).setContentIntent(open).setCategory(if(ongoing)Notification.CATEGORY_SERVICE else Notification.CATEGORY_REMINDER).setOngoing(ongoing).setAutoCancel(!ongoing).setLocalOnly(true).build()
 }
 fun show(c:Context,s:Speech){try{c.getSystemService(NotificationManager::class.java).notify(302,build(c,s.title,s.message,vibration=s.vibration))}catch(_:SecurityException){WatchStore.status(c,"알림 권한 필요")}}
}
