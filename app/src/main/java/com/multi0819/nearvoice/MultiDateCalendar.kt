package com.multi0819.nearvoice
import android.content.Context
import android.view.Gravity
import android.widget.*
import java.time.*
class MultiDateCalendar(c:Context,initial:Set<String>,private val onChange:(Set<String>)->Unit):LinearLayout(c){
 var selected:Set<String> = initial;private set
 private var month=YearMonth.now()
 init{orientation=VERTICAL;render()}
 private fun render(){removeAllViews();val nav=Ui.row(context)
  Ui.weighted(nav,Ui.button(context,"‹ 이전"){month=month.minusMonths(1);render()})
  Ui.weighted(nav,Ui.label(context,"${month.year}년 ${month.monthValue}월",16f,Ui.accent).apply{gravity=Gravity.CENTER})
  Ui.weighted(nav,Ui.button(context,"다음 ›"){month=month.plusMonths(1);render()});addView(nav)
  val weekdays=Ui.row(context);listOf("월","화","수","목","금","토","일").forEach{Ui.weighted(weekdays,Ui.label(context,it,13f,Ui.muted).apply{gravity=Gravity.CENTER})};addView(weekdays)
  val offset=month.atDay(1).dayOfWeek.value-1
  for(week in 0..5){val row=Ui.row(context);for(day in 0..6){val n=week*7+day-offset+1
   if(n in 1..month.lengthOfMonth()){val date=month.atDay(n).toString();val picked=date in selected;val old=month.atDay(n).isBefore(LocalDate.now())
    val b=Ui.button(context,n.toString()){selected=Rules.toggleDate(selected,date,LocalDate.now());onChange(selected);render()};b.textSize=14f;b.setPadding(0,0,0,0);b.minWidth=0;b.minimumWidth=0
    b.setTextColor(if(picked)Ui.bg else if(old)Ui.muted else Ui.text);b.background=Ui.shape(if(picked)Ui.accent else Ui.panel);b.isEnabled=!old||picked;Ui.weighted(row,b)
   }else Ui.weighted(row,Space(context))
  };addView(row)}
  addView(Ui.label(context,"${selected.size}개 날짜 선택 · ${selected.sorted().joinToString(", ")}",13f,Ui.muted))
  addView(Ui.button(context,"날짜 선택 전체 해제"){selected=emptySet();onChange(selected);render()})
 }
}
