package com.simplemobiletools.dialer.utilities;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;



public class notificationsTrackerDB extends SQLiteOpenHelper {
    private static final String database_name = "notifications_tracker";
    private static final int database_version = 1;

    public notificationsTrackerDB(Context context){
        super(context, database_name, null, database_version);
    }
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE TRACKER (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "IDENTIFIER TEXT, "
            + "TIME TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        /* TODO */
    }
}
