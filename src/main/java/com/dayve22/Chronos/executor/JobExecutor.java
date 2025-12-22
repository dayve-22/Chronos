package com.dayve22.Chronos.executor;

import com.dayve22.Chronos.entity.Job;
import com.dayve22.Chronos.entity.JobExecution;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JobExecutor {

    private static final Logger logger = LoggerFactory.getLogger(JobExecutor.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void execute(Job job, JobExecution execution) throws Exception {
        logger.info("Executing job: {} (type: {})", job.getId(), job.getType());

        switch (job.getType()) {
            case COMMAND:
                executeCommand(job, execution);
                break;
            case EMAIL:
                sendEmail(job, execution);
                break;
            default:
                throw new IllegalArgumentException("Unknown job type: " + job.getType());
        }
    }

    private void executeCommand(Job job, JobExecution execution) throws Exception {
        CommandJobData data = objectMapper.readValue(job.getJobData(), CommandJobData.class);

        ProcessBuilder pb = new ProcessBuilder();

        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("win");

        if (data.getCommand().startsWith("[")) {
            String[] cmdArray = objectMapper.readValue(data.getCommand(), String[].class);
            pb.command(cmdArray);
        } else {
            if (isWindows) {
                pb.command("cmd.exe", "/c", data.getCommand());
            } else {
                pb.command("sh", "-c", data.getCommand());
            }
        }

        if (data.getWorkingDirectory() != null) {
            pb.directory(new java.io.File(data.getWorkingDirectory()));
        }

        pb.redirectErrorStream(true);

        Process process = pb.start();

        // Read output
        StringBuilder output = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean finished = process.waitFor(
                data.getTimeoutSeconds() != null ? data.getTimeoutSeconds() : 300,
                java.util.concurrent.TimeUnit.SECONDS
        );

        if (!finished) {
            process.destroy();
            throw new Exception("Command timed out");
        }

        int exitCode = process.exitValue();
        execution.setOutput(output.toString());

        if (exitCode != 0) {
            throw new Exception("Exit code " + exitCode + ": " + output.toString());
        }

        logger.info("Command executed successfully for job: {}", job.getId());
    }

    private void sendEmail(Job job, JobExecution execution) throws Exception {
        EmailJobData data = objectMapper.readValue(job.getJobData(), EmailJobData.class);

        // Use JavaMail API or email service
        javax.mail.Session session = javax.mail.Session.getInstance(
                getEmailProperties(data),
                new javax.mail.Authenticator() {
                    protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                        return new javax.mail.PasswordAuthentication(
                                data.getSmtpUsername(),
                                data.getSmtpPassword()
                        );
                    }
                }
        );

        javax.mail.internet.MimeMessage message = new javax.mail.internet.MimeMessage(session);
        message.setFrom(new javax.mail.internet.InternetAddress(data.getFromEmail()));

        // Handle multiple recipients
        for (String to : data.getToEmails()) {
            message.addRecipient(
                    javax.mail.Message.RecipientType.TO,
                    new javax.mail.internet.InternetAddress(to)
            );
        }

        message.setSubject(data.getSubject());

        if (data.isHtml()) {
            message.setContent(data.getBody(), "text/html; charset=utf-8");
        } else {
            message.setText(data.getBody());
        }

        javax.mail.Transport.send(message);

        execution.setOutput("Email sent successfully to: " + String.join(", ", data.getToEmails()));
        logger.info("Email sent successfully for job: {}", job.getId());
    }

    private java.util.Properties getEmailProperties(EmailJobData data) {
        java.util.Properties props = new java.util.Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", data.getSmtpHost());
        props.put("mail.smtp.port", data.getSmtpPort());
        props.put("mail.smtp.ssl.trust", data.getSmtpHost());
        return props;
    }
}

