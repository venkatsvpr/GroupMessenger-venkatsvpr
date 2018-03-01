package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         * 
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */
        Log.v("insert", values.toString());

        String key_val = values.getAsString("key");
        String data = values.getAsString("value");


        FileOutputStream outputStream;
        try
        {
            if ((data != null ) && (key_val != null))
            {
                Log.d("venkat", "openFileOutput - Success - File: " + key_val + "data" + data);
                outputStream = getContext().openFileOutput(key_val, Context.MODE_PRIVATE);
                outputStream.write(data.getBytes());
                outputStream.close();
            }
            else
            {
                Log.d("venkat","write failed due to some reason");
            }
        }
        catch (Exception e)
        {
            Log.e("venkat", "File write failed");
        }
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */
        Log.v("query", selection);
        MatrixCursor mCursor = null;
        Log.v("query", selection);
        InputStream is = null;
        try
        {
            is = getContext().openFileInput(selection);
            InputStreamReader is_Reader = new InputStreamReader(is);
            BufferedReader b_Reader = new BufferedReader(is_Reader);
            String message = b_Reader.readLine();
            mCursor = new MatrixCursor(new String[] { "key", "value"});
            mCursor.addRow(new String []{ selection, message});
            Log.d("venkat", message);
            b_Reader.close();
            is_Reader.close();
            is.close();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

        catch (IOException e) {
            e.printStackTrace();
        }

        return mCursor;
    }
}
