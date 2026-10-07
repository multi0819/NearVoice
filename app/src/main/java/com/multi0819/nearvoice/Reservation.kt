package com.multi0819.nearvoice
import java.time.LocalDate
import java.util.UUID

data class Place(val label:String,val latitude:Double,val longitude:Double)
data class Reservation(
 val id:String=UUID.randomUUID().toString(), val title:String="예약", val message:String="", val enabled:Boolean=true,
 val trigger:String="TIME", val place:Place?=null, val radius:Double=300.0, val dates:Set<String> = setOf(LocalDate.now().toString()),
 val repeat:String="DATES", val weekdays:Set<Int> = setOf(1,2,3,4,5,6,7), val time:String="09:00",
 val voice:Boolean=true, val vibration:Boolean=true, val soundUri:String?=null, val completed:MutableSet<String> = mutableSetOf(),
 val revision:Long=System.currentTimeMillis(), val times:Set<String> = emptySet()
)
data class Entry(val inside:Boolean,val fire:Boolean)
