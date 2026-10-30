package com.manu.ai;
import android.content.Context;import android.os.Handler;import android.os.Looper;import java.io.*;import java.util.concurrent.*;
public class LocalModel {
 public interface Callback{void done(String answer,String error);} private final ExecutorService worker=Executors.newSingleThreadExecutor();private final Handler main=new Handler(Looper.getMainLooper());private final File root,modelFile;private final String sourceModel="/data/local/tmp/manu/model.gguf";private static boolean nativeReady;private volatile long lastElapsedMs;private volatile int lastOutputChars;
 static{try{System.loadLibrary("manu_jni");nativeReady=true;}catch(Throwable ignored){nativeReady=false;}}
 public LocalModel(Context c){root=new File(c.getFilesDir(),"manu");modelFile=new File(root,"model.gguf");}
 public boolean isAvailable(){return nativeReady&&new File(sourceModel).exists();}
 public void ask(final String prompt,final Callback callback){worker.execute(()->{String answer=null,error=null;long started=System.currentTimeMillis();try{prepareModel();answer=nativeGenerate(modelFile.getAbsolutePath(),prompt);if(answer==null||answer.startsWith("ERROR:"))error=answer==null?"El modelo local no respondió.":answer;}catch(Exception e){error=e.getClass().getSimpleName()+": "+e.getMessage();}lastElapsedMs=System.currentTimeMillis()-started;lastOutputChars=answer==null?0:answer.length();final String a=answer,e=error;main.post(()->callback.done(a,e));});}
 public long getLastElapsedMs(){return lastElapsedMs;} public int getLastOutputChars(){return lastOutputChars;}
 private void prepareModel()throws IOException{if(!root.exists()&&!root.mkdirs())throw new IOException("No se pudo crear el almacenamiento local");if(modelFile.exists()&&modelFile.length()==new File(sourceModel).length())return;try(InputStream in=new BufferedInputStream(new FileInputStream(sourceModel));OutputStream out=new BufferedOutputStream(new FileOutputStream(modelFile))){byte[]b=new byte[1024*1024];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}}
 private static native String nativeGenerate(String model,String prompt);
}
