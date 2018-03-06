package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final int SERVER_PORT = 10000;
    static final String REMOTE_PORT0 = "11108";
    static final String[] remote_port_arr = new String[] {"11108", "11112", "11116" , "11120" ,"11124"};
    static int gCount =0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /* Venkat - Do the server socket setup */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        ServerSocket serverSocket = null;
        try
        {
            serverSocket = new ServerSocket(SERVER_PORT);
            serverSocket.setReuseAddress(true);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);


        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        final EditText editText =   (EditText) findViewById(R.id.editText1);

        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                String message = editText.getText().toString() + "\n";
                editText.setText("");
                Log.d("venkat","Message input"+message);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message, myPort);
                Log.d("venkat",myPort+"created");
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class   ServerTask extends AsyncTask<ServerSocket, String, Void>
    {
        private Uri mUri;
        private ContentResolver mContentResolver;
        private ContentValues cv = new ContentValues();


        @Override
        protected Void doInBackground(ServerSocket... sockets) {

            ServerSocket serverSocket = sockets[0];
            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */

            Socket accept = null;
            try
            {
                while(true)
                {
                    accept = serverSocket.accept();
                    BufferedReader in = null;
                    Log.d("venkat", "venkat going to read");
                    in = new BufferedReader(new InputStreamReader(accept.getInputStream()));
                    String message = in.readLine();

                    if (message != null) {
                        Log.d("venkat", "venkat" + message + "read success");

                        Uri.Builder uriBuilder = new Uri.Builder();
                        uriBuilder.authority("edu.buffalo.cse.cse486586.groupmessenger2.provider");
                        uriBuilder.scheme("content");
                        mUri = uriBuilder.build();

                        cv.put("key", Integer.toString(gCount));
                        cv.put("value", message);
                        mContentResolver = getContentResolver();
                        mContentResolver.insert(mUri, cv);
                        gCount++;
                        Log.d("venkat", "storing" + message + "success at file" + Integer.toString(gCount - 1));
                        //publishProgress(new String[]{message});

                    }
                }

            }
            catch (IOException e)
            {
                //in.close();
                //accept.close();
                e.printStackTrace();
            }
            return null;
        }

        protected void onProgressUpdate(String...strings) {
            //     Log.d("venkat", "venkat"+"Inside onProgressUpdate"+strings[0]);
            /*
             * The following code displays what is received in doInBackground().
             */
            //   String strReceived = strings[0].trim();
            // Log.d("venkat", "venkat"+"Inside onProgressUpdate222 "+strReceived);

            return;
        }
    }
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs)
        {
            for (int i=0; i<5; i++)
            {
                String remotePort  = remote_port_arr[i];
                try
                {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));
                    String msg_to_send = msgs[0];
                    Log.d("venkat", "venkat" + msg_to_send + "towrite" + i);
                    PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                    //out.write(msg_to_send);
                    out.println(msg_to_send);
                    out.flush();
                    //out.close();
                    //socket.close();

                }
                catch (UnknownHostException e) {
                    Log.e("venkat", "ClientTask UnknownHostException");
                }
                catch (IOException e) {
                    Log.e("venkat", "ClientTask socket IOException");
                }

            }
            return null;
        }
    }
}


