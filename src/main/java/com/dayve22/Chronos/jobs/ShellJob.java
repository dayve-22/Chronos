package com.dayve22.Chronos.jobs;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Slf4j
@Component
public class ShellJob extends QuartzJobBean {

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        String command = context.getMergedJobDataMap().getString("command");
        log.info("Executing Command: {}", command);

        ProcessBuilder processBuilder = new ProcessBuilder();
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        if (isWindows) {
            processBuilder.command("cmd.exe", "/c", command);
        } else {
            processBuilder.command("sh", "-c", command);
        }
        try {
            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("Command Success. Output:\n{}", output);
            } else {
                // Capture Error Output if failed
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                StringBuilder errorOutput = new StringBuilder();
                while ((line = errorReader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }

                log.error("Command Failed (Code {}). Error:\n{}", exitCode, errorOutput);
                throw new RuntimeException("Command execution failed: " + errorOutput);
            }

        } catch (Exception e) {
            log.error("Error running shell command", e);
            throw new JobExecutionException(e);
        }
    }
}
