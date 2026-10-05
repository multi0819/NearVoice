package com.multi0819.nearvoice
import org.junit.Assert.*
import org.junit.Test
class MapsShareTest {
 @Test fun placeCoordinatesWinOverViewport(){val p=MapsShare.parse("https://www.google.com/maps/place/Office/@1,2,15z/data=!3d37.4!4d127.1")!!;assertEquals(37.4,p.latitude,0.00001);assertEquals(127.1,p.longitude,0.00001)}
 @Test fun encodedCoordinateQuery(){assertEquals(37.4,MapsShare.parse("https://maps.google.com/?q=37.4%2C127.1")!!.latitude,0.00001)}
 @Test fun namesAndViewportAreNotCoordinates(){assertNull(MapsShare.parse("https://www.google.com/maps/place/Office/@37.4,127.1,15z"));assertNull(MapsShare.parse("https://www.google.com/maps/search/?api=1&query=Office"))}
 @Test fun rejectsForeignUrlsAndInvalidCoordinates(){assertNull(MapsShare.parse("https://www.google.com.evil.test/maps/?q=37,127"));assertNull(MapsShare.parse("https://www.google.com/maps/?q=91,127"));assertFalse(MapsShare.allowed("https://www.google.com/url?q=https://evil.test"))}
 @Test fun viewportQueryDoesNotBecomeDestination(){assertNull(MapsShare.parse("https://maps.google.com/?q=Office&ll=37.4,127.1"))}
 @Test fun addressIsExtractedFromPlaceIdOnlyLink(){assertEquals("경기도 성남시 수정구 금토로 52",MapsShare.searchQuery("https://www.google.com/maps/place/%EA%B2%BD%EA%B8%B0%EB%8F%84+%EC%84%B1%EB%82%A8%EC%8B%9C+%EC%88%98%EC%A0%95%EA%B5%AC+%EA%B8%88%ED%86%A0%EB%A1%9C+52+Soul+Hub+Bldg/data=!4m2!3m1!1s0x357ca70011cbdf83:0xced66a90da9f4631"))}
 @Test fun sharedNameIsPreserved(){assertEquals("판교역",MapsShare.parse("판교역\nhttps://maps.google.com/?q=37.4,127.1")!!.label)}
 @Test fun resolvesUnsignedCidAndExactEmbedRecord(){
  val cid=GooglePlaceInfo.cid("https://www.google.com/maps/place/Office/data=!1s0x357ca70011cbdf83:0xced66a90da9f4631")!!
  assertEquals("14904217187204941361",cid)
  val html="""initEmbed([[[3169,1,2]],[["0x357ca70011cbdf83:0xced66a90da9f4631","솔브레인 본사",[37.4060459,127.0896802],"14904217187204941361"]]])"""
  val p=GooglePlaceInfo.parse(html,cid)!!
  assertEquals(37.4060459,p.latitude,0.0000001);assertEquals(127.0896802,p.longitude,0.0000001)
  assertNull(GooglePlaceInfo.parse(html,"123"))
 }
 @Test fun embedRejectsViewportAndInvalidCoordinates(){
  assertNull(GooglePlaceInfo.parse("initEmbed([[[3169,127.1,37.4]]])","123"))
  assertNull(GooglePlaceInfo.parse("""["0x1:0x7b","Office",[91,127],"123"]""","123"))
  assertNull(GooglePlaceInfo.cid("https://evil.test/maps?cid=123"))
 }
 @Test fun intersectionRecordDoesNotRequireRepeatedDecimalCid(){
  val url="https://www.google.com/maps/place/Intersection/data=!4m2!3m1!1s0x357b50f22e37d939:0x2b150b48f6526187!18m1!1e1"
  val cid=GooglePlaceInfo.cid(url)!!;assertEquals("3104399926139969927",cid)
  val html="""initEmbed([[[3174,127.1520718,37.2792952]],[["0x357b50f22e37d939:0x2b150b48f6526187","Yongin-si, 동백이마트사거리",[37.2792952,127.1520718]],"동백이마트사거리",["Yongin-si"]]])"""
  val p=GooglePlaceInfo.parse(html,cid)!!
  assertEquals(37.2792952,p.latitude,0.0000001);assertEquals(127.1520718,p.longitude,0.0000001)
  assertNull(GooglePlaceInfo.parse(html,"14904217187204941361"))
 }
 @Test fun conflictingDecimalCidIsRejected(){
  val html="""["0x357b50f22e37d939:0x2b150b48f6526187","Intersection",[37.2792952,127.1520718],"123"]"""
  assertNull(GooglePlaceInfo.parse(html,"3104399926139969927"))
 }
 @Test fun emartDongbaekBusinessRecordIsAccepted(){
  val url="https://www.google.com/maps/place/Emart/data=!1s0x357b50f3c1ee829d:0xce16e47584ba475f!18m1!1e1"
  val cid=GooglePlaceInfo.cid(url)!!;assertEquals("14850308014689896287",cid)
  val html="""[["0x357b50f3c1ee829d:0xce16e47584ba475f","이마트 동백점",[37.2779159,127.151605],"14850308014689896287"],"이마트 동백점"]"""
  val p=GooglePlaceInfo.parse(html,cid)!!;assertEquals(37.2779159,p.latitude,0.0000001);assertEquals(127.151605,p.longitude,0.0000001)
 }
}
