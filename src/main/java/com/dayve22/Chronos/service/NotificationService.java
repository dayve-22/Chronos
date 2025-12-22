package com.dayve22.Chronos.service;

import com.dayve22.Chronos.entity.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @Autowired
    private JavaMailSender mailSender;

    public void notifyJobFailure(Job job, String error) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(job.getUser().getEmail());
        message.setSubject("Job Failed: " + job.getName());
        message.setText(String.format(
                "Job '%s' (ID: %d) has failed after %d retries.\n\nError: %s",
                job.getName(), job.getId(), job.getMaxRetries(), error
        ));
        mailSender.send(message);
    }
}
