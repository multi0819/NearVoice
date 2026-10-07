package com.multi0819.nearvoice
import org.junit.Test
import org.junit.Assert.*
import java.time.*
class ScanWindowsTest {
 private fun at(time:String)=LocalDateTime.parse("2026-10-08T$time").atZone(ZoneId.of("Asia/Seoul"))
 private val reservation=Reservation(trigger="LOCATION",dates=setOf("2026-10-08"))
 private val windows=listOf(ScanWindow("07:00","09:00"),ScanWindow("17:00","20:00"))
 @Test fun startIncludedEndExcludedAndMultipleWindows(){assertTrue(ScanWindows.active(true,windows,listOf(reservation),at("07:00")));assertFalse(ScanWindows.active(true,windows,listOf(reservation),at("09:00")));assertTrue(ScanWindows.active(true,windows,listOf(reservation),at("18:00")));assertFalse(ScanWindows.active(true,windows,listOf(reservation),at("12:00")))}
 @Test fun dateAndCompletedReservationsStopScanning(){assertFalse(ScanWindows.active(true,windows,listOf(reservation.copy(dates=setOf("2026-10-09"))),at("08:00")));assertFalse(ScanWindows.active(true,windows,listOf(reservation.copy(completed=mutableSetOf("LOCATION:2026-10-08"))),at("08:00")));assertFalse(ScanWindows.active(false,windows,listOf(reservation.copy(trigger="TIME")),at("08:00")))}
 @Test fun overnightStillUsesCurrentReservationDate(){val night=listOf(ScanWindow("22:00","06:00"));assertTrue(ScanWindows.active(true,night,listOf(reservation),at("23:00")));assertTrue(ScanWindows.active(true,night,listOf(reservation),at("05:00")));assertFalse(ScanWindows.active(true,night,listOf(reservation),at("06:00")));assertFalse(ScanWindows.active(true,night,listOf(reservation),at("23:00").plusDays(1)))}
 @Test fun disabledRestrictionAllowsWholeSelectedDate(){assertTrue(ScanWindows.active(false,emptyList(),listOf(reservation),at("12:00")));assertFalse(ScanWindows.active(true,emptyList(),listOf(reservation),at("12:00")))}
 @Test fun nextBoundaryIncludesMidnightAndStrictlyFuture(){assertEquals(at("07:00"),ScanWindows.nextBoundary(true,windows,at("06:00")));assertEquals(at("09:00"),ScanWindows.nextBoundary(true,windows,at("07:00")));assertEquals(at("00:00").plusDays(1),ScanWindows.nextBoundary(true,windows,at("20:00")));assertEquals(at("00:00").plusDays(1),ScanWindows.nextBoundary(false,emptyList(),at("18:00")))}
 @Test(expected=IllegalArgumentException::class) fun equalEndpointsRejected(){ScanWindow("08:00","08:00")}
 @Test(expected=java.time.format.DateTimeParseException::class) fun invalidTimeRejected(){ScanWindow("25:00","08:00")}
}
