package com.dayve22.Chronos.executor;

import org.springframework.stereotype.Component;


@Component
public class CommandJobData {
    private String command;
    private String workingDirectory;
    private Integer timeoutSeconds;

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    public String getWorkingDirectory() { return workingDirectory; }
    public void setWorkingDirectory(String workingDirectory) { this.workingDirectory = workingDirectory; }
    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
}
