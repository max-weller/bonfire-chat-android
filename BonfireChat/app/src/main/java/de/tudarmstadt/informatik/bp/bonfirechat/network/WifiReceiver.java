package de.tudarmstadt.informatik.bp.bonfirechat.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;


/**
 * Created by Simon on 22.05.2015.
 */
public class WifiReceiver extends BroadcastReceiver {
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;

    private WifiProtocol mProtocol;

    public static WifiP2pInfo info;
    public InetSocketAddress receiverAddress;
    private final String TAG = "WifiReceiver";


    public WifiReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, WifiProtocol mProtocol) {
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.mProtocol = mProtocol;

    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive wird ausgef�hrt");
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi P2P is enabled
            } else {
                // User has to turn on WifiP2P
            }

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list of current peers
            Log.d(TAG, "PeerschangedAction");
            if (mManager != null) {
                Log.d(TAG, "request peers has been done");
                mManager.requestPeers(mChannel, mProtocol.mWifiPeerListListener);
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            Log.d(TAG, "Die ExtraNetworkInfo ist: " + intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO));
            //sendMessage();

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action))

        {
            // Respond to this device's wifi state changing
        }
    }

    public void sendMessage() {
        Log.d(TAG, "Daten werden GANZ AUSSEN gesendet && mmsg ist :" + mProtocol.packet.toString());
        FutureTask futureTask = new FutureTask(new Callable() {
            @Override
            public Object call() throws Exception {
                Log.d(TAG, "Daten werden au�en gesendet ");

                Log.d(TAG, "Daten werden gesendet");
                //WifiP2pGroup group = mManager.createGroup(mChannel,null);

                mManager.requestConnectionInfo(mChannel, mProtocol.mConnectionInfoListener);


                int port = 4242;
                int len;
                Socket socket = new Socket();
                //String msg = mWifiProtocol


                try {
                    /**
                     * Create a client socket with the host,
                     * port, and timeout information.
                     */
                    socket.setReuseAddress(true);
                    //socket.bind(new InetSocketAddress(port));
                    if (receiverAddress != null) {
                        Log.d(TAG, "ReceiverIp ist : " + receiverAddress.getAddress());
                        socket.connect(new InetSocketAddress(receiverAddress.getAddress(), port), 500);

                    } else if (!info.isGroupOwner) {
                        socket.connect((new InetSocketAddress(info.groupOwnerAddress, port)), 500);
                    }
                    if (socket.isConnected()) {
                        OutputStream outputStream = socket.getOutputStream();

                        mProtocol.send(outputStream, mProtocol.packet);

                        outputStream.close();
                    }

                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                /**
                 * Clean up any open sockets when done
                 * transferring or if an exception occurred.
                 */ finally {
                    if (socket != null) {
                        if (socket.isConnected()) {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }


                return null;
            }
        });
        //todo wieviele Threats?
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        executorService.execute(futureTask);
    }


}
