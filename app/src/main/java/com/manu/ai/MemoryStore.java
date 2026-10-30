package com.manu.ai;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;

public class MemoryStore extends SQLiteOpenHelper {
 private static final String DB="manu_memory.db"; private static final int VERSION=1;
 private static final int MAX_CONTEXT_CHARS=3600;
 public MemoryStore(Context c){super(c,DB,null,VERSION);}
 public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE messages(id INTEGER PRIMARY KEY AUTOINCREMENT, role TEXT NOT NULL, content TEXT NOT NULL, created_at INTEGER NOT NULL)");db.execSQL("CREATE TABLE memories(id INTEGER PRIMARY KEY AUTOINCREMENT, memory_key TEXT NOT NULL UNIQUE, memory_value TEXT NOT NULL, updated_at INTEGER NOT NULL)");}
 public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){}
 public synchronized void addMessage(String role,String content){if(content==null||content.trim().isEmpty())return;ContentValues v=new ContentValues();v.put("role",role);v.put("content",content.trim());v.put("created_at",System.currentTimeMillis());getWritableDatabase().insert("messages",null,v);}
 public synchronized ArrayList<String[]> recent(int limit){ArrayList<String[]> out=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT role,content FROM messages ORDER BY id DESC LIMIT ?",new String[]{String.valueOf(Math.max(1,limit))});try{while(c.moveToNext())out.add(new String[]{c.getString(0),c.getString(1)});}finally{c.close();}Collections.reverse(out);return out;}
 public synchronized boolean hasMessages(){Cursor c=getReadableDatabase().rawQuery("SELECT id FROM messages LIMIT 1",null);try{return c.moveToFirst();}finally{c.close();}}
 public synchronized void remember(String key,String value){if(key==null||value==null||key.trim().isEmpty()||value.trim().isEmpty())return;ContentValues v=new ContentValues();v.put("memory_key",key.trim());v.put("memory_value",value.trim());v.put("updated_at",System.currentTimeMillis());getWritableDatabase().insertWithOnConflict("memories",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
 public synchronized String contextFor(String current){StringBuilder body=new StringBuilder();ArrayList<String[]> mem=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT memory_key,memory_value FROM memories ORDER BY updated_at DESC LIMIT 12",null);try{while(c.moveToNext())mem.add(new String[]{c.getString(0),c.getString(1)});}finally{c.close();}if(!mem.isEmpty()){body.append("Memoria autorizada:\n");for(String[] m:mem)body.append("- ").append(m[0]).append(": ").append(m[1]).append('\n');}
  ArrayList<String[]> history=recent(12);boolean currentAlreadyPresent=!history.isEmpty()&&"user".equals(history.get(history.size()-1)[0])&&history.get(history.size()-1)[1].equals(current==null?"":current.trim());body.append("Conversación reciente:\n");int start=history.size()-1;StringBuilder recent=new StringBuilder();for(int i=start;i>=0;i--){String[] m=history.get(i);String line=m[0]+": "+m[1]+"\n";if(recent.length()+line.length()>MAX_CONTEXT_CHARS)break;recent.insert(0,line);}body.append(recent);if(!currentAlreadyPresent)body.append("Pregunta actual: ").append(current==null?"":current.trim());String result=body.toString();return result.length()>MAX_CONTEXT_CHARS?result.substring(result.length()-MAX_CONTEXT_CHARS):result;}
}
