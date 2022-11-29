package com.simplemobiletools.dialer;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class myDatabaseAccess extends SQLiteOpenHelper {

    public static final String dbname = "RICHCALLDATA";
    public static final int dbversion = 1;
    public static myDatabaseAccess instance_=null;

    myDatabaseAccess(Context context)
    {
        super(context, dbname, null, dbversion);
    }
    public  static myDatabaseAccess getInstance(Context context)
    {
        if(instance_==null){
            instance_= new myDatabaseAccess(context);
        }
        return instance_;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        checkAndProceed(db, 1, dbversion);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        checkAndProceed(db, oldVersion, newVersion);
    }
    public void checkAndProceed(SQLiteDatabase db, int oldversion, int newversion)
    {
       if(oldversion<newversion)
       {
           /* do something here */
       }
     /*  else if(oldversion==1)
       {



       } */
       else if(oldversion==newversion)
       {
           /* do something here */
           /*create the RICHCALLDATA DB here */

           db.execSQL("CREATE TABLE RICHCALLDATA ("
               + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
               + "TYPE TEXT, "
               + "VALUE TEXT);");
           String[] strings = {"name", "alternateNumber", "facebook", "instagram", "twitter","linkedin", "website", "image"};
           inserttable(db, strings); /* init table with null values on create */


       }


    }
    public void inserttable(SQLiteDatabase db, String[] strings)
    {
        for(int i=0;i<8;i++)
        {
            ContentValues cv = new ContentValues();
            cv.put("TYPE", strings[i]);
            cv.put("VALUE", "");
            db.insert("RICHCALLDATA", null, cv);
        }

    }
}
