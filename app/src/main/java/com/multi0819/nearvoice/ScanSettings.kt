package com.multi0819.nearvoice
import android.app.*
import android.content.Context
import android.widget.*
import org.json.*
import java.time.*
object ScanSettings {
 const val ACTION="com.multi0819.nearvoice.SCAN_REFRESH"
 fun restricted(c:Context)=Store.prefs(c).getBoolean("scan_restricted",false)
 fun windows(c:Context):List<ScanWindow> = runCatching{val a=JSONArray(Store.prefs(c).getString("scan_windows","[]"));(0 until a.length()).mapNotNull{runCatching{val j=a.getJSONObject(it);ScanWindow(j.getString("start"),j.getString("end"))}.getOrNull()}}.getOrDefault(emptyList())
 fun active(c:Context)=Store.read(c).let{s->s.enabled&&ScanWindows.active(restricted(c),windows(c),s.reservations,ZonedDateTime.now())}
 fun save(c:Context,on:Boolean,items:List<ScanWindow>){require(!on||items.isNotEmpty()){ "감지 시간대를 하나 이상 추가하세요." };check(Store.prefs(c).edit().putBoolean("scan_restricted",on).putString("scan_windows",JSONArray(items.map{JSONObject().put("start",it.start).put("end",it.end)}).toString()).commit()){ "감지 시간대를 저장하지 못했습니다." };TimeScheduler.reconcile(c)}
}
fun MainActivity.scanWindowSettings(){
 subpage=true;val c=Ui.page(this,"위치 감지 시간대");c.addView(Ui.button(this,"‹ 설정"){settings()})
 val enabled=Ui.toggle(this,"설정한 시간대에만 감지",ScanSettings.restricted(this));c.addView(enabled)
 c.addView(Ui.label(this,"예약의 날짜를 그대로 따릅니다. 시간대 밖에서는 GPS 감지를 쉬며, 시간 예약 알림은 그대로 실행됩니다. 대기 알림은 자동 재개를 위해 유지됩니다.",13f,Ui.muted))
 val draft=ScanSettings.windows(this).toMutableList();val list=Ui.column(this);c.addView(list)
 fun render(){list.removeAllViews();draft.forEachIndexed{index,w->
  val card=Ui.column(this);card.background=Ui.shape();card.addView(Ui.label(this,"감지 구간 ${index+1}${if(w.from>w.until)" · 자정 넘김"else""}",14f,Ui.muted))
  val row=Ui.row(this)
  fun choose(start:Boolean){val old=if(start)w.from else w.until;TimePickerDialog(this,{_,h,m->val value=LocalTime.of(h,m).toString();change{draft[index]=if(start)ScanWindow(value,w.end)else ScanWindow(w.start,value);render()}},old.hour,old.minute,true).show()}
  Ui.weighted(row,Ui.button(this,"시작 ${w.start}"){choose(true)});Ui.weighted(row,Ui.button(this,"종료 ${w.end}"){choose(false)});card.addView(row)
  card.addView(Ui.button(this,"구간 삭제"){draft.removeAt(index);render()});list.addView(card);list.addView(Ui.space(this))
 }}
 render();c.addView(Ui.button(this,"+ 시간대 추가"){draft.add(ScanWindow("07:00","09:00"));render()})
 c.addView(Ui.button(this,"감지 시간대 저장"){change{if(enabled.isChecked&&!TimeScheduler.exactAllowed(this)){checkPermissions();return@change};ScanSettings.save(this,enabled.isChecked,draft.toList());if(Store.read(this).enabled&&LocationService.permitted(this))LocationService.start(this);settings();toast("위치 감지 시간대를 저장했습니다.")}})
}
