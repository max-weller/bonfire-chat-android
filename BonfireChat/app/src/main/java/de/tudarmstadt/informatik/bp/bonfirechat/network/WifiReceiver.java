package de.tudarmstadt.informatik.bp.bonfirechat.network;

import android.bluetooth.BluetoothClass;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import de.tudarmstadt.informatik.bp.bonfirechat.models.Envelope;


/**
 * Created by Simon on 22.05.2015.
 */
public class WifiReceiver extends BroadcastReceiver {
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WifiP2pDevice connectedDevice;
    private WifiProtocol mProtocol;

    public static WifiP2pInfo info;
    public Collection<WifiP2pDevice> mDevList;
    public InetSocketAddress communicationPartner;
    public static Socket socket;



    public WifiReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, WifiProtocol mProtocol) {
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.mProtocol = mProtocol;


    }

    WifiP2pManager.ConnectionInfoListener mConnectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            WifiReceiver.info = info;
        }
    };

    WifiP2pManager.PeerListListener mWifiPeerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peers) {
            mDevList = peers.getDeviceList();
            Log.d(TAG, "the device List is: " + mDevList);
            if (mDevList != null && !(mDevList.isEmpty())) {

                for (WifiP2pDevice dev : mDevList){
                    mProtocol.mWifiP2pManager.stopPeerDiscovery(mProtocol.mChannel, null);
                    connect(dev);

                }
            }



        }
    };

    public void connect (WifiP2pDevice dev){
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = dev.deviceAddress;
            Log.d(TAG, "wifi device found " + config.deviceAddress);
            config.groupOwnerIntent = 0;
            config.wps.setup = WpsInfo.PBC;
            connectedDevice = dev;

            mManager.requestConnectionInfo(mChannel, mConnectionInfoListener);
            //String tmp = info==null ? "null" : info.toString();
            if (info != null) {
                Log.d(TAG, "Info ist: " + info);
            }

            mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "successfully connected with " + connectedDevice);
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.d(TAG, "could not connect with " + connectedDevice + "with reason " + reason);
                    }
                });
    }


    @Override
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
                mManager.requestPeers(mChannel, mWifiPeerListListener);
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            opensocket();

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action))

        {
            // Respond to this device's wifi state changing
        }


    }

    public void opensocket (){
        Log.d(TAG, "Daten werden GANZ AUSSEN gesendet");
        FutureTask futureTask = new FutureTask(new Callable() {
            @Override
            public Object call() throws Exception {
                Log.d(TAG, "Daten werden au�en gesendet");
                if (connectedDevice != null)

                {
                    Log.d(TAG, "Daten werden gesendet");
                    //WifiP2pGroup group = mManager.createGroup(mChannel,null);

                    mManager.requestConnectionInfo(mChannel, mConnectionInfoListener);


                    int port = 4242;
                    int len;
                    WifiReceiver.socket = new Socket();
                    InetAddress host;
                    //String msg = mWifiProtocol


                    try {
                        /**
                         * Create a client socket with the host,
                         * port, and timeout information.
                         */
                        WifiReceiver.socket.setReuseAddress(true);

                        if (!info.isGroupOwner) {
                            WifiReceiver.socket.connect((new InetSocketAddress(info.groupOwnerAddress, port)), 500);
                        }else {

                            WifiReceiver.socket.connect(communicationPartner, 500);
                        }




                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }
                return null;
            }
        });
        //todo wieviele Threats?
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.execute(futureTask);
    }

    public void sendMessage(){
        try {
            OutputStream outputStream = socket.getOutputStream();

            mProtocol.sendEnvelope(outputStream, mProtocol.envelope);

            outputStream.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }




    }
    public void closeSocket(){
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

    private final String TAG = "WifiReceiver";

    public static String getIPFromMac(String MAC) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {

                String[] splitted = line.split(" +");
                if (splitted != null && splitted.length >= 4) {
                    // Basic sanity check
                    String device = splitted[5];
                    if (device.matches(".*p2p-p2p0.*")) {
                        String mac = splitted[3];
                        if (mac.matches(MAC)) {
                            return splitted[0];
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
