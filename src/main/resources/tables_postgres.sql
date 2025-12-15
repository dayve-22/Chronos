-- PART A: NUKE EVERYTHING (Force Drop)

-- tables_postgres.sql

DROP TABLE IF EXISTS QRTZ_FIRED_TRIGGERS CASCADE;
DROP TABLE IF EXISTS QRTZ_PAUSED_TRIGGER_GRPS CASCADE;
DROP TABLE IF EXISTS QRTZ_SCHEDULER_STATE CASCADE;
DROP TABLE IF EXISTS QRTZ_LOCKS CASCADE;
DROP TABLE IF EXISTS QRTZ_SIMPROP_TRIGGERS CASCADE;
DROP TABLE IF EXISTS QRTZ_CRON_TRIGGERS CASCADE;
DROP TABLE IF EXISTS QRTZ_BLOB_TRIGGERS CASCADE;
DROP TABLE IF EXISTS QRTZ_TRIGGERS CASCADE;
DROP TABLE IF EXISTS QRTZ_JOB_DETAILS CASCADE;
DROP TABLE IF EXISTS QRTZ_CALENDARS CASCADE;

-- PART B: RECREATE CLEAN TABLES
CREATE TABLE QRTZ_JOB_DETAILS (
                                  SCHED_NAME VARCHAR(120) NOT NULL,
                                  JOB_NAME VARCHAR(200) NOT NULL,
                                  JOB_GROUP VARCHAR(200) NOT NULL,
                                  DESCRIPTION VARCHAR(250) NULL,
                                  JOB_CLASS_NAME VARCHAR(250) NOT NULL,
                                  IS_DURABLE BOOLEAN NOT NULL,
                                  IS_NONCONCURRENT BOOLEAN NOT NULL,
                                  IS_UPDATE_DATA BOOLEAN NOT NULL,
                                  REQUESTS_RECOVERY BOOLEAN NOT NULL,
                                  JOB_DATA BYTEA NULL,
                                  PRIMARY KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
);

CREATE TABLE QRTZ_TRIGGERS (
                               SCHED_NAME VARCHAR(120) NOT NULL,
                               TRIGGER_NAME VARCHAR(200) NOT NULL,
                               TRIGGER_GROUP VARCHAR(200) NOT NULL,
                               JOB_NAME VARCHAR(200) NOT NULL,
                               JOB_GROUP VARCHAR(200) NOT NULL,
                               DESCRIPTION VARCHAR(250) NULL,
                               NEXT_FIRE_TIME BIGINT NULL,
                               PREV_FIRE_TIME BIGINT NULL,
                               PRIORITY INTEGER NULL,
                               TRIGGER_STATE VARCHAR(16) NOT NULL,
                               TRIGGER_TYPE VARCHAR(8) NOT NULL,
                               START_TIME BIGINT NOT NULL,
                               END_TIME BIGINT NULL,
                               CALENDAR_NAME VARCHAR(200) NULL,
                               MISFIRE_INSTR SMALLINT NULL,
                               JOB_DATA BYTEA NULL,
                               PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
                               FOREIGN KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
                                   REFERENCES QRTZ_JOB_DETAILS(SCHED_NAME, JOB_NAME, JOB_GROUP)
);

CREATE TABLE QRTZ_SIMPLE_TRIGGERS (
                                      SCHED_NAME VARCHAR(120) NOT NULL,
                                      TRIGGER_NAME VARCHAR(200) NOT NULL,
                                      TRIGGER_GROUP VARCHAR(200) NOT NULL,
                                      REPEAT_COUNT BIGINT NOT NULL,
                                      REPEAT_INTERVAL BIGINT NOT NULL,
                                      TIMES_TRIGGERED BIGINT NOT NULL,
                                      PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
                                      FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
                                          REFERENCES QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE QRTZ_CRON_TRIGGERS (
                                    SCHED_NAME VARCHAR(120) NOT NULL,
                                    TRIGGER_NAME VARCHAR(200) NOT NULL,
                                    TRIGGER_GROUP VARCHAR(200) NOT NULL,
                                    CRON_EXPRESSION VARCHAR(120) NOT NULL,
                                    TIME_ZONE_ID VARCHAR(80),
                                    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
                                    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
                                        REFERENCES QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE QRTZ_SIMPROP_TRIGGERS (
                                       SCHED_NAME VARCHAR(120) NOT NULL,
                                       TRIGGER_NAME VARCHAR(200) NOT NULL,
                                       TRIGGER_GROUP VARCHAR(200) NOT NULL,
                                       STR_PROP_1 VARCHAR(512) NULL,
                                       STR_PROP_2 VARCHAR(512) NULL,
                                       STR_PROP_3 VARCHAR(512) NULL,
                                       INT_PROP_1 INTEGER NULL,
                                       INT_PROP_2 INTEGER NULL,
                                       LONG_PROP_1 BIGINT NULL,
                                       LONG_PROP_2 BIGINT NULL,
                                       DEC_PROP_1 NUMERIC(13,4) NULL,
                                       DEC_PROP_2 NUMERIC(13,4) NULL,
                                       BOOL_PROP_1 BOOLEAN NULL,
                                       BOOL_PROP_2 BOOLEAN NULL,
                                       PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
                                       FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
                                           REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE QRTZ_BLOB_TRIGGERS (
                                    SCHED_NAME VARCHAR(120) NOT NULL,
                                    TRIGGER_NAME VARCHAR(200) NOT NULL,
                                    TRIGGER_GROUP VARCHAR(200) NOT NULL,
                                    BLOB_DATA BYTEA NULL,
                                    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
                                    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
                                        REFERENCES QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE QRTZ_CALENDARS (
                                SCHED_NAME VARCHAR(120) NOT NULL,
                                CALENDAR_NAME VARCHAR(200) NOT NULL,
                                CALENDAR BLOB NOT NULL,
                                PRIMARY KEY (SCHED_NAME, CALENDAR_NAME)
);

CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS (
                                          SCHED_NAME VARCHAR(120) NOT NULL,
                                          TRIGGER_GROUP VARCHAR(200) NOT NULL,
                                          PRIMARY KEY (SCHED_NAME, TRIGGER_GROUP)
);

CREATE TABLE QRTZ_FIRED_TRIGGERS (
                                     SCHED_NAME VARCHAR(120) NOT NULL,
                                     ENTRY_ID VARCHAR(95) NOT NULL,
                                     TRIGGER_NAME VARCHAR(200) NOT NULL,
                                     TRIGGER_GROUP VARCHAR(200) NOT NULL,
                                     INSTANCE_NAME VARCHAR(200) NOT NULL,
                                     FIRED_TIME BIGINT NOT NULL,
                                     SCHED_TIME BIGINT NOT NULL,
                                     PRIORITY INTEGER NOT NULL,
                                     STATE VARCHAR(16) NOT NULL,
                                     JOB_NAME VARCHAR(200) NULL,
                                     JOB_GROUP VARCHAR(200) NULL,
                                     IS_NONCONCURRENT BOOLEAN NULL,
                                     REQUESTS_RECOVERY BOOLEAN NULL,
                                     PRIMARY KEY (SCHED_NAME, ENTRY_ID)
);

CREATE TABLE QRTZ_SCHEDULER_STATE (
                                      SCHED_NAME VARCHAR(120) NOT NULL,
                                      INSTANCE_NAME VARCHAR(200) NOT NULL,
                                      LAST_CHECKIN_TIME BIGINT NOT NULL,
                                      CHECKIN_INTERVAL BIGINT NOT NULL,
                                      PRIMARY KEY (SCHED_NAME, INSTANCE_NAME)
);

CREATE TABLE QRTZ_LOCKS (
                            SCHED_NAME VARCHAR(120) NOT NULL,
                            LOCK_NAME VARCHAR(40) NOT NULL,
                            PRIMARY KEY (SCHED_NAME, LOCK_NAME)
);

INSERT INTO QRTZ_LOCKS values('quartzScheduler', 'TRIGGER_ACCESS');
INSERT INTO QRTZ_LOCKS values('quartzScheduler', 'JOB_ACCESS');
INSERT INTO QRTZ_LOCKS values('quartzScheduler', 'CALENDAR_ACCESS');
INSERT INTO QRTZ_LOCKS values('quartzScheduler', 'STATE_ACCESS');
INSERT INTO QRTZ_LOCKS values('quartzScheduler', 'MISFIRE_ACCESS');