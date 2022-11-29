package com.simplemobiletools.dialer.missedCalls;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.CallLog;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.SecretKey;

public class callLogQueryHelper {

    private final Context context;
    private final NewCallsQuery newCallsQuery;

    callLogQueryHelper(
        Context context,
        NewCallsQuery newCallsQuery){
        this.context = context;
        this.newCallsQuery = newCallsQuery;
    }

    public static callLogQueryHelper getInstance(Context context) {
       ContentResolver contentResolver = context.getContentResolver();
       return new callLogQueryHelper(
           context,createNewCallsQuery(context, contentResolver)
       );
    }

    public static NewCallsQuery createNewCallsQuery(
        Context context, ContentResolver contentResolver) {

        return new DefaultNewCallsQuery(context.getApplicationContext(), contentResolver);
    }


    public  List<NewCall> getNewMissedCalls() {
        return newCallsQuery.query(CallLog.Calls.MISSED_TYPE);
    }

    public static void markAllMissedCallsInLogsAsRead(Context context) {
        markAllMissedCallsInLogsAsRead(context, null);
    }

    public static void markSingleMissedCallInCallLogAsRead(
        @NonNull Context context, @Nullable Uri callUri) {
        if (callUri == null) {
            Log.d("inYte", "uriisNull");
        } else {
            markAllMissedCallsInLogsAsRead(context, callUri);
        }
    }

    private static void markAllMissedCallsInLogsAsRead(Context context, Uri callUri) {
        ContentValues values = new ContentValues();
        values.put(CallLog.Calls.NEW, 0);
        values.put(CallLog.Calls.IS_READ, 1);
        StringBuilder where = new StringBuilder();
        where.append(CallLog.Calls.NEW);
        where.append(" = 1 AND ");
        where.append(CallLog.Calls.TYPE);
        where.append(" = ?");
        try {
            context
                .getContentResolver()
                .update(
                    callUri == null ? CallLog.Calls.CONTENT_URI : callUri,
                    values,
                    where.toString(),
                    new String[] {Integer.toString(CallLog.Calls.MISSED_TYPE)});
        } catch (IllegalArgumentException e) {
            Log.d(
                "inYte",
                "contacts provider update command failed");

        }
    }


    public interface NewCallsQuery {
        long NO_THRESHOLD = Long.MAX_VALUE;

        List <NewCall> query (int type);

        List <NewCall> query (int type, long entryLimit);
    }

    public static final class NewCall {
        public final Uri callsUri;
        public final String number;
        public final long dateMs;

        public NewCall(
            Uri callsUri,
            String number,
            Long dateMs) {
            this.callsUri = callsUri;
            this.number = number;
            this.dateMs = dateMs;
        }

    }

    private static  final class DefaultNewCallsQuery implements NewCallsQuery {
        private static final String[] PROJECTION = {
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.DATE
        };

        private final ContentResolver contentResolver;
        private final Context context;

        private DefaultNewCallsQuery(Context context, ContentResolver contentResolver) {
            this.context = context;
            this.contentResolver = contentResolver;
        }


        private static final String[] PROJECTION_0;

        static  {
            List<String> list = new ArrayList<>();
            list.addAll(Arrays.asList(PROJECTION));
            PROJECTION_0 = list.toArray(new String[list.size()]);
        }

        @Override
        public List<NewCall> query(int type) {
            return query(type, NO_THRESHOLD);
        }

        @Override
        public List<NewCall> query(int type, long entryLimit) {
            if(!(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG)== PackageManager.PERMISSION_GRANTED)) {
                return null;
            }
            Selection.Builder sectionBuilder =
                Selection.builder()
                .and(Selection.column(CallLog.Calls.NEW).is("=1"))
                .and(Selection.column(CallLog.Calls.TYPE).is("=", type))
                .and(Selection.column(CallLog.Calls.IS_READ).is("IS NOT 1"));

            if(entryLimit != NO_THRESHOLD) {
                sectionBuilder =
                    sectionBuilder.and(
                        Selection.column(CallLog.Calls.DATE)
                        .is("IS NULL")
                        .buildUpon()
                        .or(Selection.column(CallLog.Calls.DATE).is(">=",entryLimit))
                        .build()

                    );
            }
            Selection selection = sectionBuilder.build();
            try(Cursor cursor =
                contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ? PROJECTION : PROJECTION_0,
                    selection.getSelection(),
                    selection.getSelectionArgs(),
                    CallLog.Calls.DEFAULT_SORT_ORDER)){
                if(cursor == null) {
                    return  null;
                }
             List<NewCall> newCalls = new ArrayList<>();
                while (cursor.moveToNext()) {
                    newCalls.add(createNewCallsFromCursor(cursor));
                }
                return newCalls;
            } catch(RuntimeException e) {
                    return null;
            }
        }

        private  NewCall createNewCallsFromCursor(Cursor cursor) {
            Uri callsUri =
                ContentUris.withAppendedId(
                    CallLog.Calls.CONTENT_URI, cursor.getLong(0));
                return new NewCall(
                    callsUri,
                    cursor.getString(1),
                    cursor.getLong(2)
                );
        }
    }
}
