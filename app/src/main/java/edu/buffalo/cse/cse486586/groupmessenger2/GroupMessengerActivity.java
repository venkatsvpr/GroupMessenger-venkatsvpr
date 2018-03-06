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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;

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
    static String myPort = null;
    static int gCount =0;
    static int gAccepted = -1;
    static int gProposed = -1;
    static int gLocalSeqNum = -1;

    Queue<QueueNode> holdBackQueue = new PriorityQueue<QueueNode>(10, finalQueueCompartor);

    public static Comparator<QueueNode> finalQueueCompartor = new Comparator<QueueNode>() {

        @Override
        public int compare(QueueNode c1, QueueNode c2) {
            if (c1.priority > c2.priority) {
                return 1;
            } else if (c1.priority < c2.priority) {
                return -1;
            } else {
                if (c1.localSeq > c2.localSeq) {
                    return 1;
                } else if (c1.localSeq < c2.localSeq) {
                    return -1;
                } else {
                    if (c1.pid > c2.pid) {
                        return 1;
                    }
                    else if (c1.pid < c2.pid) {
                        return -1;
                    }
                    else {
                        return 0;
                    }
                }
            }
        }
    };
        @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /* Venkat - Do the server socket setup */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
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
                Log.d("venkat","Message input :"+message);
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
                while(true) {
                    Log.d("venkat", "going to  accept");

                    accept = serverSocket.accept();
                    Log.d ("venkat","accepting:... "+accept);
                    BufferedReader in = null;

                    Log.d("venkat", "venkat going to read");

                    in = new BufferedReader(new InputStreamReader(accept.getInputStream()));
                    String message = in.readLine();
                    Log.d("venkat","Reading Message from accept "+accept);
                    if (message == null) {
                        Log.d ("venkat", "null message read skip");
                        return null;
                    }
                    Log.d ("venkat","Read the  message: "+message);
                    String text = null;
                    String[] split_tokens = message.split("#");
                    Log.d ("venkat", "There are totally "+split_tokens.length);

                    if (split_tokens.length == 4) {
                        int fromPid = Integer.parseInt(split_tokens[0]);
                        int priority = Integer.parseInt(split_tokens[1]);
                        int seq_num = Integer.parseInt(split_tokens[2]);
                        text = split_tokens[3];
                        int max = 1 + Math.max(gAccepted, gProposed);

                        if (max < priority) {
                            gProposed = priority;
                        } else {
                            gProposed = max;
                        }

                        QueueNode node = new QueueNode(fromPid, gProposed, seq_num, text);
                        holdBackQueue.add(node);
                        Log.d("venkat","Added successfully to the  holdback queue <"+node.message+"> <"+node.priority+">");
                        String output_message = fromPid + "#" + gProposed + "#" + seq_num + "#" + text;
                        PrintWriter out_print = new PrintWriter(new BufferedWriter(new OutputStreamWriter(accept.getOutputStream())), true);
                        out_print.println(output_message);
                        out_print.flush();

                        Log.d("venkat","sent message :"+output_message);
                        try {
                            Thread.sleep(75);
                        }
                        catch (InterruptedException e){
                            e.printStackTrace();
                        }

                    }
                    else if(split_tokens.length == 5) {

                        int fromPid = Integer.parseInt(split_tokens[0]);
                        int priority = Integer.parseInt(split_tokens[1]);
                        int seq_num = Integer.parseInt(split_tokens[2]);
                        text = split_tokens[4];
                        Log.d ("venkat", "at server: "+fromPid+" "+priority+" "+seq_num+" "+text);
                        gAccepted = Math.max(gAccepted, priority);
                        Iterator<QueueNode> itr = holdBackQueue.iterator();
                        QueueNode tmp = null;
                        while (itr.hasNext()) {
                            tmp = itr.next();
                            Log.d("venkat","looping holdbackqueue  Srv <"+tmp.message+">"+ " priority:"+tmp.priority);
                            if (tmp.message.equals(text)) {
                                break;
                            }
                        }

                        if (tmp == null) {
                            Log.d ("venkat","entry not present ..not coool");

                        }
                        else {
                            if (tmp.priority <= priority) {
                                holdBackQueue.remove(tmp);
                                Log.d("venkat", " Srv new priority of " + tmp.message + " is " + tmp.priority + " changed to" + priority);
                                tmp.priority = priority;
                                tmp.deliver = true;
                                holdBackQueue.add(tmp);
                            }
                        }
                    }

                    /* check and deliver message */

                    while (holdBackQueue.peek() != null) {
                        if (holdBackQueue.peek().deliver == true) {
                            QueueNode delivery_node = holdBackQueue.poll();
                            /* Store the data */
                            Uri.Builder uriBuilder = new Uri.Builder();
                            uriBuilder.authority("edu.buffalo.cse.cse486586.groupmessenger2.provider");
                            uriBuilder.scheme("content");
                            mUri = uriBuilder.build();
                            cv.put("key", Integer.toString(gCount));
                            cv.put("value", delivery_node.message);
                            String output_message = delivery_node.message;
                            mContentResolver = getContentResolver();
                            mContentResolver.insert(mUri, cv);
                            gCount++;
                            publishProgress(new String[]    {output_message});

                        }
                        else {
                            break;
                        }
                    }
                }
/*

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
                        publishProgress(new String[]{message});

                    }
                }
                */

            }
            catch (IOException e)
            {
                //in.close();
                //accept.close();
                e.printStackTrace();
            }
            /*
            finally {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }*/
            return null;
        }

        protected void onProgressUpdate(String...strings) {
            //     Log.d("venkat", "venkat"+"Inside onProgressUpdate"+strings[0]);
            /*
             * The following code displays what is received in doInBackground().
             */
            //String strReceived = strings[0].trim();
            //Log.d("venkat", "venkat"+"Inside onProgressUpdate222 "+strReceived);
            String strReceived = strings[0].trim();
            TextView text_content = (TextView) findViewById(R.id.textView1);
            text_content.append(strReceived + "\t\n");

            return;
        }
    }
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs)
        {
            gProposed = 1 + Math.max(gAccepted,gProposed);
            int CurrentMaxPriority = gProposed;


            /* Init conections */
            Socket peer_socket[] = new Socket[5];
            for (int i=0; i<5; i++) {
                String remotePort = remote_port_arr[i];
                if (myPort.equals(remotePort)) {
                    continue;
                }
                try {
                    peer_socket[i] = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));
                    peer_socket[i].setTcpNoDelay(true);
                    if (peer_socket[i].isConnected()) {
                        Log.d("venkat"," socket connected ");
                    }
                    //peer_socket[i].setKeepAlive(true);
                } catch (UnknownHostException e) {
                    Log.e("venkat", "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e("venkat", "ClientTask socket IOException");
                }
            }



            gLocalSeqNum++;
            for (int i=0; i<5; i++)
            {
                Socket socket = peer_socket[i];
                String remotePort  = remote_port_arr[i];
                if (myPort.equals(remotePort)) {
                    String tmsg = msgs[0];
                    QueueNode node = new QueueNode(Integer.parseInt(myPort), gProposed, gLocalSeqNum, tmsg);
                    holdBackQueue.add(node);
                    continue;
                }

                PrintWriter out = null;
                BufferedReader peer = null;

                try {
                   // socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
               //          Integer.parseInt(remotePort));
                    //socket.setTcpNoDelay(true);
                    String msg_to_send = myPort+"#"+gProposed+"#"+gLocalSeqNum+"#"+msgs[0];
                    out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                    out.println(msg_to_send);
                    out.flush();
                }
                catch (UnknownHostException e) {
                    Log.e("venkat", "ClientTask UnknownHostException");
                }
                catch (IOException e) {
                    Log.e("venkat", "ClientTask socket IOException");
                }

                if (socket == null)
                {
                    return null;
                }


                try {
                    peer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String msgFromPeer = peer.readLine();
                    Log.d("venkat"," Message from peer :"+msgFromPeer+" peer: "+remotePort);
                    String[] tokens = msgFromPeer.split("#");
                    if (tokens.length >1) {
                        CurrentMaxPriority = Math.max(CurrentMaxPriority, Integer.parseInt(tokens[1]));
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }



            Log.d ("venkat", "the priority agreed is "+CurrentMaxPriority);
            for (int i = 0; i<5; i++) {
                String txt_msg = msgs[0];
                String remotePort = remote_port_arr[i];

                if (myPort.equals(remotePort)) {
                    Iterator<QueueNode> itr2 = holdBackQueue.iterator();
                    QueueNode tmp2 = null;
                    while (itr2.hasNext()) {
                        tmp2 = itr2.next();
                        Log.d("venkat","looping holdbackqueue  Clnt<"+tmp2.message+">"+ " priority:"+tmp2.priority);
                        if (tmp2.message.equals(txt_msg)) {
                            break;
                        }
                    }

                    if (tmp2 != null)
                    {
                        Log.d ("venkat","updating self priority msg: "+tmp2.message+" from "+tmp2.priority+" to"+CurrentMaxPriority);
                        holdBackQueue.remove(tmp2);
                        tmp2.priority = CurrentMaxPriority;
                        tmp2.deliver = true;
                        holdBackQueue.add(tmp2);
                    }
                }
                try {
                    peer_socket[i] = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));
                    //peer_socket[i].setTcpNoDelay(true);

                    if (peer_socket[i].isConnected()) {
                        Log.d("venkat"," socket connected ");
                    }
                    //peer_socket[i].setKeepAlive(true);
                } catch (UnknownHostException e) {
                    Log.e("venkat", "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e("venkat", "ClientTask socket IOException");
                }


                try {
                    String msg_to_send2 = myPort+"#"+Integer.toString(CurrentMaxPriority)+"#"+Integer.toString(gLocalSeqNum)+"#"+"deliver"+"#"+txt_msg;
                    Log.d("venkat","Sending  to remotePort "+remotePort+" msg: "+msg_to_send2 +" text:"+txt_msg);
                    PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(peer_socket[i].getOutputStream())), true);
                    out.println(msg_to_send2);
                    out.flush();
                }
                catch (IOException e) {
                    Log.d("venkat","io exception");
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    peer_socket[i].close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }
    }
    public class QueueNode {
        public int priority;
        public int initPriority;
        public String message;
        public int localSeq;
        public int pid;
        public boolean deliver;
        public float final_priority;

        public QueueNode(int pid, int priority, int local_seq, String message) {
            this.initPriority = priority;
            this.priority = priority;
            this.message = message;
            this.localSeq = local_seq;
            this.pid = pid;
            this.deliver = false;
        }
    }

}


