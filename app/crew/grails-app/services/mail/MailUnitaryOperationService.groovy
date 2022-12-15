package mail

import grails.compiler.GrailsCompileStatic
import org.taack.Attachment
import org.taack.User

//import jakarta.mail.*
//import jakarta.mail.internet.InternetAddress
//import jakarta.mail.internet.MimeMessage
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage


import static javax.mail.Session.getInstance
//import org.apache.commons.mail.*

/*
 * TODO: Improve that class ...
 */

@GrailsCompileStatic
class MailUnitaryOperationService {

    String sendHtmlMail(User fromUser, String subject, String body, List<Attachment> attachmentList = null, User... toUsers) {
        def toList = toUsers.collect {
            new InternetAddress(it.mail, "${it.firstName} ${it.lastName}")
        }
        if (fromUser) sendMail("${fromUser.firstName} ${fromUser.lastName}", fromUser.mail, toList, null, subject, body, null, null, attachmentList)
        else sendHtmlMail(subject, body, attachmentList, toUsers)
    }

    String sendHtmlMail(String subject, String body, List<Attachment> attachmentList = null, User... toUsers) {
        def toList = toUsers.collect {
            new InternetAddress(it.mail, "${it.firstName} ${it.lastName}")
        }
        sendMail("Intranet", "crm@citelbe.com", toList, null, subject, body, null, null, attachmentList)
    }

    String sendTxtMail(User fromUser, String subject, String body, List<Attachment> attachmentList = null, User... toUsers) {
        def toList = toUsers.collect {
            new InternetAddress(it.mail, "${it.firstName} ${it.lastName}")
        }
        sendMail("${fromUser.firstName} ${fromUser.lastName}", fromUser.mail, toList, null, subject, null, body, null, attachmentList)
    }

    String sendTxtMail(String subject, String body, List<Attachment> attachmentList = null, User... toUsers) {
        def toList = toUsers.collect {
            new InternetAddress(it.mail, "${it.firstName} ${it.lastName}")
        }
        sendMail("Intranet", "crm@citelbe.com", toList, null, subject, null, body, null, attachmentList)
    }

    /**
     * Send a single mail to a list of recipients
     * @param fromName name of the sender
     * @param from address of the sender
     * @param toList list of InternetAddress of recipients
     * @param ccList list of InternetAddress of CC recipients
     * @param subject subject
     * @param bodyHtml html body
     * @param bodyTxt alternative txt body
     * @param dataSourceList list of ByteArrayDataSource
     * @param attachmentList list of Attachment
     * @return
     */
    private static String sendMail(String fromName, String from, List<InternetAddress> toList,
                                   List<InternetAddress> ccList, String subject, String bodyHtml, String bodyTxt,
                                   def dataSourceList, List<Attachment> attachmentList) {

        if (!toList?.size())
            return (false)
        try {
            Message message = initMessage()
            message.setFrom new InternetAddress(from, fromName)
            message.setSubject(subject)
            message.setReplyTo new InternetAddress(from, fromName)
            message.setRecipient Message.RecipientType.BCC, new InternetAddress(from, fromName)
            message.setRecipients Message.RecipientType.TO, toList.toArray() as InternetAddress[]

            if (ccList && !ccList?.empty)
                message.setRecipients Message.RecipientType.CC, ccList.toArray() as InternetAddress[]
            if (bodyHtml) message.setContent(bodyHtml, "text/html")
            if (bodyTxt) message.setText(bodyTxt)
            Transport.send(message)
        } catch(MessagingException e) {
            println "error sending email: ${e}"
            e.printStackTrace()
        }
        ""
    }

    /**
     * Send a single mail to a list of recipients
     * @param fromName name of the sender
     * @param from address of the sender
     * @param toList list of InternetAddress of recipients
     * @param ccList list of InternetAddress of CC recipients
     * @param subject subject
     * @param body body
     * @param dataSourceList list of ByteArrayDataSource
     * @param attachmentList list of Attachment
     * @return
     */

    private static MimeMessage initMessage() {

        Properties props = new Properties()
        props.put("mail.smtp.auth", "true")
        props.put("mail.smtp.starttls.enable", "true")
        props.put("mail.smtp.ssl.enable", "true")
        props.put("mail.smtp.host", "smtp.gmail.com")
        props.put("mail.smtp.port", "465")
        props.put("mail.smtp.from", "crm@citelbe.com")
//        props.put("mail.debug", "true")

        Session session = getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication("crm@citelbe.com", "kfsypqyuvegghkym")
                    }
                })
        new MimeMessage(session)
    }

//    private DefaultAuthenticator defaultAuthenticator
    private long defaultAuthenticatorTime = 0

//    DefaultAuthenticator getDefaultAuthenticator() {
//        if (!defaultAuthenticator) {// || System.currentTimeMillis() - defaultAuthenticatorTime > 100000) {
//            defaultAuthenticatorTime = System.currentTimeMillis()
//            defaultAuthenticator = new DefaultAuthenticator("crm@citelbe.com", "kfsypqyuvegghkym")
//        }
//        return defaultAuthenticator
//    }

//    /**
//     * Send a single mail without file attached
//     * @param toName name of the recipient
//     * @param to address of the recipient
//     * @param fromName name of the sender
//     * @param from address of the sender
//     * @param subject subject
//     * @param body body
//     * @return mail sent or not
//     */
//    String sendTxtMail(String toName, String to, String fromName, String from, String subject, final String body) {
//        log.info "sendTxtMail AUO111 toName: ${toName}, to: ${to}, fromName: ${fromName}, from: ${from}"
//        Email email = new SimpleEmail()
//        email.sslSmtpPort = 465
//        //email.smtpPort = 587
//        email.authenticator = getDefaultAuthenticator()
//        email.hostName = "smtp.gmail.com"
//        email.setSSLOnConnect(true)
//        email.bounceAddress = "crm@citelbe.com"
//        email.from = from
//        email.addReplyTo(from)
//        email.subject = subject
//        email.setContent(body, EmailConstants.TEXT_HTML)
//        //email.addHeader()
//        //email.headers["Message-ID"]
//        email.addTo(to)
//        return email.send()
//    }

//    private static final String APPLICATION_NAME = "TaackCRM Marketing"
//    private static final File DATA_STORE_DIR = new File(
//            System.getProperty("user.home"), ".credentials/gmail-java-quickstart")
//
//    private static FileDataStoreFactory DATA_STORE_FACTORY
//    private static HttpTransport HTTP_TRANSPORT
//
//    private static final JsonFactory JSON_FACTORY =
//            JacksonFactory.getDefaultInstance()
//    private static final List<String> SCOPES =
//            Arrays.asList(GmailScopes.GMAIL_READONLY)
//    static {
//        try {
//            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
//            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR)
//        } catch (Throwable t) {
//            t.printStackTrace()
//            System.exit(1)
//        }
//    }

//    private static Credential authorize() throws IOException {
//        // Load client secrets.
//        InputStream incs =
//                MailUnitaryOperationService.class.getResourceAsStream("/client_secret.json")
//        GoogleClientSecrets clientSecrets =
//                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(incs))
//
//        // Build flow and trigger user authorization request.
//        GoogleAuthorizationCodeFlow flow =
//                new GoogleAuthorizationCodeFlow.Builder(
//                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
//                        .setDataStoreFactory(DATA_STORE_FACTORY)
//                        .setAccessType("offline")
//                        .build()
//        Credential credential = new AuthorizationCodeInstalledApp(
//                flow, new LocalServerReceiver()).authorize("user")
//        System.out.println(
//                "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath())
//        return credential
//    }

//    static Gmail getGmailService() throws IOException {
//        Credential credential = authorize()
//        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
//                .setApplicationName(APPLICATION_NAME)
//                .build()
//    }


    List<String> makeCcList(String cc, String ccs) {
        log.info "makeCcList cc: $cc, ccs: $ccs"
        List<String> ccList
        if (cc || ccs) {
            cc += ccs ? ", " + ccs : ""
            cc = cc?.replace(';', ',')
        }
        ccList = cc ? cc.split(',').collect { String sit ->
            sit.trim()
        }.findAll { !it.empty }.unique() : null
        return ccList
    }
}
