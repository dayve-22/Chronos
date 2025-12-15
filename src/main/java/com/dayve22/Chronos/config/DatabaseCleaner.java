//package com.dayve22.Chronos.config;
//
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//import javax.sql.DataSource;
//import java.sql.Connection;
//import java.sql.Statement;
//
//@Component
//public class DatabaseCleaner implements CommandLineRunner {
//
//    private final DataSource dataSource;
//
//    public DatabaseCleaner(DataSource dataSource) {
//        this.dataSource = dataSource;
//    }
//
//    @Override
//    public void run(String... args) throws Exception {
//        System.out.println("==========================================");
//        System.out.println("⚠️  STARTING QUARTZ DATABASE CLEANUP  ⚠️");
//
//        try (Connection conn = dataSource.getConnection();
//             Statement stmt = conn.createStatement()) {
//
//            // This SQL wipes all Quartz job data but keeps your Users
//            stmt.execute("TRUNCATE TABLE qrtz_fired_triggers, qrtz_simple_triggers, qrtz_simprop_triggers, " +
//                    "qrtz_cron_triggers, qrtz_blob_triggers, qrtz_triggers, " +
//                    "qrtz_job_details, qrtz_calendars, qrtz_paused_trigger_grps, " +
//                    "qrtz_locks, qrtz_scheduler_state RESTART IDENTITY CASCADE");
//
//            System.out.println("✅  SUCCESS: Quartz tables have been wiped clean.");
//        } catch (Exception e) {
//            System.err.println("❌  FAILED to clean database: " + e.getMessage());
//        }
//
//        System.out.println("==========================================");
//    }
//}