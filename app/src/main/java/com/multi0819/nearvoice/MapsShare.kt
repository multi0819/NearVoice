package com.multi0819.nearvoice
import java.net.URI
import java.net.URLDecoder
object MapsShare {
 private val number="(-?\\d+(?:\\.\\d+)?)"
 fun url(text:String):String?=Regex("https://[^\\s<>\"]+").findAll(text.take(12000)).map{it.value.trimEnd(')',']','.',',')}.firstOrNull{allowed(it)}
 fun allowed(url:String):Boolean=runCatching{val u=URI(url);u.scheme=="https"&&u.userInfo==null&&(u.port==-1||u.port==443)&&when(u.host?.lowercase()){
  "maps.app.goo.gl"->true
  "goo.gl"->u.path.startsWith("/maps/")
  "maps.google.com","maps.google.co.kr"->true
  "google.com","www.google.com","google.co.kr","www.google.co.kr"->u.path=="/maps"||u.path.startsWith("/maps/")
  else->false
 }}.getOrDefault(false)
 fun label(text:String):String=text.lineSequence().map{it.trim()}.firstOrNull{it.isNotEmpty()&&!it.contains("https://") }?.take(200)?:"구글 지도 장소"
 fun parse(text:String):Place? {
  val link=url(text)?:return null;val u=runCatching{URI(link)}.getOrNull()?:return null
  if(u.path.startsWith("/maps/dir"))return null
  val decoded=runCatching{URLDecoder.decode(link,"UTF-8")}.getOrDefault(link)
  val precise=Regex("!3d${number}!4d${number}").findAll(decoded).lastOrNull()
  val pair=if(precise!=null)precise.groupValues[1] to precise.groupValues[2] else{
   val params=u.rawQuery.orEmpty().split('&').mapNotNull{part->val bits=part.split('=',limit=2);if(bits.size==2)bits[0] to runCatching{URLDecoder.decode(bits[1],"UTF-8")}.getOrDefault("") else null}.toMap()
   val query=listOf("query","q").mapNotNull{params[it]}.firstOrNull{Regex("^${number},\\s*${number}$").matches(it)}?:return null
   val bits=query.split(',');bits[0] to bits[1].trim()
  }
  val lat=pair.first.toDoubleOrNull()?:return null;val lon=pair.second.toDoubleOrNull()?:return null
  if(!lat.isFinite()||!lon.isFinite()||lat !in -90.0..90.0||lon !in -180.0..180.0)return null
  val pathName=Regex("/maps/place/([^/]+)").find(decoded)?.groupValues?.get(1)
  return Place(if(label(text)!="구글 지도 장소")label(text)else pathName?.take(200)?:"구글 지도 장소",lat,lon)
 }
}
