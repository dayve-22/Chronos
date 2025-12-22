# ⏳ Chronos - High-Scale Distributed Job Scheduler

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-green.svg)](https://spring.io/projects/spring-boot)
[![Redis](https://img.shields.io/badge/Redis-7.0-red.svg)](https://redis.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg)](https://www.docker.com/)

**Chronos** is a production-ready, distributed job scheduling system designed to handle thousands of concurrent tasks with millisecond precision. It utilizes a **Hybrid Architecture** combining the transactional reliability of PostgreSQL with the high-speed scheduling capabilities of Redis Sorted Sets (ZSETs).

---

## 🏗️ System Architecture

![Chronos Architecture Diagram](https://github.com/dayve-22/Chronos/blob/final-submission/chronos_architecture.png)

### How it Works
1.  **Submission:** Users submit jobs via REST API (secured by JWT). Jobs are saved to **PostgreSQL** for persistence.
2.  **Scheduling:** The system calculates the `NextRunTime` and pushes the Job ID to a **Redis Sorted Set** (`score = timestamp`).
3.  **Polling:** A lightweight poller runs on every instance, querying Redis every second.
4.  **Atomic Fetch:** A custom **Lua Script** atomically checks for due jobs, pops them from Redis, and returns them to the worker. This ensures **exactly-once execution** across distributed nodes.
5.  **Execution:** Jobs are executed in isolated thread pools. The system automatically detects the OS (Windows/Linux) to run system commands correctly.

---

## 🚀 Key Features

* **⚡ High Throughput:** Capable of scheduling and executing 1000+ jobs/second using Redis.
* **🔄 Flexible Scheduling:** Support for Cron expressions (`0 0 * * *`) and Simple Intervals (`every 5 seconds`).
* **🛡️ Distributed & Fault Tolerant:**
    * Stateless architecture allows horizontal scaling (just add more containers).
    * **Atomic Locking:** Lua scripts prevent race conditions between nodes.
    * **Auto-Recovery:** Failed jobs are automatically retried with **Exponential Backoff**.
* **💻 OS Agnostic:** Smart executors automatically wrap commands for **Windows** (`cmd.exe /c`) or **Linux** (`sh -c`).
* **🔒 Secure:** Fully integrated Role-Based Access Control (RBAC) using JWT.
* **📊 Observable:** Built-in Prometheus metrics and Actuator endpoints for real-time monitoring.

---

## 🛠️ Technology Stack

* **Core:** Java 21, Spring Boot 4.0 (Spring Data JPA, Spring Security, Spring Web)
* **Database:** PostgreSQL 15 (Jobs, Users, Execution Logs)
* **Cache/Queue:** Redis & Redisson (Scheduling Engine, Distributed Locks)
* **DevOps:** Docker, Docker Compose
* **Documentation:** SpringDoc (Swagger UI/OpenAPI)

---

## 🏁 Getting Started

### Prerequisites
* Docker & Docker Compose installed on your machine.
* (Optional) Java 21 SDK and Maven if running locally without Docker.

### 1. Clone the Repository
```bash
git clone [https://github.com/yourusername/chronos.git](https://github.com/yourusername/chronos.git)
cd chronos 
```
### 2. Run with Docker Compose
* This command builds the application JAR, creates the PostgreSQL and Redis containers, and links them all together.
```bash
docker-compose up --build -d
```
### 3. Verify Installation
* **Application Logs:** docker-compose logs -f app
* **Swagger UI:** Open http://localhost:8080/swagger-ui/index.html
* **Prometheus Metrics:** http://localhost:8080/actuator/prometheus
  
## 📖 API Documentation

### The system exposes a comprehensive REST API. Full documentation is available via Swagger UI once the application is running.

**Example: Create a Recurring Job**
**POST /api/jobs**

```{
  "name": "System Health Check",
  "type": "COMMAND",
  "scheduleType": "RECURRING",
  "intervalSeconds": 10,
  "commandData": {
    "command": "echo Health OK >> /tmp/health.log",
    "timeoutSeconds": 30
  }
}
```
## 💡 Design Decisions
**1. Why Redis over Database Polling?**
Traditional polling (SELECT * FROM jobs WHERE next_run < NOW()) creates database bottlenecks and locking contention at scale.
* **Decision:** We use Redis Sorted Sets. Fetching due jobs is an $O(\log N)$ operation in memory, allowing for microsecond latency and massive scalability.
**2. Why PostgreSQL?**
While Redis is fast, it is volatile.
* **Decision:** PostgreSQL acts as the Source of Truth. It provides ACID compliance, ensuring that job definitions and execution history are never lost, even if the cache is flushed.
**3. Why Lua Scripts?**
In a distributed system, "Check then Act" logic (check if job is due, then run it) causes race conditions (double execution).
* **Decision:** A Lua script runs inside Redis to Find, Pop, and Return the job in a single atomic step. This guarantees that if 10 servers poll at the exact same millisecond, only one will receive the job.


