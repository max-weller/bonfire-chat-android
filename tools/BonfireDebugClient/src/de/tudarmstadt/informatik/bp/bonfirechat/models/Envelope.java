package de.tudarmstadt.informatik.bp.bonfirechat.models;

import org.abstractj.kalium.crypto.Box;
import org.abstractj.kalium.crypto.Random;
import org.abstractj.kalium.keys.KeyPair;
import org.abstractj.kalium.keys.PublicKey;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
 * Created by johannes on 15.06.15.
 *
 * An Envelope represents a message on its way. Envelopes are for being sent and recieved and
 * should describe a message uniquely, including network parameters.
 */
public class Envelope implements Serializable {
	static final long serialVersionUID =-4609818010132127369L;

    public UUID uuid;
    public int hopCount;
    public Date sentTime;
    public ArrayList<byte[]> recipientsPublicKeys;
    //TODO eventuell rauswerfen
    public String senderNickname;
    public byte[] senderPublicKey;
    public byte[] encryptedBody;
    public byte[] nonce;
    public int flags = 0;

    public static final int FLAG_ENCRYPTED = 4;
    public static final int FLAG_TRACEROUTE = 8;


    public Envelope(UUID uuid, int hopCount, Date sentTime, ArrayList<byte[]> recipientsPublicKeys, String senderNickname, byte[] senderPublicKey, byte[] encryptedBody) {
        this.uuid = uuid;
        this.hopCount = hopCount;
        this.sentTime = sentTime;
        this.recipientsPublicKeys = recipientsPublicKeys;
        this.senderNickname = senderNickname;
        this.senderPublicKey = senderPublicKey;
        this.encryptedBody = encryptedBody;
    }

	public Envelope(byte[] recipientPublicKey, String senderNickname, KeyPair senderKey, String message) throws UnsupportedEncodingException {
		this(UUID.randomUUID(), 0, new Date(), null, senderNickname, senderKey.getPublicKey().toBytes(), null);

		recipientsPublicKeys = new ArrayList<byte[]>();
		recipientsPublicKeys.add(recipientPublicKey);

		Box crypto = new Box(new PublicKey(recipientPublicKey), senderKey.getPrivateKey());
		nonce = new Random().randomBytes(24);
		encryptedBody = crypto.encrypt(nonce, message.getBytes("UTF-8"));
		flags |= FLAG_ENCRYPTED;

	}

/*
    public static Envelope fromMessage(Message message, boolean encrypt) {
        ArrayList<byte[]> publicKeys = new ArrayList<>();
        for(Contact recipient: message.recipients) {
            publicKeys.add(recipient.getPublicKey().asByteArray());
        }
        Envelope envelope = new Envelope(
                message.uuid,
                0,
                new Date(),
                publicKeys,
                message.sender.getNickname(),
                message.sender.getPublicKey().asByteArray(),
                message.body.getBytes(Charset.forName("UTF-8")));
        if (encrypt) {
            Identity sender = (Identity)message.sender;
            Box crypto = new Box(new PublicKey(publicKeys.get(0)), sender.privateKey);
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


    public boolean containsRecipient(Identity id) {
        for (byte[] publicKey: recipientsPublicKeys) {
            if (Arrays.equals(publicKey, id.publicKey.asByteArray())) {
                return true;
            }
        }
        return false;
    }
*/
    public boolean hasFlag(int flag) {
        return (flag & flags) == flag;
    }
}
