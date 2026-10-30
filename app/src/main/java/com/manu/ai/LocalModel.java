package com.manu.ai;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.*;
import java.util.concurrent.*;

public class LocalModel {
 public interface Callback {
  void done(String answer,String error);
  default void token(String piece){}
  default void firstToken(long latencyMs){}
  default void metrics(long promptTokens,int generatedTokens,long firstTokenMs,long totalMs){}
 }
 private final ExecutorService worker=Executors.newSingleThreadExecutor();private final Handler main=new Handler(Looper.getMainLooper());private final File root,modelFile;private final File sourceModel=new File("/data/local/tmp/manu/model.gguf");private static boolean nativeReady;private volatile long lastElapsedMs,lastFirstTokenMs,lastPromptTokens;private volatile int lastGeneratedTokens,lastOutputChars;
 static{try{System.loadLibrary("manu_jni");nativeReady=true;}catch(Throwable ignored){nativeReady=false;}}
 public LocalModel(Context c){root=new File(c.getFilesDir(),"manu");modelFile=new File(root,"model.gguf");}
 public boolean isAvailable(){return nativeReady&&(valid(modelFile)||valid(sourceModel));}
 public File getModelFile(){return modelFile;}
 public long getLastElapsedMs(){return lastElapsedMs;}public long getLastFirstTokenMs(){return lastFirstTokenMs;}public int getLastGeneratedTokens(){return lastGeneratedTokens;}public long getLastPromptTokens(){return lastPromptTokens;}
 public void ask(final String prompt,final Callback callback){worker.execute(()->{String answer=null,error=null;long started=System.currentTimeMillis();lastElapsedMs=0;lastFirstTokenMs=0;lastPromptTokens=0;lastGeneratedTokens=0;try{prepareModel();Callback bridge=new Callback(){public void done(String a,String e){callback.done(a,e);}public void token(String p){main.post(()->callback.token(p));}public void firstToken(long ms){lastFirstTokenMs=ms;main.post(()->callback.firstToken(ms));}public void metrics(long pt,int gt,long ft,long total){lastPromptTokens=pt;lastGeneratedTokens=gt;lastFirstTokenMs=ft;lastElapsedMs=total;main.post(()->callback.metrics(pt,gt,ft,total));}};answer=nativeGenerateStream(modelFile.getAbsolutePath(),PromptConfig.SYSTEM,prompt,bridge);if(answer==null||answer.startsWith("ERROR:"))error=answer==null?"El modelo local no respondió.":answer;}catch(Exception e){error=e.getClass().getSimpleName()+": "+e.getMessage();}if(lastElapsedMs==0)lastElapsedMs=System.currentTimeMillis()-started;lastOutputChars=answer==null?0:answer.length();final String a=answer,e=error;main.post(()->callback.done(a,e));});}
 private boolean valid(File f){return f.exists()&&f.isFile()&&f.length()>1024*1024;}
 private synchronized void prepareModel()throws IOException{if(!root.exists()&&!root.mkdirs())throw new IOException("No se pudo crear el almacenamiento local");if(valid(modelFile))return;if(!valid(sourceModel))throw new FileNotFoundException("Modelo GGUF no encontrado en almacenamiento privado ni en "+sourceModel);File tmp=new File(root,"model.gguf.part");try(InputStream in=new BufferedInputStream(new FileInputStream(sourceModel));OutputStream out=new BufferedOutputStream(new FileOutputStream(tmp))){byte[] b=new byte[1024*1024];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}if(!tmp.renameTo(modelFile)){if(!tmp.delete())throw new IOException("No se pudo finalizar la copia del modelo");throw new IOException("No se pudo mover el modelo al almacenamiento privado");}}
 private static native String nativeGenerateStream(String model,String systemPrompt,String prompt,Callback callback);
 private static native void nativeShutdown();
}
