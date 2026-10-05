package com.multi0819.nearvoice
import java.net.HttpURLConnection
import java.net.URI
data class SharedMapResult(val place:Place?=null,val query:String?=null)
object MapsLinkResolver {
 fun resolve(text:String):SharedMapResult? {
  MapsShare.parse(text)?.let{return SharedMapResult(place=it)}
  var url=MapsShare.url(text)?:return null
  repeat(5){
   if(!MapsShare.allowed(url))return null
   val connection=URI(url).toURL().openConnection() as HttpURLConnection
   try {
    connection.instanceFollowRedirects=false;connection.connectTimeout=5000;connection.readTimeout=5000
    connection.setRequestProperty("User-Agent","Mozilla/5.0 NearVoice/1.0")
    val status=connection.responseCode
    if(status in 300..399){val location=connection.getHeaderField("Location")?:return null;url=URI(url).resolve(location).toString();if(!MapsShare.allowed(url))return null;MapsShare.parse(url)?.let{return SharedMapResult(place=it.copy(label=if(MapsShare.label(text)!="구글 지도 장소")MapsShare.label(text)else it.label))};MapsShare.searchQuery(url)?.let{return SharedMapResult(query=it)}}else return MapsShare.searchQuery(url)?.let{SharedMapResult(query=it)}
   }finally{connection.disconnect()}
  };return null
 }
}
