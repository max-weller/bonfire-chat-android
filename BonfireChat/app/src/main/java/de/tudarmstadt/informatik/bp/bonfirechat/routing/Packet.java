package de.tudarmstadt.informatik.bp.bonfirechat.routing;

import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import de.tudarmstadt.informatik.bp.bonfirechat.models.Identity;

/**
 * Created by johannes on 29.07.15.
 *
 * packets describe basic pieces of information to be transmitted over the network. Subclassing
 * is needed to specify the particular type of packet, e.g. payload containing an actual message,
 * or a control sequence such as ACK.
 */
public abstract class Packet implements Serializable {
    // "sequence number"
    public final UUID uuid;

    // indicates type of this packet, for further casting
    protected PacketType type;

    // recipient of this packet
    public final byte[] recipientPublicKey;
    // sender of this packet
    public final byte[] senderPublicKey;

    // previous and next hops
    private List<byte[]> path;
    private List<byte[]> nextHops;

    private long timeSent;

    private int routingMode = ROUTING_MODE_NONE;

    public static final int ROUTING_MODE_NONE = 0;
    public static final int ROUTING_MODE_FLOODING = 1;
    public static final int ROUTING_MODE_DSR = 2;
    public byte[] MacAddress= null;

    public long getTimeSent() {
        return timeSent;
    }

    public void setTimeSent(long timeSent) {
        this.timeSent = timeSent;
    }

    public Packet(byte[] senderPublicKey, byte[] recipientPublicKey, UUID uuid) {
        this.senderPublicKey = senderPublicKey;
        this.recipientPublicKey = recipientPublicKey;
        this.uuid = uuid;
        this.path = new ArrayList<>();
    }

    public PacketType getType() {
        return type;
    }

    public List<byte[]> getPath() {
        return path;
    }

    public List<byte[]> getNextHops() {
        return nextHops;
    }

    public byte[] getNextHop() {
        if (routingMode == ROUTING_MODE_DSR)
            return nextHops.get(nextHops.size() - 1);
        else
            return null;
    }

    public void removeNextHop() {
        if (routingMode != ROUTING_MODE_DSR) throw new IllegalStateException("Not a DSR packet");
        nextHops.remove(nextHops.size() - 1);
    }

    public void addPathNode(byte[] id) {
        path.add(id);
    }

    public boolean isFlooding() {
        return routingMode == ROUTING_MODE_FLOODING;
    }

    public void setFlooding() {
        this.routingMode = ROUTING_MODE_FLOODING;
        this.nextHops = new ArrayList<byte[]>();
    }
    public void setDSR(List<byte[]> nextHops) {
        this.routingMode = ROUTING_MODE_DSR;
        this.nextHops = nextHops;
    }

    public int getHopCount() {
        return path.size();
    }

    public boolean hasRecipient(Identity id) {
        return (Arrays.equals(recipientPublicKey, id.publicKey.asByteArray()));
    }

    @Override
    public boolean equals(Object otherObject){
        if (otherObject == null || !(otherObject instanceof Packet)) return false;
        Packet packet = (Packet) otherObject;
        if(uuid.equals(packet.uuid) && type == packet.type )
            return true;
        return false;
    }

    @Override
    public String toString() {
        return "Packet(" + getType().toString() + ", " + uuid.toString() + ", routing=" + String.valueOf(routingMode) + ", hopCount=" + String.valueOf(path.size()) + ")";
    }
}


