package com.multi0819.nearvoice
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.*
object Ui {
 val bg=Color.rgb(8,15,25);val panel=Color.rgb(16,28,43);val accent=Color.rgb(45,223,199);val text=Color.rgb(228,240,246);val muted=Color.rgb(138,164,186)
 fun dp(c:Context,n:Int)=(n*c.resources.displayMetrics.density).toInt()
 fun shape(color:Int=panel,border:Int=Color.rgb(39,62,79))=GradientDrawable().apply{setColor(color);cornerRadius=18f;setStroke(1,border)}
 fun column(c:Context)=LinearLayout(c).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(c,16),dp(c,12),dp(c,16),dp(c,12))}
 fun row(c:Context)=LinearLayout(c).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
 fun label(c:Context,s:String,size:Float=15f,color:Int=text)=TextView(c).apply{setText(s);textSize=size;setTextColor(color);setPadding(0,dp(c,7),0,dp(c,7))}
 fun button(c:Context,s:String,action:()->Unit)=Button(c).apply{setText(s);isAllCaps=false;setTextColor(accent);textSize=14f;background=shape();minHeight=dp(c,46);setPadding(dp(c,10),dp(c,4),dp(c,10),dp(c,4));setOnClickListener{action()}}
 fun edit(c:Context,hint:String,value:String="",multiline:Boolean=false)=EditText(c).apply{setHint(hint);setText(value);setTextColor(Ui.text);setHintTextColor(muted);textSize=16f;background=shape();setPadding(dp(c,12),dp(c,12),dp(c,12),dp(c,12));isSingleLine=!multiline;if(multiline){minLines=3;maxLines=8;gravity=Gravity.TOP}}
 fun space(c:Context,n:Int=10)=Space(c).apply{layoutParams=LinearLayout.LayoutParams(1,dp(c,n))}
 fun toggle(c:Context,s:String,on:Boolean)=Switch(c).apply{text=s;isChecked=on;setTextColor(Ui.text);setPadding(0,dp(c,9),0,dp(c,9))}
 fun spinner(c:Context,items:List<String>,position:Int=0)=Spinner(c).apply{adapter=object:ArrayAdapter<String>(c,android.R.layout.simple_spinner_dropdown_item,items){override fun getView(p:Int,v:View?,g:ViewGroup):View{return super.getView(p,v,g).apply{(this as TextView).setTextColor(Ui.text);setPadding(dp(c,8),dp(c,10),dp(c,8),dp(c,10))}}};setSelection(position);background=shape()}
 fun weighted(row:LinearLayout,view:View){val lp=LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f);lp.setMargins(dp(row.context,3),dp(row.context,3),dp(row.context,3),dp(row.context,3));row.addView(view,lp)}
 fun page(a:Activity,title:String):LinearLayout {
  val outer=LinearLayout(a).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg)}
  outer.setOnApplyWindowInsetsListener{v,insets->if(android.os.Build.VERSION.SDK_INT>=30){val b=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(b.left,b.top,b.right,b.bottom)}else {@Suppress("DEPRECATION") v.setPadding(insets.systemWindowInsetLeft,insets.systemWindowInsetTop,insets.systemWindowInsetRight,insets.systemWindowInsetBottom)};insets}
  val scroll=ScrollView(a);val content=column(a);content.addView(label(a,title,25f,accent).apply{typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL)})
  scroll.addView(content);outer.addView(scroll,LinearLayout.LayoutParams(-1,-1));a.setContentView(outer);return content
 }
}
