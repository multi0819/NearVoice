package com.multi0819.nearvoice.wear
import android.app.*
import android.content.*
object WatchNotices {
 fun build(c:Context,title:String,text:String,ongoing:Boolean=false):Notification {
  val manager=c.getSystemService(NotificationManager::class.java)
  manager.createNotificationChannel(NotificationChannel("receiver","음성 수신 상태",NotificationManager.IMPORTANCE_LOW))
  manager.createNotificationChannel(NotificationChannel("message_popup_v2","예약 메시지 표시",NotificationManager.IMPORTANCE_HIGH).apply{setSound(null,null);enableVibration(false)})
  val open=PendingIntent.getActivity(c,0,Intent(c,WatchActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  return Notification.Builder(c,if(ongoing)"receiver"else"message_popup_v2").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(text).setStyle(Notification.BigTextStyle().bigText(text)).setContentIntent(open).setCategory(if(ongoing)Notification.CATEGORY_SERVICE else Notification.CATEGORY_REMINDER).setOngoing(ongoing).setAutoCancel(!ongoing).setLocalOnly(true).build()
 }
 fun show(c:Context,s:Speech){try{c.getSystemService(NotificationManager::class.java).notify(302,build(c,s.title,s.message))}catch(_:SecurityException){WatchStore.status(c,"알림 권한 필요")}}
}
