package com.multi0819.nearvoice.wear
object SpeechPolicy {
 fun accepts(sentAt:Long,now:Long,message:String):Boolean = sentAt>0&&sentAt<=now+30000&&sentAt>=now-120000&&message.isNotBlank()&&message.length<=3900
}
