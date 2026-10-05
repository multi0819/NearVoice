package com.multi0819.nearvoice
import java.math.BigInteger
import java.net.URI
import java.net.URLDecoder
/** Only accepts the requested place record, never map viewport coordinates. */
object GooglePlaceInfo {
 fun cid(url:String):String? {
  if(!MapsShare.allowed(url))return null
  val decoded=runCatching{URLDecoder.decode(url,"UTF-8")}.getOrNull()?:return null
  val hex=Regex("!1s0x[0-9a-fA-F]+:0x([0-9a-fA-F]{1,16})(?:!|[/?&#]|$)").find(decoded)?.groupValues?.get(1)
  val decimal=Regex("(?:^|&)cid=(\\d{1,20})(?:&|$)").find(URI(url).rawQuery?:"")?.groupValues?.get(1)
  val value=runCatching{if(hex!=null)BigInteger(hex,16)else BigInteger(decimal?:return null)}.getOrNull()?:return null
  return value.takeIf{it.signum()>0&&it.bitLength()<=64}?.toString()
 }
 fun parse(html:String,cid:String):Place? {
  if(!cid.matches(Regex("\\d{1,20}")))return null
  val number="(-?\\d+(?:\\.\\d+)?)"
  val record=Regex("\\[\\s*\"0x[0-9a-fA-F]+:0x([0-9a-fA-F]+)\"\\s*,\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\s*,\\s*\\[\\s*$number\\s*,\\s*$number\\s*](?:\\s*,\\s*\"(\\d{1,20})\")?\\s*]")
  for(m in record.findAll(html)){
   if(runCatching{BigInteger(m.groupValues[1],16).toString()}.getOrNull()!=cid)continue
   if(m.groupValues[5].isNotEmpty()&&m.groupValues[5]!=cid)continue
   val lat=m.groupValues[3].toDoubleOrNull()?:continue;val lon=m.groupValues[4].toDoubleOrNull()?:continue
   if(!lat.isFinite()||!lon.isFinite()||lat !in -90.0..90.0||lon !in -180.0..180.0)continue
   val label=m.groupValues[2].replace(Regex("\\\\u([0-9a-fA-F]{4})")){it.groupValues[1].toInt(16).toChar().toString()}.replace("\\\"","\"").replace("\\/","/").replace("\\\\","\\").take(500)
   return Place(label,lat,lon)
  };return null
 }
}
