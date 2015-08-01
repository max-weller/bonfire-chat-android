package de.tudarmstadt.informatik.bp.bonfirechat.routing;

import android.content.Context;

import org.abstractj.kalium.crypto.Box;
import org.abstractj.kalium.keys.PublicKey;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import de.tudarmstadt.informatik.bp.bonfirechat.data.BonfireData;
import de.tudarmstadt.informatik.bp.bonfirechat.helper.CryptoHelper;
import de.tudarmstadt.informatik.bp.bonfirechat.models.Contact;
import de.tudarmstadt.informatik.bp.bonfirechat.models.Identity;
import de.tudarmstadt.informatik.bp.bonfirechat.models.Message;

/**
 * Created by johannes on 15.06.15.
 *
 * An Envelope represents a message on its way. Envelopes are for being sent and recieved and
 * should describe a message uniquely, including network parameters.
 */
public class Envelope extends PayloadPacket {

    public final Date sentTime;
    //TODO eventuell rauswerfen
    public final String senderNickname;
    public final byte[] senderPublicKey;
    public byte[] encryptedBody;
    public byte[] nonce;
    public int flags;

    public static final int FLAG_ENCRYPTED = 4;
    public static final int FLAG_TRACEROUTE = 8;


    public Envelope(UUID uuid, int hopCount, Date sentTime, byte[] recipientPublicKey, String senderNickname, byte[] senderPublicKey, byte[] encryptedBody) {
        super(recipientPublicKey, uuid);
        this.hopCount = hopCount;
        this.sentTime = sentTime;
        this.senderNickname = senderNickname;
        this.senderPublicKey = senderPublicKey;
        this.encryptedBody = encryptedBody;
    }


    public static Envelope fromMessage(Message message, boolean encrypt) {
        byte[] publicKey = message.recipients.get(0).getPublicKey().asByteArray();
        Envelope envelope = new Envelope(
                message.uuid,
                0,
                new Date(),
                publicKey,
                message.sender.getNickname(),
                message.sender.getPublicKey().asByteArray(),
                message.body.getBytes(Charset.forName("UTF-8")));
        if (encrypt) {
            Identity sender = (Identity)message.sender;
            Box crypto = new Box(new PublicKey(publicKey), sender.privateKey);
            envelope.nonce = CryptoHelper.generateNonce();
            envelope.encryptedBody = crypto.encrypt(envelope.nonce, envelope.encryptedBody);
            envelope.flags |= FLAG_ENCRYPTED;
        }
        return envelope;
    }

    public Message toMessage(Context ctx) {
        byte[] body = encryptedBody;
        int msgFlags = 0;
        if (hasFlag(FLAG_ENCRYPTED)) {
            Identity id = BonfireData.getInstance(ctx).getDefaultIdentity();
            Box crypto = new Box(new PublicKey(senderPublicKey), id.privateKey);
            body = crypto.decrypt(nonce, body);
            msgFlags |= Message.FLAG_ENCRYPTED;
        }
        return new Message(
                new String(body, Charset.forName("UTF-8")),
                Contact.findOrCreate(ctx, senderPublicKey, senderNickname),
                sentTime,
                msgFlags,
                uuid);
    }


    public boolean hasFlag(int flag) {
        return (flag & flags) == flag;
    }
}