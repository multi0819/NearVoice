package com.multi0819.nearvoice
import android.app.*
import android.content.*
import android.media.*
import android.net.Uri
import android.os.*
import android.speech.tts.*
import java.util.Locale
import org.json.JSONObject
@android.annotation.SuppressLint("MissingPermission")
class PlaybackService:Service(){
 private val handler=Handler(Looper.getMainLooper());private val queue=EventQueue();private val events=mutableMapOf<String,Triple<Reservation,String,String>>()
 private var current:String?=null;private var tts:TextToSpeech?=null;private var initialized=false;private var ttsOk=false
 private var sound:Ringtone?=null;private var focus:AudioFocusRequest?=null;private var lock:PowerManager.WakeLock?=null
 private val finishTimeout=Runnable{finishCurrent()}
 companion object {
  fun enqueue(c:Context,r:Reservation,condition:String,date:String){try{c.startForegroundService(Intent(c,PlaybackService::class.java).putExtra("reservation",Codec.json(r).toString()).putExtra("condition",condition).putExtra("date",date))}
   catch(e:RuntimeException){Store.error(c,"자동 읽기를 시작하지 못했습니다. 앱에서 권한을 확인하세요.");Notices.alert(c,r.title,r.message)}
  }
  fun test(c:Context,r:Reservation){enqueue(c,r,"TEST",java.time.LocalDate.now().toString())}
 }
 override fun onBind(i:Intent?)=null
 override fun onCreate(){super.onCreate();startForeground(202,Notices.build(this,"playback","NearVoice","알림 준비 중",true))
  lock=getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"NearVoice:playback").apply{acquire(180000)}
  tts=TextToSpeech(this){result->handler.post{
   initialized=true;val language=if(result==TextToSpeech.SUCCESS)tts?.setLanguage(Locale.KOREAN)?:TextToSpeech.ERROR else TextToSpeech.ERROR
   ttsOk=language>=TextToSpeech.LANG_AVAILABLE
   if(!ttsOk)Store.error(this,"한국어 음성 엔진 또는 음성 데이터가 없습니다. 설정에서 음성 테스트를 확인하세요.")
   tts?.setOnUtteranceProgressListener(object:UtteranceProgressListener(){override fun onStart(id:String?){}
    override fun onDone(id:String?){handler.post{if(id==current)finishCurrent()}}
    @Deprecated("Legacy callback") override fun onError(id:String?){handler.post{if(id==current){Store.error(this@PlaybackService,"음성 읽기에 실패했습니다.");finishCurrent()}}}
   });next()
  }}
  handler.postDelayed({if(!initialized){initialized=true;ttsOk=false;Store.error(this,"음성 엔진 응답이 없습니다.");next()}},10000)
 }
 override fun onStartCommand(i:Intent?,flags:Int,startId:Int):Int {
  if(i==null){stopSelf();return START_NOT_STICKY}
  val r=runCatching{Codec.reservation(JSONObject(i.getStringExtra("reservation")?:""))}.getOrNull()?:run{stopSelf();return START_NOT_STICKY}
  val condition=i.getStringExtra("condition")?:"TEST";val date=i.getStringExtra("date")?:"";val key=Rules.eventKey(r.id,condition,date,r.revision)
  if(queue.add(key))events[key]=Triple(r,condition,date)
  next();return START_NOT_STICKY
 }
 private fun next(){if(!initialized||current!=null)return
  val key=queue.poll()?:run{stopSelf();return};val event=events[key]?:run{queue.done(key);next();return}
  val (snapshot,condition,date)=event;val state=Store.read(this);val r=if(condition=="TEST")snapshot else state.reservations.find{it.id==snapshot.id}
  if(condition!="TEST"&&!Rules.canDeliver(r,state.enabled,condition,date,snapshot.revision)){events.remove(key);queue.done(key);next();return}
  current=key
  if(condition!="TEST")Store.complete(this,snapshot.id,condition,date)
  val reservation=r?:snapshot
  getSystemService(NotificationManager::class.java).notify(202,Notices.build(this,"playback",reservation.title,reservation.message,true))
  Notices.alert(this,reservation.title,reservation.message,reservation.id.hashCode())
  val audio=getSystemService(AudioManager::class.java);val attributes=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
  focus=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(attributes).setOnAudioFocusChangeListener{change->if(change==AudioManager.AUDIOFOCUS_LOSS)handler.post{finishCurrent()}}.build()
  audio.requestAudioFocus(focus!!)
  if(reservation.vibration){try{getSystemService(Vibrator::class.java).vibrate(VibrationEffect.createWaveform(longArrayOf(0,250,150,250),-1))}catch(_:SecurityException){}}
  if(reservation.soundUri!=null){try{sound=RingtoneManager.getRingtone(this,Uri.parse(reservation.soundUri));sound?.audioAttributes=attributes;sound?.play()}catch(_:Exception){Store.error(this,"선택한 알림음을 재생할 수 없습니다.")}}
  handler.postDelayed({if(current!=key)return@postDelayed;sound?.stop();sound=null
   if(reservation.voice&&ttsOk){tts?.setAudioAttributes(attributes);val status=tts?.speak(reservation.message,TextToSpeech.QUEUE_FLUSH,Bundle(),key)
    if(status!=TextToSpeech.SUCCESS){Store.error(this,"음성 읽기에 실패했습니다.");finishCurrent()}
    else handler.postDelayed(finishTimeout,120000)
   }else {if(reservation.voice&&!ttsOk)Notices.alert(this,"음성 설정 필요",reservation.message);finishCurrent()}
  },if(reservation.soundUri!=null)1800 else 500)
 }
 private fun finishCurrent(){val key=current?:return;handler.removeCallbacks(finishTimeout);tts?.stop();sound?.stop();sound=null
  focus?.let{getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it)};focus=null;events.remove(key);queue.done(key);current=null;next()
 }
 override fun onDestroy(){handler.removeCallbacksAndMessages(null);tts?.stop();tts?.shutdown();sound?.stop();focus?.let{getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it)}
  lock?.let{if(it.isHeld)it.release()};super.onDestroy()}
}
