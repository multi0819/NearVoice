package com.multi0819.nearvoice
import android.app.*
import android.content.*
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.webkit.*
import android.widget.*
import java.util.Locale
import java.util.concurrent.Executors
import org.json.JSONObject
@android.annotation.SuppressLint("MissingPermission")
class PlacePicker:Activity(){
 private lateinit var web:WebView;private lateinit var info:TextView;private lateinit var name:EditText
 private var mapReady=false;private var selected:Place?=null;private val executor=Executors.newSingleThreadExecutor()
 override fun onCreate(b:Bundle?){super.onCreate(b);val c=Ui.page(this,"목적지 선택")
  val initial=runCatching{Codec.place(JSONObject(intent.getStringExtra("place")?:""))}.getOrNull();selected=initial
  name=Ui.edit(this,"저장할 장소 이름 / 주소",initial?.label?:"");c.addView(name)
  val searchRow=Ui.row(this);Ui.weighted(searchRow,Ui.button(this,"주소 검색"){search(name.text.toString())});Ui.weighted(searchRow,Ui.button(this,"현재 위치"){val l=LocationService.latest
   if(l!=null){select(Place("현재 위치",l.latitude,l.longitude));center(l.latitude,l.longitude)}else if(LocationService.permitted(this)){val m=getSystemService(android.location.LocationManager::class.java)
    val last=listOf(android.location.LocationManager.GPS_PROVIDER,android.location.LocationManager.NETWORK_PROVIDER).mapNotNull{runCatching{m.getLastKnownLocation(it)}.getOrNull()}.maxByOrNull{it.time}
    if(last!=null){select(Place("현재 위치",last.latitude,last.longitude));center(last.latitude,last.longitude)}else info.text="현재 위치가 없습니다. 주소 검색이나 지도 선택을 사용하세요."
   }else info.text="위치 권한을 먼저 허용하세요. 주소 검색과 지도 선택은 바로 가능합니다."
  });c.addView(searchRow)
  c.addView(Ui.button(this,"구글 지도에서 찾기"){openGoogleMaps()})
  c.addView(Ui.label(this,"구글 지도에서 장소 검색 → 공유 → NearVoice를 선택하세요.",13f,Ui.muted))
  info=Ui.label(this,initial?.let{"%.5f, %.5f".format(it.latitude,it.longitude)}?:"주소를 검색하거나 지도를 눌러 지점을 선택하세요.",14f,Ui.accent);c.addView(info)
  web=WebView(this);web.settings.javaScriptEnabled=true;web.settings.allowFileAccess=false;web.settings.allowContentAccess=false;web.settings.cacheMode=WebSettings.LOAD_DEFAULT
  web.settings.userAgentString="NearVoice/1.0 (+https://github.com/multi0819/NearVoice) ${web.settings.userAgentString}"
  web.settings.mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
  web.webViewClient=object:WebViewClient(){override fun shouldOverrideUrlLoading(v:WebView,request:WebResourceRequest):Boolean{
   val u=request.url
   if(u.scheme=="nearvoice"&&u.host=="pick"&&v.url?.startsWith("https://nearvoice.local/")==true){val lat=u.getQueryParameter("lat")?.toDoubleOrNull();val lon=u.getQueryParameter("lon")?.toDoubleOrNull()
    if(lat!=null&&lon!=null){val p=Place(name.text.toString().ifBlank{"지도 선택 지점"},lat,lon);if(Rules.validPlace(p))select(p)}
   }else if(u.scheme=="nearvoice"&&u.host=="error"){info.text="지도 연결을 확인하세요. 주소 검색은 별도로 사용할 수 있습니다."}
   return true
  }
  override fun onPageFinished(v:WebView,url:String){mapReady=true;selected?.let{center(it.latitude,it.longitude)}}
  override fun onReceivedError(v:WebView,request:WebResourceRequest,error:WebResourceError){if(request.isForMainFrame)info.text="지도를 불러오지 못했습니다. 인터넷 연결을 확인하세요."}
  }
  c.addView(web,LinearLayout.LayoutParams(-1,Ui.dp(this,380)))
  c.addView(Ui.label(this,"© OpenStreetMap contributors · 지도 이용 시 좌표와 네트워크 정보가 지도 제공자에게 전달됩니다.",12f,Ui.muted))
  val bottom=Ui.row(this);Ui.weighted(bottom,Ui.button(this,"취소"){finish()});Ui.weighted(bottom,Ui.button(this,"이 위치 선택"){val p=selected
   if(p==null)Toast.makeText(this,"지도에서 위치를 선택하세요.",Toast.LENGTH_LONG).show()else{val label=name.text.toString().trim().ifEmpty{p.label};setResult(RESULT_OK,Intent().putExtra("place",Codec.place(p.copy(label=label)).toString()));finish()}
  });c.addView(bottom)
  val js=assets.open("leaflet.js").bufferedReader().use{it.readText()};val css=assets.open("leaflet.css").bufferedReader().use{it.readText()}
  val lat=initial?.latitude?:37.5665;val lon=initial?.longitude?:126.9780
  val html="""<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><meta http-equiv="Content-Security-Policy" content="default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src https://tile.openstreetmap.org data:;"><style>$css html,body,#map{height:100%;margin:0;background:#101c2b}.leaflet-container{background:#101c2b}</style></head><body><div id="map"></div><script>$js</script><script>
   const map=L.map('map').setView([$lat,$lon],15);let marker;
   const tiles=L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'© OpenStreetMap contributors'}).addTo(map);
   tiles.on('tileerror',()=>{location.href='nearvoice://error';});
   function pin(lat,lon){if(marker)map.removeLayer(marker);marker=L.circleMarker([lat,lon],{radius:10,color:'#2ddfc7',fillColor:'#2ddfc7',fillOpacity:0.8}).addTo(map);}
   function center(lat,lon){map.setView([lat,lon],16);pin(lat,lon);}
   map.on('click',e=>{pin(e.latlng.lat,e.latlng.lng);location.href='nearvoice://pick?lat='+e.latlng.lat+'&lon='+e.latlng.lng;});
   ${if(initial!=null)"pin($lat,$lon);"else ""}
   </script></body></html>"""
  web.loadDataWithBaseURL("https://nearvoice.local/map.html",html,"text/html","UTF-8",null)
  intent.getStringExtra("shared_map")?.let{shared->selected=null;name.setText(MapsShare.label(shared).takeIf{it!="구글 지도 장소"}?:"");info.text="공유한 장소 확인 중…";executor.execute{val result=runCatching{MapsLinkResolver.resolve(shared)}.getOrNull();runOnUiThread{if(isDestroyed)return@runOnUiThread;val p=result?.place;if(p!=null){select(p);name.setText(p.label);center(p.latitude,p.longitude)}else if(!result?.query.isNullOrBlank()){name.setText(result!!.query);info.text="공유한 주소의 위치를 검색합니다. 결과를 확인해 선택하세요.";search(result.query!!)}else{info.text="공유한 장소를 확인하지 못했습니다. 주소 검색이나 지도 선택을 사용하세요."}}}}
  if(b==null&&intent.getBooleanExtra("open_google",false))openGoogleMaps()
 }
 private fun openGoogleMaps(){val query=name.text.toString().trim();val url=if(query.isEmpty())"https://www.google.com/maps"else "https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}";val i=Intent(Intent.ACTION_VIEW,Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).setPackage("com.google.android.apps.maps");try{startActivity(i)}catch(_:ActivityNotFoundException){try{startActivity(i.setPackage(null))}catch(_:ActivityNotFoundException){Toast.makeText(this,"구글 지도 앱이나 브라우저를 설치하세요.",Toast.LENGTH_LONG).show()}}}
 private fun select(p:Place){selected=p;info.text="선택됨 · %.5f, %.5f".format(p.latitude,p.longitude);if(name.text.isBlank())name.setText(p.label)}
 private fun center(lat:Double,lon:Double){if(!mapReady)return;web.evaluateJavascript("center($lat,$lon)",null)}
 private fun search(query:String){if(query.isBlank()){info.text="주소나 장소 이름을 입력하세요.";return};info.text="주소 검색 중…"
  executor.execute{val results=runCatching{@Suppress("DEPRECATION") Geocoder(this,Locale.KOREAN).getFromLocationName(query,5)}
   runOnUiThread{if(isDestroyed)return@runOnUiThread;val list=results.getOrNull();if(list.isNullOrEmpty()){info.text="주소를 찾지 못했습니다. 상세 주소를 입력하거나 지도를 선택하세요.";return@runOnUiThread}
    AlertDialog.Builder(this).setTitle("주소 결과 선택").setItems(list.map{it.getAddressLine(0)?:query}.toTypedArray()){_,n->val a=list[n];val p=Place(a.getAddressLine(0)?:query,a.latitude,a.longitude);select(p);name.setText(p.label);center(p.latitude,p.longitude)}.setNegativeButton("취소",null).show()
   }
  }
 }
 override fun onDestroy(){executor.shutdownNow();web.destroy();super.onDestroy()}
}
