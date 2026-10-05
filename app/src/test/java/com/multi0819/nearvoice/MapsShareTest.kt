package com.multi0819.nearvoice
import org.junit.Assert.*
import org.junit.Test
class MapsShareTest {
 @Test fun placeCoordinatesWinOverViewport(){val p=MapsShare.parse("https://www.google.com/maps/place/Office/@1,2,15z/data=!3d37.4!4d127.1")!!;assertEquals(37.4,p.latitude,0.00001);assertEquals(127.1,p.longitude,0.00001)}
 @Test fun encodedCoordinateQuery(){assertEquals(37.4,MapsShare.parse("https://maps.google.com/?q=37.4%2C127.1")!!.latitude,0.00001)}
 @Test fun namesAndViewportAreNotCoordinates(){assertNull(MapsShare.parse("https://www.google.com/maps/place/Office/@37.4,127.1,15z"));assertNull(MapsShare.parse("https://www.google.com/maps/search/?api=1&query=Office"))}
 @Test fun rejectsForeignUrlsAndInvalidCoordinates(){assertNull(MapsShare.parse("https://www.google.com.evil.test/maps/?q=37,127"));assertNull(MapsShare.parse("https://www.google.com/maps/?q=91,127"));assertFalse(MapsShare.allowed("https://www.google.com/url?q=https://evil.test"))}
 @Test fun viewportQueryDoesNotBecomeDestination(){assertNull(MapsShare.parse("https://maps.google.com/?q=Office&ll=37.4,127.1"))}
 @Test fun sharedNameIsPreserved(){assertEquals("판교역",MapsShare.parse("판교역\nhttps://maps.google.com/?q=37.4,127.1")!!.label)}
}
