package program;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

public class MailHandler extends GeneralHandlerAbstract implements ReadMailInterface {
    private static final Logger LOGGER = Logger.getLogger(MailHandler.class.getName());

    private String protocol;
    private String host;
    private String port;
    private String userName;
    private String passWord;
    private String saveDirectory;

    private MailHandler(String protocol, String host, String port, String userName, String passWord) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.userName = userName;
        this.passWord = passWord;
    }

    public static void main(String[] args) {
        BasicConfigurator.configure();
        MailHandler mailHandler = new MailHandler(Config.PROTOCOL_IMAP, Config.HOST_IMAP, Config.PORT_IMAP, Config.USER_NAME, Config.PASS);
        mailHandler.setSaveDirectory(Config.DOWNLOAD_PATH);
        mailHandler.readMail();
    }

    private void setSaveDirectory(String saveDirectory) {
        this.saveDirectory = saveDirectory;
    }

    private Properties getServerProperties() {
        Properties properties = new Properties();
        properties.put(String.format("mail.%s.host", protocol), this.host);
        properties.put(String.format("mail.%s.port", protocol), this.port);
        properties.setProperty(
                String.format("mail.%s.socketFactory.class", protocol),
                "javax.net.ssl.SSLSocketFactory");
        properties.setProperty(
                String.format("mail.%s.socketFactory.fallback", protocol),
                "false");
        properties.setProperty(
                String.format("mail.%s.socketFactory.port", protocol),
                String.valueOf(port));

        return properties;
    }

    public void readMail() {
        Properties properties = getServerProperties();
        Session session = Session.getDefaultInstance(properties);
        try {
            // connects to the message store
            Store store = session.getStore(Config.PROTOCOL_IMAP);
            store.connect(userName, passWord);

            // opens the inbox folder
            Folder folderInbox = store.getFolder(Config.GET_FOLDER_EMAIL);
            folderInbox.open(Folder.READ_ONLY);

            // fetches new messages from server
            Message[] arrayMessages = folderInbox.getMessages();

            for (Message message : arrayMessages) {
                Address[] addresses = message.getFrom();
                String from = addresses[0].toString();
                String subject = message.getSubject();
                if (subject == null) {
                    sendMail(from, Config.TOPIC_SEND, Config.NOT_TOPIC);
                    LOGGER.info(Config.SEND_SUCCESS + Config.NOT_TOPIC);
                    continue;
                }
                if (subject.startsWith(Config.TOPIC_RECEIVE)) {

                    Multipart multiPart = (Multipart) message.getContent();
                    int numberOfParts = multiPart.getCount();
                    for (int partCount = 0; partCount < numberOfParts; partCount++) {
                        BodyPart part = multiPart.getBodyPart(partCount);
                        if (isFileZip(part)) {
                            String fileName = part.getFileName();
                            String extension = fileName.substring(fileName.lastIndexOf('.'));
                            if (extension.equalsIgnoreCase(Config.EXTENSION_ZIP)) {
                                String path = downLoad(fileName, part);
                                FileHandler fileHandler = new FileHandler(from, path);
                                fileHandler.extractFile();
                            } else {
                                sendMail(from, Config.TOPIC_SEND, Config.FILE_ZIP);
                                LOGGER.info(Config.SEND_SUCCESS + Config.FILE_ZIP);
                            }
                        } else {
                            sendMail(from, Config.TOPIC_SEND, Config.NOT_ATTACHMENT);
                            LOGGER.info(Config.SEND_SUCCESS + Config.NOT_ATTACHMENT);
                        }
                    }
                } else {
                    sendMail(from, Config.TOPIC_SEND, Config.NOT_RIGHT_TOPIC);
                    LOGGER.info(Config.SEND_SUCCESS + Config.NOT_RIGHT_TOPIC);
                }
            }
            // disconnect
            folderInbox.close(false);
            store.close();
        } catch (NoSuchProviderException ex) {
            LOGGER.error(ex.getMessage());
        } catch (MessagingException ex) {
            LOGGER.error(ex.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }


    private String downLoad(String fileName, BodyPart part) throws IOException {
        // save an attachment from a MimeBodyPart to a file
        String destFilePath = saveDirectory + fileName;

        FileOutputStream output = null;
        try {
            output = new FileOutputStream(destFilePath);
            InputStream input = part.getInputStream();

            byte[] buffer = new byte[10 * 1024];

            int byteRead;

            while ((byteRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, byteRead);
            }
            LOGGER.info("Download success!");
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
        } catch (MessagingException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } finally {
            if (output != null) {
                output.close();
            }
        }

        return destFilePath;
    }

    private boolean isFileZip(BodyPart bodyPart) {
        try {
            if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                return true;
            }
        } catch (MessagingException e) {
            LOGGER.error(e.getMessage());
            return false;
        }
        return false;
    }

    public void sendMail(String mailTo, String subject, String message) {
        Session session = Session.getInstance(setSMTPProperti(), createAuthen());
        // creates a new e-mail message
        Message msg = new MimeMessage(session);

        try {
            msg.setFrom(new InternetAddress(userName));
            InternetAddress[] toAddresses = {new InternetAddress(mailTo)};
            msg.setRecipients(Message.RecipientType.TO, toAddresses);
            msg.setSubject(subject);
            msg.setSentDate(new Date());
            // set plain text message
            msg.setText(message);
            // sends the e-mail
            Transport.send(msg);
        } catch (MessagingException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private Properties setSMTPProperti() {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", Config.HOST_SMTP);
        properties.put("mail.smtp.port", Config.PORT_SMTP);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        return properties;
    }

    private Authenticator createAuthen() {
        // creates a new session with an authenticator
        Authenticator authen = new Authenticator() {
            @Override
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(Config.USER_NAME, Config.PASS);
            }
        };
        return authen;
    }
}
