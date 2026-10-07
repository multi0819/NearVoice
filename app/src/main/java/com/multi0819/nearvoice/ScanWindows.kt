package com.multi0819.nearvoice
import java.time.*
data class ScanWindow(val start:String,val end:String){
 val from:LocalTime=LocalTime.parse(start);val until:LocalTime=LocalTime.parse(end)
 init{require(from!=until){"시작과 종료 시간을 다르게 입력하세요."}}
 fun includes(time:LocalTime)=if(from<until)time>=from&&time<until else time>=from||time<until
}
object ScanWindows {
 fun active(restricted:Boolean,windows:List<ScanWindow>,reservations:List<Reservation>,now:ZonedDateTime)=reservations.any{Rules.locationAllowed(it,now.toLocalDate())}&&(!restricted||windows.any{it.includes(now.toLocalTime())})
 fun nextBoundary(restricted:Boolean,windows:List<ScanWindow>,now:ZonedDateTime):ZonedDateTime{
  val midnight=now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
  return if(!restricted)midnight else (listOf(midnight)+windows.flatMap{w->listOf(w.from,w.until).map{now.toLocalDate().atTime(it).atZone(now.zone)}}).filter{it.isAfter(now)}.minBy{it.toInstant()}
 }
}
