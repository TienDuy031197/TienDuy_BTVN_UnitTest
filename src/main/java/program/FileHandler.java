package program;

import UnitTest.AddNumber;
import org.apache.log4j.Logger;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileHandler extends GeneralHandlerAbstract implements HandlerFileInterface {
    private static final int LENGHT_BYTE = 10240;
    private static final Logger LOGGER = Logger.getLogger(MailHandler.class.getName());

    private String addressTo;
    private String pathFile;

    public FileHandler(String addressTo, String pathFile) {
        this.addressTo = addressTo;
        this.pathFile = pathFile;
    }

    public void extractFile() {
        final String OUTPUT_FOLDER = Config.EXTRACT_PATH;
        String FILE_PATH = this.pathFile;
        //Create the Output folder if it does not exist.
        File folder = new File(OUTPUT_FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        //Create a buffer.
        byte[] buffer = new byte[LENGHT_BYTE];
        ZipInputStream zipIs = null;
        try {
            // Create ZipInputStream object to read file from a path (path).
            zipIs = new ZipInputStream(new FileInputStream(FILE_PATH));
            ZipEntry entry = null;
            // Browse each Entry (From top to bottom to the end)
            while ((entry = zipIs.getNextEntry()) != null) {
                String entryName = entry.getName();
                if ((entry = zipIs.getNextEntry()) != null) {
                    sendMail(addressTo, Config.TOPIC_SEND, Config.SINGLE_FILE);
                    LOGGER.info(Config.SEND_SUCCESS + Config.SINGLE_FILE);
                    break;
                }
                String extension = entryName.substring(entryName.lastIndexOf('.'));
                if (extension.equalsIgnoreCase(Config.EXTENSION_JAVA)) {
                    String outFileName = OUTPUT_FOLDER + entryName;
                    LOGGER.info("entry: " + entryName);
                    LOGGER.info("Unzip: " + outFileName);
                    if (entry.isDirectory()) {
                        //Create folders.
                        new File(outFileName).mkdirs();
                    } else {
                        //Create a Stream to write data to the file
                        FileOutputStream fos = new FileOutputStream(outFileName);
                        int len;
                        //Read data on current Entry.
                        while ((len = zipIs.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                        fos.close();
                        autoDot();
                    }
                } else {
                    sendMail(addressTo, Config.TOPIC_SEND, Config.FILE_JAVA);
                    LOGGER.info(Config.SEND_SUCCESS + Config.FILE_JAVA);
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        } finally {
            try {
                if (zipIs != null) {
                    zipIs.close();
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        }
    }

    public void sendMail(String mailTo, String subject, String message) {
        Session session = Session.getInstance(setSMTPProperties(), createAuthentication());
        // creates a new e-mail message
        Message msg = new MimeMessage(session);

        try {
            msg.setFrom(new InternetAddress(Config.USER_NAME));
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

    private Properties setSMTPProperties() {
        // sets SMTP server properties
        Properties properties = new Properties();
        properties.put("mail.smtp.host", Config.HOST_SMTP);
        properties.put("mail.smtp.port", Config.PORT_SMTP);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        return properties;
    }

    private Authenticator createAuthentication() {
        // creates a new session with an authenticator
        Authenticator authen = new Authenticator() {
            @Override
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(Config.USER_NAME, Config.PASS);
            }
        };
        return authen;
    }

    private void autoDot() {
        Integer[] a = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        Integer[] b = {10, 9, 8, 7, 6, 5, 4, 3, 2, 1};
        Integer[] result = {11, 11, 11, 11, 11, 11, 11, 11, 11, 11};
        Integer totalScore = 0;
        for (int i = 0; i < 10; i++) {
            AddNumber addNumber = new AddNumber(a[i], b[i]);
            if (addNumber.add() == result[i]) {
                totalScore += 1;
            }
        }
        sendMail(addressTo, Config.RESULT, Config.MESSAGE_RESULT + totalScore);
        LOGGER.info("Score: " + totalScore);
    }
}
