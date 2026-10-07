package com.multi0819.nearvoice.wear
import android.app.Service
import android.content.Intent
import android.media.*
import android.os.*
import android.speech.tts.*
import java.util.Locale
@android.annotation.SuppressLint("MissingPermission")
class VoiceService:Service(){
 companion object {@Volatile var running=false}
 private val handler=Handler(Looper.getMainLooper());private val queue=java.util.ArrayDeque<Speech>();private val pending=mutableSetOf<String>()
 private var current:Speech?=null;private var tts:TextToSpeech?=null;private var ready=false;private var voiceOk=false;private var wake:PowerManager.WakeLock?=null;private var focus:AudioFocusRequest?=null
 private val timeout=Runnable{WatchStore.status(this,"음성 재생 시간 초과");finishSpeech()}
 override fun onBind(i:Intent?)=null
 override fun onCreate(){super.onCreate();startForeground(301,WatchNotices.build(this,"NearVoice · 음성 수신 ON","휴대폰 예약 메시지를 기다립니다.",true));running=true
  tts=TextToSpeech(this){result->handler.post{
   if(ready)return@post;ready=true;voiceOk=result==TextToSpeech.SUCCESS&&(tts?.setLanguage(Locale.KOREAN)?:TextToSpeech.ERROR)>=TextToSpeech.LANG_AVAILABLE
   WatchStore.status(this,if(voiceOk)"한국어 음성 준비됨"else"한국어 음성 데이터가 없습니다. 워치 음성 합성 설정을 확인하세요.")
   tts?.setOnUtteranceProgressListener(object:UtteranceProgressListener(){override fun onStart(id:String?){}
    override fun onDone(id:String?){handler.post{if(id==current?.key){WatchStore.status(this@VoiceService,"읽기 완료");finishSpeech()}}}
    @Deprecated("Legacy callback") override fun onError(id:String?){handler.post{if(id==current?.key){WatchStore.status(this@VoiceService,"음성 읽기 실패");finishSpeech()}}}
   });next()
  }}
  handler.postDelayed({if(!ready){ready=true;WatchStore.status(this,"음성 엔진 응답 없음 · 문자로 확인하세요.");next()}},10000)
 }
 override fun onStartCommand(i:Intent?,flags:Int,startId:Int):Int {
  if(!WatchStore.prefs(this).getBoolean("enabled",false)){stopSelf();return START_NOT_STICKY}
  i?.getStringExtra("speech")?.let{Speech.parse(it)}?.let{s->if(pending.add(s.key)){queue.add(s);next()}}
  return START_STICKY
 }
 private fun next(){if(!ready||current!=null)return;val s=queue.poll()?:return;current=s;WatchNotices.show(this,s)
  if(!s.voice){finishSpeech();return};if(!voiceOk){WatchStore.status(this,"한국어 음성 없음 · 메시지를 확인하세요.");finishSpeech();return}
  wake=getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"NearVoice:watchSpeech").apply{acquire(125000)}
  val attributes=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
  focus=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(attributes).setOnAudioFocusChangeListener{change->if(change==AudioManager.AUDIOFOCUS_LOSS)handler.post{finishSpeech()}}.build()
  getSystemService(AudioManager::class.java).requestAudioFocus(focus!!);tts?.setAudioAttributes(attributes)
  if(tts?.speak(s.message,TextToSpeech.QUEUE_FLUSH,Bundle(),s.key)!=TextToSpeech.SUCCESS){WatchStore.status(this,"음성 재생 실패");finishSpeech()}else{WatchStore.status(this,"읽는 중 · ${s.title}");handler.postDelayed(timeout,120000)}
 }
 private fun finishSpeech(){handler.removeCallbacks(timeout);tts?.stop();wake?.let{if(it.isHeld)it.release()};wake=null
  focus?.let{getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it)};focus=null;current?.let{pending.remove(it.key)};current=null;next()
 }
 override fun onDestroy(){running=false;handler.removeCallbacksAndMessages(null);tts?.stop();tts?.shutdown();wake?.let{if(it.isHeld)it.release()};focus?.let{getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it)};super.onDestroy()}
}
