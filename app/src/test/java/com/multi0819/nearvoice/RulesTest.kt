package com.multi0819.nearvoice
import org.junit.Test
import org.junit.Assert.*
import java.time.*
class RulesTest {
 private val zone = ZoneId.of("Asia/Seoul")
 private fun at(s:String) = LocalDateTime.parse(s).atZone(zone)
 @Test fun selectedDatesIncludeLeapDayAndSkipCompleted() {
  val r=Reservation(dates=setOf("2028-02-29","2028-03-03"),repeat="DATES",time="09:00",trigger="BOTH")
  assertEquals(at("2028-02-29T09:00"),Rules.nextTime(r,at("2028-02-28T10:00")))
  r.completed.add("TIME:2028-02-29")
  assertEquals(at("2028-03-03T09:00"),Rules.nextTime(r,at("2028-02-28T10:00")))
  assertTrue(Rules.locationAllowed(r,LocalDate.parse("2028-02-29")))
 }
 @Test fun weekdaysAndPastDates() {
  val r=Reservation(repeat="WEEKDAYS",weekdays=setOf(1,5),time="08:30")
  assertEquals(at("2026-10-09T08:30"),Rules.nextTime(r,at("2026-10-05T09:00")))
  assertNull(Rules.nextTime(Reservation(dates=setOf("2026-10-04"),time="09:00"),at("2026-10-05T09:00")))
 }
 @Test fun boundaryJitterDoesNotRearm() {
  assertEquals(Entry(true,true),Rules.entry(290.0,300.0,false))
  assertEquals(Entry(true,false),Rules.entry(325.0,300.0,true))
  assertEquals(Entry(false,false),Rules.entry(370.0,300.0,true))
  assertEquals(Entry(true,true),Rules.entry(280.0,300.0,false))
 }
 @Test fun locationDateAndConditionCompletionAreIndependent() {
  val r=Reservation(repeat="DATES",dates=setOf("2026-10-05","2026-11-03"),trigger="BOTH")
  assertFalse(Rules.locationAllowed(r,LocalDate.parse("2026-10-06")))
  r.completed.add("LOCATION:2026-10-05")
  assertFalse(Rules.locationAllowed(r,LocalDate.parse("2026-10-05")))
  assertTrue(Rules.locationAllowed(r,LocalDate.parse("2026-11-03")))
  assertNotNull(Rules.nextTime(r,at("2026-10-05T01:00")))
 }
 @Test fun multipleReservationsAndQueueKeysDoNotCollide() {
  val a=Reservation(id="a",time="09:00",dates=setOf("2026-10-05"))
  val b=a.copy(id="b")
  assertEquals(Rules.nextTime(a,at("2026-10-05T08:00")),Rules.nextTime(b,at("2026-10-05T08:00")))
  assertNotEquals(Rules.eventKey(a.id,"TIME","2026-10-05"),Rules.eventKey(b.id,"TIME","2026-10-05"))
 }
 @Test fun validationRejectsEmptyMessageAndInvalidRadius() {
  assertNotNull(Rules.validate(Reservation(message="")))
  assertNotNull(Rules.validate(Reservation(message="x",radius=-1.0)))
  assertNull(Rules.validate(Reservation(message="우유 사기",dates=setOf("2026-10-05"))))
 }
 @Test fun calendarSelectionSurvivesMonthNavigation() {
  val selected=setOf("2026-10-05","2026-11-03")
  assertEquals(setOf("2026-11-03"),Rules.toggleDate(selected,"2026-10-05",LocalDate.parse("2026-10-01")))
  assertEquals(selected,Rules.toggleDate(selected,"2026-09-30",LocalDate.parse("2026-10-01")))
 }
 @Test fun disabledDeletedAndRevisedAlarmsCannotDeliver() {
  val r=Reservation(id="a",revision=5,dates=setOf("2026-10-05"),trigger="BOTH")
  assertFalse(Rules.canDeliver(r,false,"TIME","2026-10-05",5))
  assertFalse(Rules.canDeliver(null,true,"TIME","2026-10-05",5))
  assertFalse(Rules.canDeliver(r,true,"TIME","2026-10-05",4))
  assertTrue(Rules.canDeliver(r,true,"TIME","2026-10-05",5))
  r.completed.add("TIME:2026-10-05")
  assertFalse(Rules.canDeliver(r,true,"TIME","2026-10-05",5))
  assertTrue(Rules.canDeliver(r,true,"LOCATION","2026-10-05",5))
 }
 @Test fun queueIsSerialAndDeduplicated() {
  val q=EventQueue()
  assertTrue(q.add("a")); assertTrue(q.add("b")); assertFalse(q.add("a"))
  assertEquals("a",q.poll());assertFalse(q.add("a"));q.done("a")
  assertEquals("b",q.poll());q.done("b");assertNull(q.poll())
 }
 @Test fun excessiveMessageCannotBreakSpeechEngine() {
  assertNotNull(Rules.validate(Reservation(message="a".repeat(4001))))
 }
 @Test fun reentryAllowedForDailyButSelectedDateIsOnce() {
  val daily=Reservation(trigger="LOCATION",place=Place("집",37.0,127.0),repeat="DAILY",completed=mutableSetOf("LOCATION:2026-10-05"))
  assertTrue(Rules.locationAllowed(daily,LocalDate.parse("2026-10-05")))
  assertFalse(Rules.locationAllowed(daily.copy(repeat="DATES",dates=setOf("2026-10-05")),LocalDate.parse("2026-10-05")))
 }
 @Test fun staleEditorCannotEraseCompletedEvents() {
  val stale=Reservation(id="a",message="수정",completed=mutableSetOf())
  val current=stale.copy(completed=mutableSetOf("TIME:2026-10-05"))
  assertEquals(setOf("TIME:2026-10-05"),Rules.mergeSaved(stale,current).completed)
 }
 @Test fun midnightDoesNotCreateEntryWhileStillInside() {
  val tracker=EntryTracker()
  assertTrue(tracker.update("a",1,100.0,300.0,"2026-10-05").fire)
  assertFalse(tracker.update("a",1,100.0,300.0,"2026-10-06").fire)
  assertFalse(tracker.update("a",1,400.0,300.0,"2026-10-06").fire)
  assertTrue(tracker.update("a",1,100.0,300.0,"2026-10-06").fire)
 }
 @Test fun editingDisabledOrPartCompletedReservationRemainsPossible() {
  val r=Reservation(enabled=false,trigger="BOTH",dates=setOf("2026-10-05"),completed=mutableSetOf("TIME:2026-10-05"))
  assertTrue(Rules.timeScheduleValid(r,at("2026-10-05T10:00"),false))
  assertTrue(Rules.timeScheduleValid(r,at("2026-10-05T10:00"),true))
  assertFalse(Rules.timeScheduleValid(r.copy(trigger="TIME"),at("2026-10-05T10:00"),true))
 }
 @Test fun permissionAndVolumeWarningsAreVisibleTogether() {
  val warnings=Rules.healthWarnings(false,0,"음성 엔진 없음")
  assertTrue(warnings.contains("알림 권한 / 시스템 알림 설정 필요"))
  assertTrue(warnings.contains("알람 볼륨 0 · 소리가 들리지 않을 수 있음"))
  assertTrue(warnings.contains("음성 엔진 없음"))
 }
 @Test fun savingCompletedMessageRearmsBothConditionsOnSameDate() {
  val current=Reservation(id="done",message="전 메시지",trigger="BOTH",place=Place("회사",37.0,127.0),dates=setOf("2026-10-05"),time="13:00",revision=1,completed=mutableSetOf("TIME:2026-10-05","LOCATION:2026-10-05"))
  val saved=Rules.mergeSaved(current.copy(message="수정 메시지",revision=2),current,true)
  assertEquals(at("2026-10-05T13:00"),Rules.nextTime(saved,at("2026-10-05T12:00")))
  assertTrue(Rules.canDeliver(saved,true,"TIME","2026-10-05",2))
  assertTrue(Rules.canDeliver(saved,true,"LOCATION","2026-10-05",2))
  assertFalse(Rules.canDeliver(saved,true,"TIME","2026-10-05",1))
  val tracker=EntryTracker();assertTrue(tracker.update(current.id,1,100.0,300.0,"2026-10-05").fire)
  assertTrue(tracker.update(saved.id,2,100.0,300.0,"2026-10-05").fire)
 }
 @Test fun togglingReservationPreservesCompletionRecordedWhileScreenWasOpen() {
  val stale=Reservation(id="done",revision=1)
  val current=stale.copy(completed=mutableSetOf("TIME:2026-10-05"))
  val saved=Rules.mergeSaved(stale.copy(enabled=false,revision=2),current)
  assertEquals(current.completed,saved.completed)
 }
 @Test fun rebookedLocationCanQueueWhileOldRevisionIsPlaying() {
  val q=EventQueue()
  val old=Rules.eventKey("a","LOCATION","2026-10-05",1)
  val revised=Rules.eventKey("a","LOCATION","2026-10-05",2)
  assertTrue(q.add(old));assertEquals(old,q.poll())
  assertTrue(q.add(revised));assertFalse(q.add(revised))
  q.done(old);assertEquals(revised,q.poll())
 }
 @Test fun watchModeUsesNotificationsOnlyWhenScreenIsOff() {
  assertTrue(Rules.notificationOnly(true,false,false))
  assertFalse(Rules.notificationOnly(true,true,false))
  assertFalse(Rules.notificationOnly(false,false,false))
  assertFalse(Rules.notificationOnly(true,false,true))
 }
 @Test fun cachedLocationMustBeRecentAccurateAndNotFromFuture(){
  assertTrue(Rules.locationUsable(120_000_000_000L,11.0))
  assertFalse(Rules.locationUsable(120_000_000_001L,11.0))
  assertFalse(Rules.locationUsable(-1,11.0))
  assertFalse(Rules.locationUsable(0,101.0))
  assertFalse(Rules.locationUsable(0,Double.NaN))
 }
 @Test fun locationModeIgnoresClockAndRearmCanDeliverInsideToday(){
  val today=LocalDate.parse("2026-10-06")
  val r=Reservation(id="stationary",message="알림",trigger="LOCATION",time="09:00",place=Place("회사",37.4060459,127.0896802),dates=setOf(today.toString()),radius=300.0,revision=1)
  assertTrue(Rules.locationAllowed(r,today));assertNull(Rules.nextTime(r,at("2026-10-06T07:47")))
  val tracker=EntryTracker();assertTrue(tracker.update(r.id,1,75.0,r.radius,today.toString()).fire)
  val saved=Rules.mergeSaved(r.copy(revision=2),r,true)
  assertTrue(tracker.update(saved.id,saved.revision,75.0,saved.radius,today.toString()).fire)
  assertTrue(Rules.canDeliver(saved,true,"LOCATION",today.toString(),2))
 }
 @Test fun locationDiagnosticDistinguishesDateCompletionAndEntry(){
  val d=LocalDate.parse("2026-10-06");val r=Reservation(trigger="LOCATION",dates=setOf(d.toString()))
  assertEquals("반경 안 · 알림 요청",Rules.locationDiagnostic(r,d,75.0,Entry(true,true)))
  assertEquals("반경 안 · 이미 진입 처리됨",Rules.locationDiagnostic(r,d,75.0,Entry(true,false)))
  assertEquals("반경 밖",Rules.locationDiagnostic(r,d,400.0,Entry(false,false)))
  assertEquals("오늘은 선택한 날짜/요일이 아님",Rules.locationDiagnostic(r,d.plusDays(1),75.0,Entry(true,true)))
  assertEquals("오늘 위치 알림 실행 기록 있음",Rules.locationDiagnostic(r.copy(completed=mutableSetOf("LOCATION:$d")),d,75.0,Entry(true,true)))
 }
}
