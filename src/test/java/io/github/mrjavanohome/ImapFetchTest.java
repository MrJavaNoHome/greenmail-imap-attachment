package io.github.mrjavanohome;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImapFetchTest {

    @RegisterExtension
    public final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP_IMAP);

    @Test
    void sendAndReceiveUsingGreenMail() throws Exception {
        //given users
        User from = new User("foo@example.com", "foo");
        User to = new User("bar@example.com", "secret-pwd");

        //given send mail
        Session smtpSession = greenMail.getSmtp().createSession();
        Message msg = createMessage(from, to, smtpSession);
        Transport.send(msg);
        greenMail.setUser(to.getLogin(), to.getLogin(), to.getPass());

        //when
        Properties imapProperties = System.getProperties();
        Session imapSession = greenMail.getImap().createSession(imapProperties);
        Store store = imapSession.getStore("imap");
        store.connect(to.getLogin(), to.getPass());
        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);
        Message msgReceived = inbox.getMessage(1);

        //then
        final MimeMultipart content = (MimeMultipart) msgReceived.getContent();
        final BodyPart attachmentPart = content.getBodyPart(1);
        final InputStream inputStream = attachmentPart.getInputStream();
        final String attachmentName = attachmentPart.getFileName();
        final byte[] attachmentBytes = inputStream.readAllBytes();

        assertEquals(msg.getSubject(), msgReceived.getSubject());
        assertTrue(attachmentName.startsWith("test-attachment"));
        assertEquals(1, greenMail.getReceivedMessagesForDomain(to.getLogin()).length);
        assertEquals("Hello Github", new String(attachmentBytes));
    }

    private Message createMessage(User from, User to, Session smtpSession) throws MessagingException, IOException {
        Message msg = new MimeMessage(smtpSession);
        msg.setFrom(new InternetAddress(from.getLogin()));
        msg.addRecipient(Message.RecipientType.TO, new InternetAddress(to.getLogin()));
        msg.setSubject("Email to local mail server");

        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText("Fetch me via IMAP");

        MimeBodyPart attachmentPart = createAttachmentPart();

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);
        multipart.addBodyPart(attachmentPart);
        msg.setContent(multipart);
        return msg;
    }

    private MimeBodyPart createAttachmentPart() throws IOException, MessagingException {
        MimeBodyPart attachmentPart = new MimeBodyPart();

        final File attachmentFile = File.createTempFile("test-attachment", "txt");
        new FileWriter(attachmentFile)
                .append("Hello Github")
                .close();
        attachmentPart.attachFile(attachmentFile);
        return attachmentPart;
    }


    private static final class User {

        private final String login;
        private final String pass;

        private User(String login, String pass) {
            this.login = login;
            this.pass = pass;
        }

        public String getLogin() {
            return login;
        }

        public String getPass() {
            return pass;
        }
    }


}
