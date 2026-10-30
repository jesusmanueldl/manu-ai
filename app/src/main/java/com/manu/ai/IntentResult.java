package com.manu.ai;
public class IntentResult { public enum Kind{ANSWER,OPEN_APP,SMS_CONFIRM,UNKNOWN}; public final Kind kind; public final String text,a,b; public IntentResult(Kind k,String t,String x,String y){kind=k;text=t;a=x;b=y;} }
