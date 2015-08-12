package de.tudarmstadt.informatik.bp.bonfirechat.network;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import de.tudarmstadt.informatik.bp.bonfirechat.helper.CryptoHelper;
import de.tudarmstadt.informatik.bp.bonfirechat.routing.Envelope;
import de.tudarmstadt.informatik.bp.bonfirechat.routing.Packet;

/**
 * Created by Simon on 22.05.2015.
 */
public class WifiProtocol extends SocketProtocol {

    Context ctx;
    //todo in etwas sinnvolles  aendern:
    public static final String MBSSID ="1A:2B:3C:4D:5E:6F";
    //if a callback is needed in case of loss of framework communication this should be unnulled;
    public static final WifiP2pManager.ChannelListener MCHLISTENER =null;
    WifiP2pManager mWifiP2pManager;
    WifiP2pManager.Channel mChannel;
    IntentFilter mIntentFilter;
    WifiReceiver mReceiver;
    public Packet packet;
    public static InetAddress mServerInetAdress;
    public static ServerSocket mServerSocket;


    private static final String TAG = "WifiProtocol";


    public WifiProtocol(Context ctx){
        this.ctx = ctx;
        Looper mSrcLooper = ctx.getMainLooper();
        this.mWifiP2pManager= (WifiP2pManager) ctx.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel =  mWifiP2pManager.initialize(ctx, mSrcLooper, MCHLISTENER);
        mReceiver = new WifiReceiver(mWifiP2pManager,mChannel, this);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        ctx.registerReceiver(mReceiver, mIntentFilter);
        registerWifiReceiverSocket();


    }

    @Override
    public void sendPacket(Packet packet, Peer peer) {
        // TODO: send packet only to specified recipients
        this.packet = packet;

        if ((WifiReceiver.info == null  || !WifiReceiver.info.groupFormed)){
            Log.d(TAG, "Der mWifiManager ist " + mWifiP2pManager);
            mWifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Discovering of peers was successful!");

                }

                ;

                @Override
                public void onFailure(int reason) {
                    Log.d(TAG, "the Reason the discovering of peers failed with reason " + reason);
                }

            });
            mReceiver.sendMessage();

        }else {
            mReceiver.sendMessage();


        }
    }




    private void registerWifiReceiverSocket() {


        FutureTask futureTask = new FutureTask(new Callable() {

            @Override
            public Object call() throws Exception {
                Log.d(TAG, "do in Backround wird ausgefuehrt");
                try {



                    if (WifiProtocol.mServerSocket==null){
                        WifiProtocol.mServerSocket = new ServerSocket(4242);
                    }


                    Log.d(TAG, "Server: Socket opened");

                    /**
                     * Create a server socket and wait for client connections. This
                     * call blocks until a connection is accepted from a client
                     */



                        //WifiProtocol.mServerInetAdress = mServerSocket.getInetAddress();
                        Socket client = WifiProtocol.mServerSocket.accept();
                        mReceiver.receiverAddress = (InetSocketAddress) client.getRemoteSocketAddress();

                        Log.d(TAG, "Server: connection done");

                        InputStream inputstream = client.getInputStream();
                        WifiProtocol mySocketProtocol = new WifiProtocol(ctx);
                        Packet p;

                        p = mySocketProtocol.receive(inputstream);
                        Log.d(TAG, "Die message war: " + p);
                        Log.d(TAG,"Der Empfänger ist " +  CryptoHelper.toBase64(p.recipientPublicKey));

                        packetListener.onPacketReceived(WifiProtocol.this, p);



                    //mServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        });


        //todo wieviele Threats?
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.execute(futureTask);

    }


    @Override
    public boolean canSend() {
        return true;
    }

    @Override
    public void shutdown() {

    }
}
