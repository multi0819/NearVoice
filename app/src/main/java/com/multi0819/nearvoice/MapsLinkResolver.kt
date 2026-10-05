package com.multi0819.nearvoice
import java.net.HttpURLConnection
import java.net.URI
data class SharedMapResult(val place:Place)
object MapsLinkResolver {
 private const val MAX_BYTES=512*1024
 fun resolve(text:String):SharedMapResult? {
  MapsShare.parse(text)?.let{return SharedMapResult(it)}
  var url=MapsShare.url(text)?:return null
  repeat(5){
   if(!MapsShare.allowed(url))return null
   MapsShare.parse(url)?.let{return SharedMapResult(named(it,text))}
   GooglePlaceInfo.cid(url)?.let{cid->
    val html=readEmbed("https://maps.google.com/maps?cid=$cid&output=embed")?:return null
    return GooglePlaceInfo.parse(html,cid)?.let{SharedMapResult(named(it,text))}
   }
   val connection=open(url)
   try {
    if(connection.responseCode !in 300..399)return null
    val location=connection.getHeaderField("Location")?:return null
    url=URI(url).resolve(location).toString()
   }finally{connection.disconnect()}
  };return null
 }
 private fun named(place:Place,text:String)=place.copy(label=MapsShare.label(text).takeIf{it!="구글 지도 장소"}?:place.label)
 private fun open(url:String)=(URI(url).toURL().openConnection() as HttpURLConnection).apply{
  instanceFollowRedirects=false;connectTimeout=10000;readTimeout=10000
  setRequestProperty("User-Agent","Mozilla/5.0 NearVoice/1.0")
 }
 private fun readEmbed(initial:String):String? {
  var url=initial
  repeat(3){
   if(!MapsShare.allowed(url))return null
   val connection=open(url)
   try{
    val status=connection.responseCode
    if(status in 300..399){url=URI(url).resolve(connection.getHeaderField("Location")?:return null).toString()}
    else {
     if(status!=200)return null
     val bytes=connection.inputStream.use{input->
      val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
      while(output.size()<=MAX_BYTES){val count=input.read(buffer,0,minOf(buffer.size,MAX_BYTES+1-output.size()));if(count<0)break;output.write(buffer,0,count)}
      output.toByteArray()
     }
     if(bytes.size>MAX_BYTES)return null
     return bytes.toString(Charsets.UTF_8)
    }
   }finally{connection.disconnect()}
  };return null
 }
}
