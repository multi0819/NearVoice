package com.multi0819.nearvoice
import android.app.*
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.text.InputType
import android.view.View
import android.widget.*
import java.time.*
fun MainActivity.openEditor(original:Reservation?,initialPlace:Place?=null){subpage=true;val base=original?:Reservation(place=initialPlace,trigger=if(initialPlace==null)"TIME"else"LOCATION");val c=Ui.page(this,if(original==null)"예약 추가"else"예약 수정")
 c.addView(Ui.button(this,"‹ 취소 / 목록"){dashboard()});val title=Ui.edit(this,"예약 제목",base.title);c.addView(title);c.addView(Ui.space(this));val message=Ui.edit(this,"읽어줄 메시지",base.message,true);c.addView(message)
 c.addView(Ui.label(this,"알림 조건",16f));val modes=listOf("TIME","LOCATION","BOTH");val trigger=Ui.spinner(this,listOf("시간 알림","위치 알림","위치 + 시간"),modes.indexOf(base.trigger).coerceAtLeast(0));c.addView(trigger)
 var place=base.place;val placeText=Ui.label(this,place?.label?:"목적지를 선택하세요",14f,Ui.muted);val placeBox=Ui.column(this);placeBox.addView(placeText);val places=Ui.row(this)
 fun selected(p:Place){place=p;placeText.text="${p.label}\n%.5f, %.5f".format(p.latitude,p.longitude)}
 val state=Store.read(this)
 Ui.weighted(places,Ui.button(this,"집"){state.home?.let{selected(it)}?:run{toast("설정에서 집 주소를 등록하세요.")}})
 Ui.weighted(places,Ui.button(this,"회사"){state.work?.let{selected(it)}?:run{toast("설정에서 회사 주소를 등록하세요.")}})
 Ui.weighted(places,Ui.button(this,"지도"){pickPlace(place){selected(it)}});placeBox.addView(places)
 placeBox.addView(Ui.label(this,"알림 반경 (m)",14f));val radius=Ui.edit(this,"100~50000",base.radius.toInt().toString());radius.inputType=InputType.TYPE_CLASS_NUMBER;placeBox.addView(radius)
 val presets=Ui.row(this);listOf(100,300,500,1000).forEach{n->Ui.weighted(presets,Ui.button(this,"${n}m"){radius.setText(n.toString())})};placeBox.addView(presets);c.addView(placeBox)
 trigger.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{override fun onNothingSelected(p:AdapterView<*>?){};override fun onItemSelected(p:AdapterView<*>?,v:View?,pos:Int,id:Long){placeBox.visibility=if(pos==0)View.GONE else View.VISIBLE}}
 c.addView(Ui.label(this,"반복 / 날짜",16f));val repeats=listOf("DATES","DAILY","WEEKDAYS");val repeat=Ui.spinner(this,listOf("달력에서 날짜 선택 (복수 가능)","매일 반복","요일별 반복"),repeats.indexOf(base.repeat).coerceAtLeast(0));c.addView(repeat)
 var dates=base.dates;val calendar=MultiDateCalendar(this,dates){dates=it};c.addView(calendar)
 val days=Ui.row(this);val weekdayChecks=(1..7).map{n->CheckBox(this).apply{text=listOf("월","화","수","목","금","토","일")[n-1];setTextColor(Ui.text);textSize=12f;setPadding(0,0,0,0);isChecked=n in base.weekdays;Ui.weighted(days,this)}};c.addView(days)
 repeat.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{override fun onNothingSelected(p:AdapterView<*>?){};override fun onItemSelected(p:AdapterView<*>?,v:View?,pos:Int,id:Long){calendar.visibility=if(pos==0)View.VISIBLE else View.GONE;days.visibility=if(pos==2)View.VISIBLE else View.GONE}}
 var time=runCatching{LocalTime.parse(base.time)}.getOrDefault(LocalTime.of(9,0));val timeButton=Ui.button(this,"알림 시간 · $time"){}
 timeButton.setOnClickListener{TimePickerDialog(this,{_,h,m->time=LocalTime.of(h,m);timeButton.text="알림 시간 · $time"},time.hour,time.minute,true).show()};c.addView(timeButton)
 c.addView(Ui.label(this,"위치 알림은 선택 날짜에 활성화됩니다. 시간 알림은 선택 날짜의 지정 시각에 실행됩니다.",13f,Ui.muted))
 val voice=Ui.toggle(this,"메시지 음성 읽기",base.voice);val vibration=Ui.toggle(this,"진동",base.vibration);c.addView(voice);c.addView(vibration)
 var sound=base.soundUri;val soundText=Ui.label(this,if(sound==null)"알림음 없음"else"알림음 선택됨",13f,Ui.muted);c.addView(soundText)
 val soundRow=Ui.row(this);Ui.weighted(soundRow,Ui.button(this,"알림음 선택"){soundCallback={uri->sound=uri;soundText.text=if(uri==null)"알림음 없음"else"알림음 선택됨"};val i=Intent(RingtoneManager.ACTION_RINGTONE_PICKER).putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_ALARM).putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT,true).putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,sound?.let{Uri.parse(it)});startActivityForResult(i,11)})
 Ui.weighted(soundRow,Ui.button(this,"소리 끄기"){sound=null;soundText.text="알림음 없음"});c.addView(soundRow)
 fun draft()=base.copy(title=title.text.toString().trim().ifEmpty{"예약"},message=message.text.toString().trim(),trigger=modes[trigger.selectedItemPosition],place=place,
  radius=radius.text.toString().toDoubleOrNull()?:0.0,dates=dates,repeat=repeats[repeat.selectedItemPosition],weekdays=weekdayChecks.mapIndexedNotNull{idx,b->if(b.isChecked)idx+1 else null}.toSet(),time=time.toString(),voice=voice.isChecked,vibration=vibration.isChecked,soundUri=sound,completed=base.completed.toMutableSet(),revision=maxOf(System.currentTimeMillis(),base.revision+1))
 c.addView(Ui.space(this));val controls=Ui.row(this);Ui.weighted(controls,Ui.button(this,"미리 듣기"){val r=draft();if(r.message.isBlank())toast("메시지를 입력하세요.")else PlaybackService.test(this,r)})
 Ui.weighted(controls,Ui.button(this,"예약 저장"){val r=draft();val error=Rules.validate(r);if(error!=null){toast(error);return@button}
  if(!Rules.timeScheduleValid(r,ZonedDateTime.now(),original==null)){toast("앞으로 실행할 날짜와 시간을 선택하세요.");return@button}
  change{Store.upsert(this,r,rearm=true);if(Store.read(this).enabled&&r.trigger!="TIME"&&LocationService.permitted(this))LocationService.start(this);dashboard();toast("예약을 저장했습니다. 실행 기록이 초기화되었습니다.")}
 });c.addView(controls)
}
