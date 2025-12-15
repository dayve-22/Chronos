package com.dayve22.Chronos.jobs;

import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Component
public class CommandExecutionJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(CommandExecutionJob.class);

    public static final String DATA_COMMAND = "command";
    public static final String DATA_RETRIES_ALLOWED = "retriesAllowed";
    public static final String DATA_RETRIES_COUNT = "retriesCount";

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        String command = dataMap.getString(DATA_COMMAND);
        int retriesAllowed = dataMap.containsKey(DATA_RETRIES_ALLOWED) ? dataMap.getInt(DATA_RETRIES_ALLOWED) : 0;
        int currentRetries = dataMap.containsKey(DATA_RETRIES_COUNT) ? dataMap.getInt(DATA_RETRIES_COUNT) : 0;

        StringBuilder output = new StringBuilder();

        try {
            log.info("Executing command: {}", command);

            ProcessBuilder processBuilder = new ProcessBuilder();
            boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
            if (isWindows) {
                processBuilder.command("cmd.exe", "/c", command);
            } else {
                processBuilder.command("sh", "-c", command);
            }

            Process process = processBuilder.start();

            // Read Output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // Read Error
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                output.append("ERROR: ").append(line).append("\n");
            }

            int exitCode = process.waitFor();
            context.setResult(output.toString()); // Pass output to Listener

            if (exitCode != 0) {
                throw new Exception("Process exited with code " + exitCode);
            }

        } catch (Exception e) {
            log.error("Job Execution Failed", e);
            context.setResult(output.toString() + "\nEXCEPTION: " + e.getMessage());

            if (currentRetries < retriesAllowed) {
                log.info("Retrying job... Attempt {} of {}", currentRetries + 1, retriesAllowed);
                dataMap.put(DATA_RETRIES_COUNT, currentRetries + 1);

                JobExecutionException je = new JobExecutionException(e);
                je.setRefireImmediately(true);
                throw je;
            }
            throw new JobExecutionException(e);
        }
    }
}