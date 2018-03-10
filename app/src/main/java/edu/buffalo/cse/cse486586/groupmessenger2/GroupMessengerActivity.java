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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
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

/* References:
 socket.setsoTimeout - https://docs.oracle.com/javase/7/docs/api/java/net/Socket.html < And some other links from oracle java >
 Data Input/Output Stream - https://developer.android.com/reference/java/io/DataOutputStream.html
 priority-queue - https://docs.oracle.com/javase/7/docs/api/java/util/PriorityQueue.html <and it follow up links>
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
    static int tClear = -1;

    Queue<QueueNode> holdBackQueue = new PriorityQueue<QueueNode>(10, finalQueueCompartor);

    public static Comparator<QueueNode> finalQueueCompartor = new Comparator<QueueNode>() {

        @Override
        public int compare(QueueNode c1, QueueNode c2) {
            /*
            if (c1.pid == c2.pid) {
                if (c1.local_seq > c2.local_seq) {
                    return 1;
                }
                else {
                    return -1;
                }
            }
            else

             */

            if (c1.priority > c2.priority) {
                return 1;
            } else if (c1.priority < c2.priority) {
                return -1;
            } else {
                if (c1.pid > c2.pid)
                {
                    return 1;
                }
                else {
                    return -1;
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
            //serverSocket.setReuseAddress(true);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }


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
            while(true) {
                try {

                    Log.d("venkat", "going to  accept");

                    accept = serverSocket.accept();

                    Log.d("venkat", "accepting:... " + accept);
                    Log.d("venkat", "venkat going to read");

                    DataInputStream in = new DataInputStream(accept.getInputStream());
                    String message = null;
                    message = in.readUTF();
                    //in.close();

                    Log.d("venkat", "Reading Message from accept " + accept);

                    if (message == null) {
                        Log.d("venkat", "null message read skip");
                        //accept.close();
                        continue;
                    }
                    Log.d("venkat", "Read the  message: " + message);
                    String text = null;
                    String[] split_tokens = message.split("#");
                    Log.d("venkat", "There are totally " + split_tokens.length);

                    if (split_tokens.length == 5) {

                        int fromPid = Integer.parseInt(split_tokens[0]);
                        int priority = Integer.parseInt(split_tokens[1]);
                        int seq_num = Integer.parseInt(split_tokens[2]);
                        tClear = Integer.parseInt(split_tokens[3]);
                        text = split_tokens[4];

                        if (!myPort.equals(fromPid)) {
                            int max = 1 + Math.max(gAccepted, gProposed);

                            if (max < priority) {
                                gProposed = priority;
                            } else {
                                gProposed = max;
                            }
                        }

                        QueueNode node = new QueueNode(fromPid, gProposed, seq_num, text);
                        holdBackQueue.add(node);
                        Log.d("venkat", "Added successfully to the  holdback queue <" + node.message + "> <" + node.priority + ">");
                        String output_message = fromPid + "#" + gProposed + "#" + seq_num + "#" + text;
                        DataOutputStream out_print = new DataOutputStream(accept.getOutputStream());
                        out_print.writeUTF(output_message);
                        out_print.flush();
                        //out_print.close();
                        Log.d("venkat", "sent message :" + output_message);
/*
                        try {
                            Thread.sleep(75);
                        }
                        catch (InterruptedException e){
                            e.printStackTrace();
                        }
*/
                    } else if (split_tokens.length == 6) {

                        int fromPid = Integer.parseInt(split_tokens[0]);
                        int priority = Integer.parseInt(split_tokens[1]);
                        int seq_num = Integer.parseInt(split_tokens[2]);
                        tClear = Integer.parseInt(split_tokens[4]);
                        delete_info(tClear);
                        text = split_tokens[5];
                        Log.d("venkat", "at server: " + fromPid + " " + priority + " " + seq_num + " " + text);
                        gAccepted = Math.max(gAccepted, priority);
                        Iterator<QueueNode> itr = holdBackQueue.iterator();
                        QueueNode tmp = null;
                        Boolean found = false;
                        while ((itr.hasNext()) && (found == false)) {
                            tmp = itr.next();
                            Log.d("venkat", "looping holdbackqueue  Srv <" + tmp.message + ">" + " priority:" + tmp.priority);
                            if (tmp.message.equals(text)) {
                                found = true;
                            }
                        }

                        if (tmp == null) {
                            Log.d("venkat", "entry not present ..not coool ");
                          //  QueueNode node = new QueueNode(fromPid, priority, seq_num, text);
                           // node.deliver = true;
                           // holdBackQueue.add(node);
                        }
                        else if (found == true) {
                            if (tmp.priority <= priority) {
                                holdBackQueue.remove(tmp);
                                Log.d("venkat", " Srv new priority of " + tmp.message + " is " + tmp.priority + " changed to" + priority);
                                tmp.priority = priority;
                                tmp.deliver = true;
                                holdBackQueue.add(tmp);
                                Log.d("venkat", "Added successfully to the  holdback queue <" + tmp.message + "> <" + tmp.priority + ">");

                            }
                        }

                        String output_message = "ack";
                        DataOutputStream out_print = new DataOutputStream(accept.getOutputStream());
                        out_print.writeUTF(output_message);
                        out_print.flush();

                       //accept.close();
                    }

                    /* check and deliver message */
                    Boolean breakset = false;
                    while ((holdBackQueue.peek() != null) && (breakset == false)) {
                        if ((holdBackQueue.peek().deliver == true) && (holdBackQueue.peek().pid != tClear)) {
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
                            publishProgress(new String[]{output_message});
                        }
                        else if (holdBackQueue.peek().pid == tClear) {
                            Log.d("venkat", "deleteing the message from " + tClear);
                            QueueNode delivery_node = holdBackQueue.poll();
                            holdBackQueue.remove(delivery_node);
                        }
                        else {
                            breakset = true;
                        }
                    }


                } catch (IOException e) {
                    Log.d ("venkat", "something is wrong... very wrong.. seriously wrong. ########### ");
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
            }
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
            if (msgs[0]== null) {
                return null;
            }

            Socket socket = null;

            gProposed = 1 + Math.max(gAccepted,gProposed);
            int CurrentMaxPriority = gProposed;


            /* Init conections */
            gLocalSeqNum++;

            /* Send proposal message to  all peers and except a reply back from them with their
               find the max of all the proposal replies

               Send acceptabnce on that proposal to all peers. and excpect for an ack.
               once ack received close the channel.

               Incase there is a timeout whiile communicating with a node.. mark it and
               add that information in all packets that a node sends..

               messages from these down nodes shouldnt be delieverd and can be delieted.
             */
            for (int i=0; i<5; i++)
            {

                if (tClear != -1) {
                    if (tClear == Integer.parseInt(remote_port_arr[i])) {
                        continue;
                    }
                }

                String remotePort  = remote_port_arr[i];
                /*

                if (myPort.equals(remotePort)) {
                    String tmsg = msgs[0];
                    QueueNode node = new QueueNode(Integer.parseInt(myPort), gProposed, gLocalSeqNum, tmsg);
                    Log.d("venkat","Adding to the queue..  <"+node.message+"> <"+node.priority+">");
                    holdBackQueue.add(node);
                    continue;
                }
                */

                PrintWriter out = null;

                try {
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));
                    //socket.setTcpNoDelay(true);
                    socket.setSoTimeout(500);
                    String msg_to_send = myPort + "#" + gProposed + "#" + gLocalSeqNum + "#" + tClear + "#" + msgs[0];
                    DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                    output.writeUTF(msg_to_send);
                    output.flush();
                    //output.close();

                    DataInputStream peer = new DataInputStream(socket.getInputStream());
                    String msgFromPeer = peer.readUTF();


                    if (msgFromPeer != null) {
                        Log.d("venkat", " Message from peer :" + msgFromPeer + " peer: " + remotePort);
                        String[] tokens = msgFromPeer.split("#");
                        if (tokens.length > 1) {
                            CurrentMaxPriority = Math.max(CurrentMaxPriority, Integer.parseInt(tokens[1]));
                        }
                    }
                    else
                    {
                        tClear = Integer.parseInt(remote_port_arr[i]);
                        delete_info(tClear);
                        Log.d ("venkat"," read null here   "+tClear);
                    }


                    output.close();
                    peer.close();
                    socket.close();

                }
                catch (SocketTimeoutException e) {
                    tClear = Integer.parseInt(remote_port_arr[i]);
                }
                catch (EOFException  e) {
                    tClear = Integer.parseInt(remote_port_arr[i]);
                }
                catch (StreamCorruptedException e ){
                    tClear = Integer.parseInt(remote_port_arr[i]);
                }
                catch (IOException e) {
                    tClear = Integer.parseInt(remote_port_arr[i]);
                    Log.e("venkat", "ClientTask socket IOException");
                }
            }

            Log.d ("venkat", "the priority agreed is "+CurrentMaxPriority);
            for (int i = 0; i<5; i++) {
                if (tClear != -1){
                    if (tClear == Integer.parseInt(remote_port_arr[i])) {
                        continue;
                    }
                }

                String txt_msg = msgs[0];
                String remotePort = remote_port_arr[i];

                /*
                if (myPort.equals(remotePort)) {
                    Iterator<QueueNode> itr2 = holdBackQueue.iterator();
                    QueueNode tmp2 = null;
                    Boolean present = false;
                    while ((itr2.hasNext()) && (present == false)) {
                        tmp2 = itr2.next();
                        Log.d("venkat","looping holdbackqueue  Clnt<"+tmp2.message+">"+ " priority:"+tmp2.priority);
                        if (tmp2.message.equals(txt_msg)) {
                            present = true;
                        }
                    }

                    if (present == true)
                    {
                        Log.d ("venkat","updating self priority msg: "+tmp2.message+" from "+tmp2.priority+" to"+CurrentMaxPriority);
                        holdBackQueue.remove(tmp2);
                        tmp2.priority = CurrentMaxPriority;
                        tmp2.deliver = true;
                        holdBackQueue.add(tmp2);
                        Log.d("venkat","Adding back to the queue..  <"+tmp2.message+"> <"+tmp2.priority+">");

                    }
                }*/

                try {
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));
                    //socket.setTcpNoDelay(true);
                    socket.setSoTimeout(500);

                    String msg_to_send2 = myPort+"#"+Integer.toString(CurrentMaxPriority)+"#"+Integer.toString(gLocalSeqNum)+"#"+"deliver"+"#"+tClear+"#"+txt_msg;
                    Log.d("venkat","Sending  to remotePort "+remotePort+" msg: "+msg_to_send2 +" text:"+txt_msg);
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeUTF(msg_to_send2);
                    out.flush();

                    DataInputStream peer = new DataInputStream(socket.getInputStream());
                    String msgFromPeer = peer.readUTF();

                    if (msgFromPeer.equals("done")) {
                        out.close();
                        peer.close();
                        socket.close();
                    }
                }
                catch (SocketTimeoutException e) {
                    tClear = Integer.parseInt(remote_port_arr[i]);
                }
                catch (EOFException  e) {
                    tClear = Integer.parseInt(remote_port_arr[i]);
                }
                catch (StreamCorruptedException e ){
                    tClear = Integer.parseInt(remote_port_arr[i]);
                }
                catch (IOException e) {
                    tClear = Integer.parseInt(remote_port_arr[i]);
                    Log.e("venkat", "ClientTask socket IOException");
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

    public void delete_info (Integer port) {
      /* VENKAT TODO: see if there is a need to  delete the messages in the queue.
         can we not achieve the same by not delievering such messages. or removeing it at deletion side
             */
      /*  if (port <= 0) {
            return;
        }
        Iterator<QueueNode> itr2 = holdBackQueue.iterator();
        QueueNode tmp2 = null;
        while (itr2.hasNext()) {
            tmp2 = itr2.next();
            Log.d("venkat","looping holdbackqueue  Clnt<"+tmp2.message+">"+ " priority:"+tmp2.priority);
            if ((tmp2.message.equals(Integer.toString(port))) || (tmp2.deliver == false)) {
                holdBackQueue.remove(tmp2);
                break;
            }
        }
        */
    }
}


