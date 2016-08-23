package com.example.androidserver;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by apple on 16/8/20.
 */
public class SimpleHttpServer {
    public static final String TAG ="SimpleHttpServer";
    boolean isEnable;
    private WebConfiguration webConfiguration;
    private ServerSocket socket;
    private final ExecutorService threadPool;

    public SimpleHttpServer(WebConfiguration webConfiguration) {
        this.webConfiguration = webConfiguration;
        threadPool = Executors.newCachedThreadPool();
    }

    /**
     * 启动Server(异步)
     */
    public void startAsync(){
        isEnable = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
               doProcSync();
            }
        }).start();
    }

    private void doProcSync() {
        try {
            InetSocketAddress socketAddress = new InetSocketAddress(webConfiguration.getPort());
            socket = new ServerSocket();
            socket.bind(socketAddress);
            while (isEnable) {
                final Socket remotePeer = socket.accept();
                threadPool.submit(new Runnable() {


                    @Override
                    public void run() {
                        Log.i(TAG, "a remote peeer accepted..." + remotePeer.getRemoteSocketAddress().toString());
                        onAcceptRemotePeer(remotePeer);
                    }
                });
            }
        } catch (IOException e) {
            Log.e(TAG, "doProcSync: "+e.toString() );
        }
    }

    private void onAcceptRemotePeer(Socket remotePeer) {
        try {
           // remotePeer.getOutputStream().write("configration connected successful ".getBytes());
            InputStream nis = remotePeer.getInputStream();
            String headerLine = null;
            while ((headerLine = StreamToolkit.readLine(nis)) != null) {
                if (headerLine.equals("\r\n")) {
                    break;
                }
                Log.i(TAG, "onAcceptRemotePeer: "+headerLine);
            }
        } catch (IOException e) {
            Log.e(TAG, "onAcceptRemotePeer: "+e.toString() );
        }

    }

    /**
     * 停止Server(异步)
     */
    public void stopAsync() throws IOException {
        if (!isEnable) {
            return;
        }
        isEnable = false;
        socket.close();
        socket = null;
        new Thread(new Runnable() {
            @Override
            public void run() {

            }
        }).start();
    }
}
