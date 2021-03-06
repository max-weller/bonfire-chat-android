package de.tudarmstadt.informatik.bp.bonfirechat.models.test;

import org.abstractj.kalium.keys.KeyPair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import de.tudarmstadt.informatik.bp.bonfirechat.models.Contact;
import de.tudarmstadt.informatik.bp.bonfirechat.routing.Envelope;
import de.tudarmstadt.informatik.bp.bonfirechat.models.Message;
import de.tudarmstadt.informatik.bp.bonfirechat.models.MyPublicKey;

/**
 * Created by jonas on 16.07.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class EnvelopeTest {

    public Envelope envelope;
    public Message message;
    public UUID uuid;
    public Date date;
    public Contact contact;
    public byte[] recipient = {42};
    public byte[] senderPublicKey = {42};
    public byte[] encryptedBody = {44};
    public MyPublicKey myPublicKey;

    /**
     * initializes an envelope using a mocked version of KeyPair before testing
     */
    @Before
    public void initTests(){
        uuid = new UUID(0,0);
        date = new Date(42000);
        envelope = new Envelope(uuid, date, recipient, senderPublicKey, encryptedBody);
        KeyPair keyPairMock = Mockito.mock(KeyPair.class);
        org.abstractj.kalium.keys.PublicKey publicKeyMock = Mockito.mock(org.abstractj.kalium.keys.PublicKey.class);
        when(keyPairMock.getPublicKey()).thenReturn(publicKeyMock);
        myPublicKey = mock(MyPublicKey.class);
        when(myPublicKey.asByteArray()).thenReturn(recipient);

        contact = new Contact("nickname", "Nick", "Name", "", myPublicKey, "", "", 0);
    }

    /**
     * Tests correct creation of envelope from message
     * <br><br>
     * uuid, the recipient's public key and sender's public key should match
     * Flags should be initialized with zero
     */
    @Test
    public void testFromMessage(){
        message = new Message("body", contact, date, Message.FLAG_PROTO_BT, contact);
        Envelope newEnvelope = Envelope.fromMessage(message);
        assertEquals(message.uuid, newEnvelope.uuid);
        assertEquals(recipient, newEnvelope.recipientPublicKey);
        assertTrue (Arrays.equals(senderPublicKey, newEnvelope.senderPublicKey));
        assertEquals(0, newEnvelope.flags);
    }

    /**
     * Tests correct behaviour of hasFlag
     * <br><br>
     * If specified flag is set, hasFlag() should return true, false otherwise
     */
    @Test
    public void testHasFlag(){
        assertFalse(envelope.hasFlag(Message.FLAG_ENCRYPTED));
        assertFalse(envelope.hasFlag(Message.FLAG_PROTO_BT));
        assertFalse(envelope.hasFlag(Message.FLAG_PROTO_CLOUD));
        assertFalse(envelope.hasFlag(Message.FLAG_PROTO_WIFI));
    }
}
