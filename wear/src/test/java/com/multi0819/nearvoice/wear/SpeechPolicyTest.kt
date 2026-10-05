package com.multi0819.nearvoice.wear
import org.junit.Test
import org.junit.Assert.*
class SpeechPolicyTest {
 @Test fun timelyMessagesCanBeRead(){assertTrue(SpeechPolicy.accepts(1000000,1000010,"예약 메시지"))}
 @Test fun staleAndInvalidMessagesAreRejected(){assertFalse(SpeechPolicy.accepts(1000000,1120001,"예약"));assertFalse(SpeechPolicy.accepts(1000000,969999,"예약"));assertFalse(SpeechPolicy.accepts(0,1000000,"예약"));assertFalse(SpeechPolicy.accepts(1000000,1000000," "));assertFalse(SpeechPolicy.accepts(1000000,1000000,"x".repeat(3901)))}
}
