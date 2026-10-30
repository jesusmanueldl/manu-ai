package com.manu.ai;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.StatFs;
import java.text.SimpleDateFormat;
import java.util.*;

public class IntentRouter {
 private final Context context;
 public IntentRouter(Context c){context=c.getApplicationContext();}
 public IntentResult route(String raw){String s=raw.trim(),l=s.toLowerCase(Locale.ROOT);
  if(l.equals("hora")||l.contains("qué hora")||l.contains("que hora"))return answer("Son las "+new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date()));
  if(l.contains("ram")||l.contains("memoria disponible")||l.contains("memoria queda")||l.contains("cuánta memoria")||l.contains("cuanta memoria"))return answer(deviceMemory());
  if(l.contains("espacio")||l.contains("almacenamiento")||l.contains("storage"))return answer(storage());
  if(l.contains("batería")||l.contains("bateria"))return answer(battery());
  if(l.contains("cpu")||l.contains("procesador"))return answer(cpu());
  if(l.startsWith("abre ")||l.startsWith("abrir ")){String app=s.substring(s.indexOf(' ')+1).trim();return new IntentResult(IntentResult.Kind.OPEN_APP,"Voy a abrir "+app+".",app,null);}
  if(l.startsWith("manda mensaje")||l.startsWith("envía mensaje")||l.startsWith("envia mensaje")){String body=s.substring(s.indexOf(' ')+1).trim();return new IntentResult(IntentResult.Kind.SMS_CONFIRM,"Preparé el mensaje. Confirma antes de enviarlo.",null,body);}
  if(l.contains("qué aplicaciones")||l.contains("que aplicaciones")||l.contains("apps instaladas"))return answer(installedApps());
  if(l.contains("quién eres")||l.contains("quien eres"))return answer("Soy MANU: Modelo Autónomo Neural Útil. Trabajo localmente y puedo consultar funciones del teléfono de forma segura.");
  return new IntentResult(IntentResult.Kind.UNKNOWN,"",null,null);
 }
 private IntentResult answer(String text){return new IntentResult(IntentResult.Kind.ANSWER,text,null,null);}
 private String deviceMemory(){ActivityManager.MemoryInfo m=new ActivityManager.MemoryInfo();((ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(m);return String.format(Locale.getDefault(),"RAM disponible: %s de %s (%.0f%% disponible).",fmt(m.availMem),fmt(m.totalMem),100d*m.availMem/m.totalMem);}
 private String storage(){StatFs s=new StatFs(Environment.getDataDirectory().getPath());long free=s.getAvailableBytes(),total=s.getTotalBytes();return String.format(Locale.getDefault(),"Almacenamiento interno libre: %s de %s.",fmt(free),fmt(total));}
 private String battery(){BatteryManager b=(BatteryManager)context.getSystemService(Context.BATTERY_SERVICE);int p=b.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);return p>0?"Batería: "+p+"%.":"No pude leer el porcentaje de batería.";}
 private String cpu(){return "Procesador: "+android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL+" ("+Runtime.getRuntime().availableProcessors()+" núcleos disponibles para la app).";}
 private String installedApps(){PackageManager pm=context.getPackageManager();List<ApplicationInfo> apps=pm.getInstalledApplications(PackageManager.GET_META_DATA);ArrayList<String> names=new ArrayList<>();for(ApplicationInfo a:apps)if(pm.getLaunchIntentForPackage(a.packageName)!=null)names.add(pm.getApplicationLabel(a).toString());Collections.sort(names,String.CASE_INSENSITIVE_ORDER);return "Aplicaciones disponibles: "+String.join(", ",names)+".";}
 private String fmt(long n){if(n<1024L*1024L)return n/1024+" KB";if(n<1024L*1024L*1024L)return String.format(Locale.getDefault(),"%.1f MB",n/1048576d);return String.format(Locale.getDefault(),"%.1f GB",n/1073741824d);}
}
