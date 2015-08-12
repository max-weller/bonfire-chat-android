package de.tudarmstadt.informatik.bp.bonfirechat.network;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.jivesoftware.smack.SmackAndroid;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.informatik.bp.bonfirechat.R;
import de.tudarmstadt.informatik.bp.bonfirechat.data.BonfireAPI;
import de.tudarmstadt.informatik.bp.bonfirechat.data.BonfireData;
import de.tudarmstadt.informatik.bp.bonfirechat.helper.RingBuffer;
import de.tudarmstadt.informatik.bp.bonfirechat.models.Contact;
import de.tudarmstadt.informatik.bp.bonfirechat.models.Conversation;
import de.tudarmstadt.informatik.bp.bonfirechat.models.Identity;
import de.tudarmstadt.informatik.bp.bonfirechat.models.Message;
import de.tudarmstadt.informatik.bp.bonfirechat.routing.AckPacket;
import de.tudarmstadt.informatik.bp.bonfirechat.routing.Envelope;
import de.tudarmstadt.informatik.bp.bonfirechat.routing.Packet;
import de.tudarmstadt.informatik.bp.bonfirechat.routing.PacketType;
import de.tudarmstadt.informatik.bp.bonfirechat.routing.PayloadPacket;
import de.tudarmstadt.informatik.bp.bonfirechat.routing.Retransmission;
import de.tudarmstadt.informatik.bp.bonfirechat.routing.RoutingManager;
import de.tudarmstadt.informatik.bp.bonfirechat.stats.CurrentStats;
import de.tudarmstadt.informatik.bp.bonfirechat.stats.StatsCollector;
import de.tudarmstadt.informatik.bp.bonfirechat.ui.MessagesActivity;

/**
 * Created by mw on 21.05.15.
 *
 */
public class ConnectionManager extends NonStopIntentService {

    private static final String TAG = "ConnectionManager";

    // action in Intents which are sent to the service
    public static final String GO_ONLINE_ACTION = "de.tudarmstadt.informatik.bp.bonfirechat.GO_ONLINE";
    public static final String SENDMESSAGE_ACTION = "de.tudarmstadt.informatik.bp.bonfirechat.SENDMESSAGE";

    // action in event Intents broadcasted by the service
    public static final String MSG_RECEIVED_BROADCAST_EVENT =
            "de.tudarmstadt.informatik.bp.bonfirechat.MSG_RECEIVED_BROADCAST";
    public static final String MSG_SENT_BROADCAST_EVENT =
            "de.tudarmstadt.informatik.bp.bonfirechat.MSG_SENT_BROADCAST";
    public static final String MSG_ACKED_BROADCAST_EVENT =
            "de.tudarmstadt.informatik.bp.bonfirechat.MSG_ACKED_BROADCAST";
    public static final String CONNECTED_BROADCAST_EVENT =
            "de.tudarmstadt.informatik.bp.bonfirechat.CONNECTED_BROADCAST";
    public static final String CONNECTION_CLOSED_BROADCAST_EVENT =
            "de.tudarmstadt.informatik.bp.bonfirechat.CONNECTION_CLOSED_BROADCAST";
    public static final String NEW_CONVERSATION_BROADCAST_EVENT =
            "de.tudarmstadt.informatik.bp.bonfirechat.NEW_CONVERSATION_BROADCAST";



    public static final String EXTENDED_DATA_CONVERSATION_ID =
            "de.tudarmstadt.informatik.bp.bonfirechat.CONVERSATION_ID";
    public static final String EXTENDED_DATA_PEER_ID =
            "de.tudarmstadt.informatik.bp.bonfirechat.PEER_ID";
    public static final String EXTENDED_DATA_PROTOCOL_CLASS =
            "de.tudarmstadt.informatik.bp.bonfirechat.PROTOCOL_CLASS";
    public static final String EXTENDED_DATA_MESSAGE_TEXT =
            "de.tudarmstadt.informatik.bp.bonfirechat.MESSAGE_TEXT";
    public static final String EXTENDED_DATA_MESSAGE_UUID =
            "de.tudarmstadt.informatik.bp.bonfirechat.MESSAGE_UUID";
    public static final String EXTENDED_DATA_ERROR =
            "de.tudarmstadt.informatik.bp.bonfirechat.ERROR";


    // maximum hops for a message until it will be discarded
    public static final int MAX_HOP_COUNT = 20;

    // buffer for storing which messages have already been handled
    // Those were either already sent in the first place, or received
    private static final RingBuffer<Packet> processedPackets = new RingBuffer<>(250);


    private static RoutingManager routingManager = new RoutingManager();

    // currently visible peers
    private static final List<Peer> peers = new ArrayList<>();
    private static Handler handler = new Handler();

    public static final Class[] registeredProtocols = new Class[]{
            BluetoothProtocol.class,
            WifiProtocol.class,
            GcmProtocol.class };


    /**
     * Creates the ConnectionManager, called by Android creating the service
     */
    public ConnectionManager() {
        super("BonFire_ConnectionManager");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SmackAndroid.init(this);
        removeOutdatedPeersThread.run();
    }

    public static List<IProtocol> connections = new ArrayList<IProtocol>();

    public IProtocol getOrCreateConnection(Class typ) {
        IProtocol p = getConnection(typ);
        if (p == null) {
            try {
                p = (IProtocol) typ.getDeclaredConstructor(Context.class).newInstance(ConnectionManager.this);
                p.setIdentity(BonfireData.getInstance(this).getDefaultIdentity());
                p.setOnPacketReceivedListener(packetListener);
                p.setOnPeerDiscoveredListener(peerListener);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            connections.add(p);
            Log.d(TAG, "Die aktivierten connections sind: " + connections.toString());
        }
        return p;
    }

    private IProtocol getConnection(Class typ) {
        for(IProtocol p : connections) {
            if (typ.isInstance(p)) return p;
        }
        return null;
    }

    private void closeConnection(Class typ) {
        for(IProtocol p : connections) {
            if (typ.isInstance(p)) {
                connections.remove(p);
                p.shutdown();
            }
        }
        synchronized (peers) {
            for (Peer p : peers) {
                if (p.getProtocolClass().equals(typ))
                    peers.remove(p);
            }
        }
    }

    private OnPeerDiscoveredListener peerListener = new OnPeerDiscoveredListener() {
        @Override
        public void discoveredPeer(IProtocol sender, byte[] address) {
            Peer found = new Peer(sender.getClass(), address);
            Log.d(TAG, "Peer was discovered by "+sender.toString()+" : "+Peer.formatMacAddress(address));
            synchronized (peers) {
                int index = peers.indexOf(found);
                // is this peer already known to us?
                if (index != -1) {
                    peers.get(index).updateLastSeen(sender.getClass());
                }
                // otherwise add it
                else {
                    peers.add(found);
                }
            }
        }
    };

    private Runnable removeOutdatedPeersThread = new Runnable() {
        @Override
        public void run() {
            synchronized (peers) {
                for (Peer peer : peers) {
                    if (peer.isOutdated()) {
                        peers.remove(peer);
                    }
                }
            }
            handler.postDelayed(removeOutdatedPeersThread, 1000 * 60 * 5); // every 5 minutes
        }
    };

    private OnPacketReceivedListener packetListener = new OnPacketReceivedListener() {
        @Override
        public void onPacketReceived(IProtocol sender, Packet packet) {
            StatsCollector.publishMessageHop(sender.getClass(), "RECV", null, packet);
            Log.d(TAG, "onPacketReceived: " + sender.getClass().getSimpleName() + ", " + packet.toString());
            // has this packet not yet been processed?
            if (!processedPackets.contains(packet)) {
                // remember this packet
                processedPackets.enqueue(packet);
                // remember path to sender
                routingManager.registerPath(packet);
                // is this packet sent to us?
                if (packet.hasRecipient(BonfireData.getInstance(ConnectionManager.this).getDefaultIdentity())) {
                    Log.d(TAG, "this packet is for us. ");
                    // is it  a payload packet?
                    if (packet.getType() == PacketType.Payload) {
                        handlePayloadPacket((PayloadPacket) packet, sender);
                    }
                    // is it an ACK packet?
                    else if (packet.getType() == PacketType.Ack) {
                        handleAckPacket((AckPacket) packet, sender);
                    }
                    // otherwise it's an unknown packet type
                    else {
                        // TODO: team: evaluate: throw Exception instead?
                        Log.e(TAG, "received packet of unknown type");
                    }
                } else {
                    forwardPacket(packet);
                }
            }
        }

        private void handlePayloadPacket(PayloadPacket packet, IProtocol sender) {
            // turn it into an Envelope, as those are the only supported PayloadPackets
            Envelope envelope = (Envelope) packet;
            Log.i(TAG, "handling PayloadPacket");
            // traceroute stuff
            TracerouteHandler.handleTraceroute(ConnectionManager.this, sender, "Recv", envelope);
            BonfireAPI.publishTraceroute(envelope);
            // unpack the Envelope to a Message and handle it
            final Message message = envelope.toMessage(ConnectionManager.this);
            message.setTransferProtocol(sender.getClass());
            // TODO if message is already in database, do not display push notification again
            storeAndDisplayMessage(message);
            // send ACK
            acknowledgePacket(envelope);
            // update statistics
            CurrentStats.getInstance().messageReceived += 1;
        }

        private void handleAckPacket (AckPacket packet, IProtocol sender) {
            // cancel the pending retransmission for this packet
            Retransmission.cancel(packet.acknowledgesUUID);

            // notify the ui that the recipient has acknowledged this message
            final Intent localIntent = new Intent(MSG_ACKED_BROADCAST_EVENT)
                    .putExtra(EXTENDED_DATA_MESSAGE_UUID, packet.acknowledgesUUID)
                    .putExtra(EXTENDED_DATA_PROTOCOL_CLASS, sender.getClass());

            LocalBroadcastManager.getInstance(ConnectionManager.this).sendBroadcast(localIntent);
        }

        private void forwardPacket(Packet packet) {
            // if the packet has been sent less than 20 hops, redistribute it
            if (packet.getHopCount() < MAX_HOP_COUNT) {
                sendPacket(ConnectionManager.this, packet);
            }
        }

        private void storeAndDisplayMessage(Message message) {
            LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(ConnectionManager.this);
            Log.d(TAG, "display message: " + message.body);
            BonfireData data = BonfireData.getInstance(ConnectionManager.this);
            Conversation conversation = data.getConversationByPeer((Contact) message.sender);
            if (conversation == null) {
                Log.d(TAG, "creating new conversation for peer "+message.sender.getNickname());
                conversation = new Conversation((Contact) message.sender, message.sender.getNickname(), 0);
                data.createConversation(conversation);

                final Intent localIntent = new Intent(NEW_CONVERSATION_BROADCAST_EVENT)
                        // Puts the status into the Intent
                        .putExtra(EXTENDED_DATA_CONVERSATION_ID, conversation.rowid);
                broadcastManager.sendBroadcast(localIntent);

            }
            Log.d(TAG, "conversationId=" + conversation.rowid);

            long id = data.createMessage(message, conversation);
            if (id == -1) {
                Log.e(TAG, "Unable to insert message to db, ignoring");
                return;
            }
            Log.d(TAG, "message stored in db with uuid=" + message.uuid);

            final Intent localIntent = new Intent(MSG_RECEIVED_BROADCAST_EVENT)
                    .putExtra(EXTENDED_DATA_CONVERSATION_ID, conversation.rowid)
                    .putExtra(EXTENDED_DATA_MESSAGE_UUID, message.uuid)
                    .putExtra(EXTENDED_DATA_MESSAGE_TEXT, message.body);
            // Broadcasts the Intent to receivers in this app.
            broadcastManager.sendBroadcast(localIntent);


            final Intent intent = new Intent(ConnectionManager.this, MessagesActivity.class);
            intent.putExtra("ConversationId", conversation.rowid);
            final TaskStackBuilder stackBuilder = TaskStackBuilder.create(ConnectionManager.this);
            stackBuilder.addParentStack(MessagesActivity.class);
            stackBuilder.addNextIntent(intent);
            final PendingIntent pi = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT);
            //PendingIntent.getActivity(ConnectionManager.this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            final Uri sound = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.correct);
            final NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(ConnectionManager.this)
                            .setSmallIcon(R.drawable.ic_whatshot_white_24dp)
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                            .setContentTitle(conversation.title)
                            .setContentText(message.body)
                            .setContentIntent(pi)
                            .setSound(sound)
                            .setAutoCancel(true)
                            .setVibrate(new long[]{500, 500, 150, 150, 150, 150, 300, 300, 300, 0});

            final NotificationManager mNotifyMgr =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mNotifyMgr.notify(1, mBuilder.build());
        }
    };

    @Override
    protected void onHandleIntent(Intent intent) {
        final Bundle extras = intent.getExtras();
        final BonfireData db = BonfireData.getInstance(this);
        if (intent.getAction() == GO_ONLINE_ACTION) {
            final Identity id = db.getDefaultIdentity();
            id.registerWithServer();
            db.updateIdentity(id);

            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            for(Class protocol : registeredProtocols) {
                if (isProtocolEnabled(protocol)) {
                    getOrCreateConnection(protocol);
                } else {
                    closeConnection(protocol);
                }
            }

        } else if (intent.getAction() == SENDMESSAGE_ACTION) {
            final Packet packet = (Packet) intent.getSerializableExtra("packet");
            Log.d(TAG, "Loading packet " + packet.toString());

            if (packet instanceof PayloadPacket && packet.getHopCount() == 0)
                Retransmission.add(this, (PayloadPacket)packet, 20000);

            // let RoutingManager decide where to send the packet to
            List<Peer> chosenPeers = routingManager.chooseRecipients(packet, peers);
            if (chosenPeers == null) {
                Log.e(TAG, "don't know how to send this packet: "+packet.toString());
                return;
            }
            for(Peer peer : chosenPeers) {
                try {
                    Class protocolClass = peer.getProtocolClass();
                    IProtocol protocol = getConnection(protocolClass);
                    if (protocol != null && protocol.canSend()) {
                        StatsCollector.publishMessageHop(protocolClass, "SEND", peer, packet);
                        Log.i(TAG, "Sending via " + peer.toString());
                        protocol.sendPacket(packet, peer);
                    } else throw new IllegalAccessException("Protocol "+protocolClass.getSimpleName()+" not ready");
                } catch(Exception ex) {
                    ex.printStackTrace();
                    Log.e(TAG, "Unable to send packet via peer:"+peer.toString());
                }
            }
            packet.setTimeSent(System.currentTimeMillis());

            // if a message object is specified, this packet was just generated on this phone
            // notify UI that sending has started
            final Intent localIntent = new Intent(MSG_SENT_BROADCAST_EVENT)
                    .putExtra(EXTENDED_DATA_MESSAGE_UUID, packet.uuid);

            LocalBroadcastManager.getInstance(ConnectionManager.this).sendBroadcast(localIntent);

            // update statistics
            CurrentStats.getInstance().messagesSent += 1;

        } else if (!extras.isEmpty()) {
            final GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
            final String messageType = gcm.getMessageType(intent);

            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(intent.getAction())) {
                Log.w(TAG, "GCM: Send error: " + extras.toString());

            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(intent.getAction())) {
                Log.w(TAG, "GCM: Deleted messages on server: " + extras.toString());

            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                final GcmProtocol gcmProto = (GcmProtocol)getConnection(GcmProtocol.class);
                Log.i(TAG, "gcmProto=" + gcmProto);
                if (gcmProto == null) return;
                gcmProto.onHandleGcmIntent(intent);
            }
        }
    }

    private boolean isProtocolEnabled(Class protocol) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getBoolean("enable_" + protocol.getSimpleName(), true);
    }

    private IProtocol chooseConnection() {
        for(Class protocol : registeredProtocols) {
            // Bluetooth enabled and ready?
            if (isProtocolEnabled(protocol)) {
                IProtocol p = getOrCreateConnection(protocol);
                if (p.canSend()) {
                    return p;
                }
            }
        }
        return null;
    }


    private void acknowledgePacket(Envelope packetToAck) {
        AckPacket ack = new AckPacket(packetToAck.uuid, packetToAck.recipientPublicKey, packetToAck.senderPublicKey);
        ack.setDSR(packetToAck.getPath());
        sendPacket(this, ack);
    }


    // static helper method to enqueue
    public static void sendPacket(Context ctx, Packet packet) {
        // remember this packet
        processedPackets.enqueue(packet);

        // and dispatch sending intent
        final Intent intent = new Intent(ctx, ConnectionManager.class);
        intent.setAction(ConnectionManager.SENDMESSAGE_ACTION);
        intent.putExtra("packet", packet);
        ctx.startService(intent);
    }

    /**
     * this should not be called on a retransmission as it adds the current node as path node
     * @param ctx
     * @param envelope
     */
    public static void sendEnvelope(Context ctx, Envelope envelope) {
        if (envelope.getHopCount() > 0) throw new IllegalArgumentException("This method only sends fresh envelopes");

        // retrieve best path to target from RoutingManager, if known. otherwise use flooding
        List<byte[]> hopsToTarget = routingManager.getPath(envelope);
        if (hopsToTarget == null) envelope.setFlooding();
        else envelope.setDSR(hopsToTarget);

        sendPacket(ctx, envelope);
    }
    public static void sendMessage(Context ctx, Message message) {
        sendEnvelope(ctx, Envelope.fromMessage(message, true));
    }
}
