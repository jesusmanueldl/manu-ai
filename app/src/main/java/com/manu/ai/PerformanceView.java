package com.manu.ai;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

public class PerformanceView extends View {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG); private final RectF box=new RectF(); private boolean running; private long started; private int frame;
 public PerformanceView(Context c){super(c);paint.setTypeface(android.graphics.Typeface.create("sans",0));setContentDescription("Actividad de inferencia local");}
 public void start(){running=true;started=System.currentTimeMillis();frame=0;invalidate();}
 public void stop(){running=false;invalidate();}
 protected void onDraw(Canvas c){super.onDraw(c);float d=getResources().getDisplayMetrics().density;paint.setColor(Color.rgb(245,246,247));box.set(0,0,getWidth(),getHeight());c.drawRoundRect(box,16*d,16*d,paint);paint.setColor(Color.rgb(116,119,124));paint.setTextSize(12*d);String label=running?"MANU está pensando · inferencia local":"Inferencia local";c.drawText(label,16*d,22*d,paint);paint.setColor(Color.rgb(22,125,100));int n=18;float gap=5*d,w=(getWidth()-32*d-(n-1)*gap)/n;for(int i=0;i<n;i++){float wave=(float)(Math.sin((i+frame)*.72)*.5+.5);float h=(running?(10+wave*28):12)*d;float left=16*d+i*(w+gap);c.drawRoundRect(left,getHeight()-16*d-h,left+w,getHeight()-16*d,3*d,3*d,paint);}if(running){frame++;postInvalidateDelayed(90);}}
}
