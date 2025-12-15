package com.dayve22.Chronos.jobs;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Component
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class CommandExecutionJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(CommandExecutionJob.class);

    public static final String DATA_COMMAND = "command";
    public static final String DATA_RETRIES_ALLOWED = "retriesAllowed";
    public static final String DATA_RETRIES_COUNT = "retriesCount";

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        JobDataMap dataMap = context.getJobDetail().getJobDataMap();

        String command = dataMap.getString(DATA_COMMAND);
        int retriesAllowed = dataMap.getIntValue(DATA_RETRIES_ALLOWED);
        int currentRetries = dataMap.containsKey(DATA_RETRIES_COUNT)
                ? dataMap.getIntValue(DATA_RETRIES_COUNT)
                : 0;

        StringBuilder output = new StringBuilder();

        try {
            log.info("Executing command: {}", command);

            ProcessBuilder processBuilder = new ProcessBuilder();
            boolean isWindows = System.getProperty("os.name")
                    .toLowerCase()
                    .startsWith("windows");

            if (isWindows) {
                processBuilder.command("cmd.exe", "/c", command);
            } else {
                processBuilder.command("sh", "-c", command);
            }

            Process process = processBuilder.start();

            // -------- STDOUT --------
            try (BufferedReader reader =
                         new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // -------- STDERR --------
            try (BufferedReader errorReader =
                         new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    output.append("ERROR: ").append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            context.setResult(output.toString());

            if (exitCode != 0) {
                throw new RuntimeException("Process exited with code " + exitCode);
            }

            log.info("Command executed successfully");

        } catch (Exception e) {

            log.error("Job execution failed (attempt {} of {})",
                    currentRetries + 1, retriesAllowed, e);

            context.setResult(output + "\nEXCEPTION: " + e.getMessage());

            if (currentRetries < retriesAllowed) {
                dataMap.put(DATA_RETRIES_COUNT, currentRetries + 1);
                // Let Quartz refire based on trigger schedule (safe for JDBC)
                throw new JobExecutionException(e, false);
            }

            throw new JobExecutionException(e);
        }
    }
}