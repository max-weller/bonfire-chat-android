package de.tudarmstadt.informatik.bp.bonfirechat.routing;

import android.content.Context;
import android.util.Log;

import org.abstractj.kalium.crypto.Box;
import org.abstractj.kalium.keys.PublicKey;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
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
    public final byte[] senderPublicKey;
    public byte[] encryptedBody;
    public byte[] nonce;
    public int flags;

    public static final int FLAG_BINARY = 1;
    public static final int FLAG_LOCATION = 2;
    public static final int FLAG_ENCRYPTED = 4;
    public static final int FLAG_TRACEROUTE = 8;

    public Envelope(UUID uuid, Date sentTime, byte[] recipientPublicKey, byte[] senderPublicKey, byte[] encryptedBody) {
        super(senderPublicKey, recipientPublicKey, uuid);
        this.sentTime = sentTime;
        this.senderPublicKey = senderPublicKey;
        this.encryptedBody = encryptedBody;
    }


    public static Envelope fromMessage(Message message) {
        byte[] publicKey = message.recipients.get(0).getPublicKey().asByteArray();
        byte[] bodyBytes;
        if (message.hasFlag(Message.FLAG_IS_FILE)) {
            try {
                File file = new File(message.body);
                FileInputStream inputStream = new FileInputStream(file);
                bodyBytes = new byte[(int) file.length()];
                inputStream.read(bodyBytes);
                inputStream.close();
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to convert IS_FILE message: " + e.getMessage());
            }
        } else {
            // Concatenate Sender Nickname and Message Text to be stored in Encrypted Body
            String parts = message.sender.getNickname() + "|" + message.body;
            bodyBytes = parts.getBytes(Charset.forName("UTF-8"));
        }
        Envelope envelope = new Envelope(
                message.uuid,
                new Date(),
                publicKey,
                message.sender.getPublicKey().asByteArray(),
                bodyBytes);
        if (message.hasFlag(Message.FLAG_ENCRYPTED)) {
            Identity sender = (Identity) message.sender;
            Box crypto = new Box(new PublicKey(publicKey), sender.privateKey);
            envelope.nonce = CryptoHelper.generateNonce();
            envelope.encryptedBody = crypto.encrypt(envelope.nonce, envelope.encryptedBody);
            envelope.flags |= FLAG_ENCRYPTED;
        }
        if (message.hasFlag(Message.FLAG_IS_FILE)) {
            envelope.flags |= FLAG_BINARY;
        }

        if (message.hasFlag(Message.FLAG_IS_LOCATION)) {
            envelope.flags |= FLAG_LOCATION;
        }
        return envelope;
    }

    public Message toMessage(Context ctx) {
        byte[] body = encryptedBody;
        int msgFlags = 0;
        Contact theContact;
        if (getRoutingMode() == ROUTING_MODE_DSR) {
            msgFlags |= Message.FLAG_ROUTING_DSR;
        } else if (getRoutingMode() == ROUTING_MODE_FLOODING) {
            msgFlags |= Message.FLAG_ROUTING_FLOODING;
        }
        if (hasFlag(FLAG_ENCRYPTED)) {
            Identity id = BonfireData.getInstance(ctx).getDefaultIdentity();
            Box crypto = new Box(new PublicKey(senderPublicKey), id.privateKey);
            body = crypto.decrypt(nonce, body);
            msgFlags |= Message.FLAG_ENCRYPTED;
        }
        if (hasFlag(FLAG_LOCATION)) {
            msgFlags |= Message.FLAG_IS_LOCATION;
        }

        String messageBody;
        if (hasFlag(FLAG_BINARY)) {
            theContact = BonfireData.getInstance(ctx).getContactByPublicKey(senderPublicKey);
            File target = Message.getImageFile(this.uuid);
            try (FileOutputStream os = new FileOutputStream(target)) {
                os.write(body);
                os.close();
                messageBody = target.getAbsolutePath();
                msgFlags |= Message.FLAG_IS_FILE;
            } catch (IOException ex) {
                messageBody = "ERROR:" + ex.getMessage();
            }
        } else {
            String[] parts = new String(body, Charset.forName("UTF-8")).split("\\|", 2);
            theContact = Contact.findOrCreate(ctx, senderPublicKey, parts[0]);
            messageBody = parts[1];
        }
        Message message = new Message(messageBody, theContact, sentTime, msgFlags, uuid);
        message.traceroute = getTraceroute();
        Log.w("Envelope", "unpacking envelope. traceroute : " + message.traceroute);
        return message;
    }

    public boolean hasFlag(int flag) {
        return (flag & flags) == flag;
    }

    @Override
    public String toString() {
        return super.toString() + ":Envelope(...)";
    }
}
