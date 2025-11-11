# 🌱 InvernaderosAPI / API de Invernaderos

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-blue.svg)](https://kotlinlang.org/)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-Private-red.svg)](LICENSE)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)
[![TimescaleDB](https://img.shields.io/badge/TimescaleDB-Latest-yellow.svg)](https://www.timescale.com/)
[![MQTT](https://img.shields.io/badge/MQTT-EMQX-purple.svg)](https://www.emqx.io/)

**[Language: [English](#english) | [Español](#español)]**

---

## English

### 📑 Table of Contents

- [What is this?](#-what-is-this)
- [Features](#-features)
- [How it Works](#%EF%B8%8F-how-it-works)
- [Quick Start](#-quick-start)
- [Architecture](#-architecture)
- [API Reference](#-api-reference)
- [Database Structure](#-database-structure)
- [Configuration](#-configuration)
- [Deployment](#-deployment)
- [Testing](#-testing)
- [WebSocket & Real-Time Updates](#-websocket--real-time-updates)
- [MQTT Integration](#-mqtt-integration)
- [Troubleshooting](#-troubleshooting)
- [Security Best Practices](#-security-best-practices)
- [Contributing](#-contributing)
- [FAQ](#-faq)
- [Roadmap](#%EF%B8%8F-roadmap)
- [License & Credits](#-license--credits)
- [Support](#-support)

---

### 🎯 What is this?

**InvernaderosAPI** is a smart greenhouse monitoring system that helps you keep an eye on your plants' health in real-time.

**Think of it like this:** Imagine you have a greenhouse full of delicate plants, and you want to know the temperature, humidity, and soil moisture at all times—even when you're not there. This API is like having a tireless assistant who constantly checks all the sensors, writes down every reading in a organized notebook, and immediately alerts you when something interesting happens.

**In technical terms:** It's a RESTful API built with Spring Boot and Kotlin that receives sensor data via MQTT, stores it in a time-series database (TimescaleDB), caches recent data in Redis for fast access, and broadcasts real-time updates via WebSocket to connected clients.

**Real-world use case:** A farmer with multiple greenhouses can monitor temperature, humidity, and other environmental factors from their phone or computer, receive alerts when values go outside safe ranges, and analyze historical trends to optimize growing conditions.

---

### ✨ Features


#### 🎨 What This System Can Do

- **📊 Real-Time Monitoring** - Like having a security camera for your plants, but instead of video, you get live sensor readings
- **💾 Historical Data Storage** - Think of it as a diary that remembers every temperature reading, every humidity spike, forever
- **⚡ Lightning-Fast Access** - Recent data is cached like keeping your most-used tools within arm's reach instead of in the garage
- **🔔 Live Notifications** - Get instant alerts via WebSocket—like getting a text message the moment something changes
- **📈 Statistics & Analytics** - Easily see patterns: "What was the average temperature last week?" or "When was humidity highest?"
- **🌐 RESTful API** - Access your data from anywhere: web apps, mobile apps, or even Excel spreadsheets
- **🔐 Secure Configuration** - All passwords and sensitive data are kept in environment variables, never hardcoded
- **🐳 Docker Ready** - Deploy anywhere with one command—like a food truck that can set up shop anywhere
- **📡 MQTT Protocol** - Industry-standard IoT communication, like having all your devices speak the same language
- **🔄 Automatic Scaling** - TimescaleDB automatically manages large amounts of time-series data efficiently

#### 🛠️ Technical Features

- ✅ **REST API** with Spring Boot 3.5.7 + Kotlin
- ✅ **Time-Series Database** with TimescaleDB (PostgreSQL extension)
- ✅ **Metadata Database** with PostgreSQL
- ✅ **Distributed Cache** with Redis
- ✅ **MQTT Communication** with EMQX Broker
- ✅ **WebSocket** for real-time client updates
- ✅ **OpenAPI/Swagger** documentation
- ✅ **Docker** containerization
- ✅ **Health Checks** and monitoring endpoints
- ✅ **JPA/Hibernate** for data persistence
- ✅ **Spring Integration** for message routing
- ✅ **Actuator** for production monitoring

---

### 🏗️ How it Works

Let's explain the architecture using a restaurant analogy:


```
┌─────────────────┐
│  IoT Sensors    │ ← The "customers" sending their orders (sensor data)
│  (Greenhouse)   │
└────────┬────────┘
         │
         ↓ MQTT Protocol (like a waiter taking orders)
┌─────────────────┐
│  EMQX Broker    │ ← The "order window" where all requests arrive
└────────┬────────┘
         │
         ↓ Spring Integration (the kitchen manager distributing tasks)
┌─────────────────┐
│  Spring Boot    │ ← The "kitchen" processing everything
│      API        │
└────────┬────────┘
         │
         ├────→ Redis Cache ← "Fast-access fridge" for recent orders
         │
         ├────→ TimescaleDB ← "Recipe archive" storing all historical data
         │
         ├────→ PostgreSQL ← "Inventory system" with greenhouse metadata
         │
         └────→ WebSocket ← "Intercom" announcing orders to dining room staff (clients)
```

**How the data flows:**

1. **Sensors Send Data (The Customer Orders)**: Your greenhouse sensors measure temperature, humidity, etc., and send this data via MQTT—like customers placing orders
   
2. **MQTT Broker Receives (The Order Window)**: EMQX Broker receives all messages on the "GREENHOUSE" topic—like an order window collecting all requests

3. **Spring Boot Processes (The Kitchen)**: Our API listens to the broker and processes each message:
   - Validates the data (makes sure the order is correct)
   - Stores the last 1000 messages in Redis (keeps recent orders on the counter for quick access)
   - Saves ALL data permanently in TimescaleDB (writes it in the permanent record book)
   - Broadcasts updates via WebSocket (announces the new order to everyone waiting)

4. **Clients Request Data (Dining Room Staff)**: Your applications can:
   - Ask for recent data from Redis (quick access to what's on the counter)
   - Query historical data from TimescaleDB (look up old records from the archive)
   - Subscribe to WebSocket for real-time updates (listen to the intercom)

**Why this architecture?**

- **MQTT** → Efficient for IoT devices (low bandwidth, reliable delivery)
- **Redis** → Fast access to recent data (milliseconds response time)
- **TimescaleDB** → Optimized for time-series data (queries like "average temperature last month" are lightning-fast)
- **WebSocket** → Real-time push notifications (no need to constantly ask "is there new data?")
- **PostgreSQL** → Reliable storage for greenhouses, tenants, and configuration

---

### 🚀 Quick Start

Get the system running in less than 5 minutes!

#### Prerequisites

- **Docker** and **Docker Compose** (v20.10+ and v2.0+)
- **Git**
- That's it! (Java 21 is optional, only needed for local development without Docker)

#### Step 1: Clone the Repository

```bash
git clone https://github.com/apptolast/InvernaderosAPI.git
cd InvernaderosAPI
```

#### Step 2: Configure Environment Variables

```bash
# Copy the example environment file
cp .env.example .env

# Generate secure passwords
openssl rand -base64 32
```

Now edit the `.env` file and replace all `<your_*>` placeholders with actual values:

```env
TIMESCALE_PASSWORD=your_generated_password_here
METADATA_PASSWORD=another_secure_password_here
REDIS_PASSWORD=redis_password_here
MQTT_USERNAME=your_mqtt_username
MQTT_PASSWORD=mqtt_secure_password
```

⚠️ **IMPORTANT**: Never use default passwords in production! Generate unique, strong passwords for each service.

#### Step 3: Start All Services

```bash
# Start everything with Docker Compose
docker-compose up -d

# Check that services are running
docker-compose ps
```

#### Step 4: Verify It Works

```bash
# Health check
curl http://localhost:8080/actuator/health

# Expected response: {"status":"UP"}
```

#### Step 5: Explore the API

Open your browser and visit:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Documentation**: http://localhost:8080/v3/api-docs

🎉 **Congratulations!** Your greenhouse monitoring system is now running!

#### Next Steps

- Check out [API Reference](#-api-reference) to start querying data
- Review [WebSocket documentation](#-websocket--real-time-updates) to receive real-time updates
- See [MQTT Integration](#-mqtt-integration) to connect your sensors

---

### 🏛️ Architecture


#### System Components

```
┌──────────────────────────────────────────────────────────────┐
│                     CLIENT APPLICATIONS                       │
│  (Web Dashboard, Mobile App, Analytics Tools)                │
└───────┬──────────────────────────────────────────┬───────────┘
        │ HTTP/REST                    WebSocket/STOMP
        ↓                                          ↓
┌─────────────────────────────────────────────────────────────┐
│              SPRING BOOT API (Port 8080)                     │
│  ┌──────────┐  ┌──────────┐  ┌───────────┐  ┌────────────┐ │
│  │Controllers│  │ Services │  │Repositories│  │  WebSocket │ │
│  └──────────┘  └──────────┘  └───────────┘  └────────────┘ │
└───┬────────┬──────────┬──────────┬────────────────┬─────────┘
    │        │          │          │                │
    │        │          │          │                │
    ↓        ↓          ↓          ↓                ↓
┌──────┐ ┌──────┐ ┌────────┐ ┌──────────┐    ┌──────────┐
│EMQX  │ │Redis │ │Timesca │ │PostgreSQL│    │  MQTT    │
│Broker│ │Cache │ │ leDB   │ │ Metadata │    │Publishers│
│:1883 │ │:6379 │ │ :5432  │ │  :5433   │    └────┬─────┘
└──┬───┘ └──────┘ └────────┘ └──────────┘         │
   │                                               │
   ↑ MQTT Subscribe                    MQTT Publish ↑
   │                                               │
┌──┴───────────────────────────────────────────────┴────┐
│          IoT SENSORS (Greenhouse Devices)             │
│  (Temperature, Humidity, Soil Moisture, etc.)         │
└───────────────────────────────────────────────────────┘
```

#### Technology Stack Explained

| Component | Technology | Purpose | Analogy |
|-----------|-----------|---------|---------|
| **API Framework** | Spring Boot 3.5.7 | Handles all HTTP requests and business logic | The restaurant kitchen where orders are prepared |
| **Language** | Kotlin | Modern, concise programming language | The recipes and cooking techniques |
| **Message Broker** | EMQX | Distributes MQTT messages from sensors | The order window collecting all customer requests |
| **Fast Cache** | Redis | Stores last 1000 messages for quick access | The "mise en place" - ingredients prepped and ready |
| **Time-Series DB** | TimescaleDB | Optimized for sensor readings over time | The recipe book with every dish ever made recorded |
| **Metadata DB** | PostgreSQL | Stores greenhouse info, users, config | The employee handbook and inventory lists |
| **Real-Time** | WebSocket | Pushes live updates to clients | The kitchen bell that rings when orders are ready |
| **Documentation** | Swagger/OpenAPI | Interactive API documentation | The restaurant menu with descriptions |
| **Containerization** | Docker | Packages everything for easy deployment | The food truck that can go anywhere |

---

### 💻 API Reference

#### Base URL
```
http://localhost:8080/api
```

#### Core Endpoints

##### 1. Get Recent Messages

Retrieve the last N messages received from sensors.

```http
GET /api/greenhouse/messages/recent?limit=100
```

**Parameters:**
- `limit` (optional): Number of messages to retrieve (1-1000, default: 100)

**Example Request:**
```bash
curl http://localhost:8080/api/greenhouse/messages/recent?limit=10
```

**Example Response:**
```json
[
  {
    "timestamp": "2025-11-09T18:16:24Z",
    "sensor01": 24.5,
    "sensor02": 65.3,
    "setpoint01": 25.0,
    "setpoint02": 60.0,
    "setpoint03": 70.0,
    "greenhouseId": "001",
    "rawPayload": "{\"SENSOR_01\":24.5,...}"
  }
]
```

**Non-Technical Explanation:** Like asking "Show me the last 10 temperature readings" - you get a list of the most recent sensor data.

---

##### 2. Get Messages by Time Range

Query messages within a specific time period.

```http
GET /api/greenhouse/messages/range?from={start}&to={end}
```

**Parameters:**
- `from`: Start timestamp (ISO-8601 format)
- `to`: End timestamp (ISO-8601 format)

**Example Request:**
```bash
curl "http://localhost:8080/api/greenhouse/messages/range?from=2025-01-01T00:00:00Z&to=2025-01-02T00:00:00Z"
```

**Non-Technical Explanation:** Like asking "What were the temperatures yesterday between 9 AM and 5 PM?"

---

##### 3. Get Latest Message

Retrieve the most recent sensor reading.

```http
GET /api/greenhouse/messages/latest
```

**Example Request:**
```bash
curl http://localhost:8080/api/greenhouse/messages/latest
```

**Example Response:**
```json
{
  "timestamp": "2025-11-11T19:25:30Z",
  "sensor01": 23.8,
  "sensor02": 68.2,
  "setpoint01": 25.0,
  "greenhouseId": "001"
}
```

---

##### 4. Get Sensor Statistics

Calculate statistics for a specific sensor over a time period.

```http
GET /api/greenhouse/statistics/{sensorId}?period={period}
```

**Parameters:**
- `sensorId`: Sensor identifier (e.g., SENSOR_01, SETPOINT_01)
- `period`: Time period (1h, 24h, 7d, 30d)

**Example Request:**
```bash
curl http://localhost:8080/api/greenhouse/statistics/SENSOR_01?period=24h
```

**Example Response:**
```json
{
  "sensorId": "SENSOR_01",
  "period": "24h",
  "minValue": 18.5,
  "maxValue": 28.3,
  "avgValue": 23.7,
  "count": 1440,
  "lastValue": 24.1
}
```

**Non-Technical Explanation:** Like asking "What was the lowest, highest, and average temperature today?"

---

##### 5. Get Summary Statistics

Get a complete summary of all sensors and setpoints.

```http
GET /api/greenhouse/statistics/summary?period={period}
```

**Example Request:**
```bash
curl http://localhost:8080/api/greenhouse/statistics/summary?period=1h
```

**Non-Technical Explanation:** Like getting a dashboard showing all your sensors at a glance with their min/max/average values.

---

##### 6. Cache Information

Check the status of the Redis cache.

```http
GET /api/greenhouse/cache/info
```

**Example Response:**
```json
{
  "totalMessages": 1000,
  "cacheSize": "245KB",
  "oldestTimestamp": "2025-11-11T18:00:00Z",
  "newestTimestamp": "2025-11-11T19:25:30Z"
}
```

---

##### 7. Health Check

Verify the API is running and healthy.

```http
GET /actuator/health
```

**Example Response:**
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "redis": {"status": "UP"},
    "mqtt": {"status": "UP"}
  }
}
```

---

#### API Usage Examples in Different Languages

**JavaScript (Fetch API):**
```javascript
// Get recent messages
fetch('http://localhost:8080/api/greenhouse/messages/recent?limit=50')
  .then(response => response.json())
  .then(data => console.log(data))
  .catch(error => console.error('Error:', error));
```

**Python (requests):**
```python
import requests

# Get sensor statistics
response = requests.get(
    'http://localhost:8080/api/greenhouse/statistics/SENSOR_01',
    params={'period': '24h'}
)
data = response.json()
print(f"Average: {data['avgValue']}")
```

**PHP (cURL):**
```php
<?php
$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, "http://localhost:8080/api/greenhouse/messages/latest");
curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
$output = curl_exec($ch);
$data = json_decode($output, true);
curl_close($ch);
echo "Latest temperature: " . $data['sensor01'];
?>
```

**curl (Command Line):**
```bash
# Get messages from last week
curl "http://localhost:8080/api/greenhouse/messages/range?from=$(date -u -d '7 days ago' +%Y-%m-%dT%H:%M:%SZ)&to=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
```

---

### 🗄️ Database Structure


Our system uses two specialized databases, like having separate filing systems for different types of information:

#### TimescaleDB (Time-Series Database) - Port 5432

**What it stores:** All sensor readings over time

**Think of it like:** A weather station's logbook that records temperature every minute for years. You can quickly answer questions like "What was the temperature trend last month?" because the data is organized chronologically.

**Tables:**
- `sensor_readings` - Every sensor measurement with a timestamp
- Hypertables automatically partition data by time for efficiency

**Example Data:**
```sql
timestamp            | sensor_id  | value | greenhouse_id
---------------------|------------|-------|-------------
2025-11-11 19:00:00 | SENSOR_01  | 24.5  | 001
2025-11-11 19:01:00 | SENSOR_01  | 24.7  | 001
2025-11-11 19:02:00 | SENSOR_01  | 24.6  | 001
```

**Why TimescaleDB?**
- **Fast queries** on time ranges ("show me last week")
- **Automatic data retention** policies (keep 1 year, delete older)
- **Compression** for old data (saves storage space)
- **Continuous aggregates** (pre-calculated hourly/daily averages)

---

#### PostgreSQL (Metadata Database) - Port 5433

**What it stores:** Configuration and reference data

**Think of it like:** The company directory that lists all employees, departments, and policies. This data doesn't change often, but you need it to understand who's who and what's what.

**Tables:**
- `greenhouses` - Information about each greenhouse (name, location, owner)
- `tenants` - Organizations or users who own greenhouses
- `users` - User accounts and permissions
- `mqtt_users` - MQTT client credentials and access control
- `sensors` - Sensor definitions and metadata

**Example Schema:**
```sql
-- Greenhouses
CREATE TABLE greenhouses (
  id VARCHAR PRIMARY KEY,
  name VARCHAR NOT NULL,
  location VARCHAR,
  tenant_id BIGINT REFERENCES tenants(id)
);

-- Sensors
CREATE TABLE sensors (
  id VARCHAR PRIMARY KEY,
  type VARCHAR,
  unit VARCHAR,
  greenhouse_id VARCHAR REFERENCES greenhouses(id)
);
```

---

#### Redis (Cache) - Port 6379

**What it stores:** Last 1000 messages for lightning-fast access

**Think of it like:** Your web browser's cache that keeps recently visited websites ready for instant loading.

**Data Structure:**
- **Sorted Set**: Messages ranked by timestamp
- **Key**: `greenhouse:messages`
- **TTL**: Automatic cleanup of old data

**Why Redis?**
- Response time in **milliseconds** (vs. database queries that might take seconds)
- Perfect for "show me the latest readings" requests
- Reduces load on the main database
- Automatically maintains only recent data

---

### 🔧 Configuration

#### Environment Variables

All sensitive configuration is managed through environment variables. Never hardcode credentials!

**Required Variables (.env file):**

```bash
# TimescaleDB Configuration
TIMESCALE_DB_NAME=greenhouse_timeseries
TIMESCALE_USER=admin
TIMESCALE_PASSWORD=<generate_secure_password>

# PostgreSQL Metadata Database
METADATA_DB_NAME=greenhouse_metadata
METADATA_USER=admin
METADATA_PASSWORD=<generate_secure_password>

# Redis Cache
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=<generate_secure_password>

# MQTT Broker (EMQX)
MQTT_BROKER_URL=tcp://emqx:1883
MQTT_USERNAME=<your_mqtt_username>
MQTT_PASSWORD=<generate_secure_password>
MQTT_CLIENT_ID_PREFIX=api_local_001

# EMQX Dashboard
EMQX_DASHBOARD_USERNAME=admin
EMQX_DASHBOARD_PASSWORD=<generate_secure_password>

# Spring Boot
SPRING_PROFILES_ACTIVE=local
JAVA_OPTS=-Xms256m -Xmx512m
```

**To generate secure passwords:**
```bash
# Generate a random password
openssl rand -base64 32

# Or use pwgen
pwgen -s 32 1
```

#### Configuration Files

**1. Application Configuration (`application-local.yaml.example`)**

```yaml
spring:
  datasource:
    # TimescaleDB connection
    timescale:
      url: jdbc:postgresql://timescaledb:5432/${TIMESCALE_DB_NAME}
      username: ${TIMESCALE_USER}
      password: ${TIMESCALE_PASSWORD}
    # Metadata PostgreSQL connection
    metadata:
      url: jdbc:postgresql://postgresql-metadata:5432/${METADATA_DB_NAME}
      username: ${METADATA_USER}
      password: ${METADATA_PASSWORD}
  
  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}
    password: ${REDIS_PASSWORD}

mqtt:
  broker:
    url: ${MQTT_BROKER_URL}
    username: ${MQTT_USERNAME}
    password: ${MQTT_PASSWORD}
    client-id: ${MQTT_CLIENT_ID_PREFIX}
  topics:
    greenhouse: "GREENHOUSE"
```

**2. Docker Compose Override (optional customization):**

Copy `docker-compose.override.yaml.example` to `docker-compose.override.yaml` to customize service configurations locally.

---

### 🐳 Deployment

#### Local Development with Docker Compose

**1. Prerequisites:**
```bash
# Verify Docker is installed
docker --version  # Should be v20.10+
docker-compose --version  # Should be v2.0+
```

**2. Setup:**
```bash
# Clone and configure
git clone https://github.com/apptolast/InvernaderosAPI.git
cd InvernaderosAPI
cp .env.example .env
# Edit .env with your passwords
nano .env
```

**3. Start Services:**
```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f api

# Check service status
docker-compose ps
```

**4. Stop Services:**
```bash
# Stop all services
docker-compose down

# Stop and remove volumes (⚠️ deletes all data)
docker-compose down -v
```

---

#### Production Deployment

For production deployments, see the comprehensive [DEPLOYMENT.md](DEPLOYMENT.md) guide, which covers:

- Kubernetes deployment (development and production environments)
- CI/CD with GitHub Actions
- SSL/TLS configuration
- Backup and disaster recovery
- Monitoring and alerting
- Scaling strategies

**Quick Production Checklist:**

- [ ] Use strong, unique passwords for all services
- [ ] Enable SSL/TLS for all external connections
- [ ] Configure firewall rules (restrict database access)
- [ ] Set up automated backups
- [ ] Enable application monitoring
- [ ] Configure log aggregation
- [ ] Use secrets manager (AWS Secrets Manager, Vault, etc.)
- [ ] Set up alerting for critical errors
- [ ] Review security audit report: [SECURITY_AUDIT_REPORT.md](SECURITY_AUDIT_REPORT.md)

---

#### Docker Services

| Service | Container Name | Port(s) | Purpose |
|---------|---------------|---------|---------|
| API | invernaderos-api | 8080 | Main Spring Boot application |
| TimescaleDB | invernaderos-timescaledb | 5432 | Time-series sensor data |
| PostgreSQL | invernaderos-postgres-metadata | 5433 (mapped from 5432) | Metadata and configuration |
| Redis | invernaderos-redis | 6379 | Cache for recent messages |
| EMQX | invernaderos-emqx | 1883 (MQTT), 18083 (Dashboard) | MQTT message broker |

**Access Points:**
- **API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **EMQX Dashboard**: http://localhost:18083 (default login: admin/public)
- **Actuator**: http://localhost:8080/actuator

---

### 🧪 Testing

#### Running Tests

```bash
# Run all tests
./gradlew test

# Run tests with coverage report
./gradlew test jacocoTestReport

# View coverage report
open build/reports/jacoco/test/html/index.html
```

#### Test Structure

```
src/test/kotlin/
└── com/apptolast/invernaderos/
    ├── InvernaderosApplicationTests.kt  # Application context tests
    ├── controller/                       # API endpoint tests
    ├── service/                          # Business logic tests
    └── integration/                      # Integration tests
```

#### Manual Testing with curl

**Test Health Endpoint:**
```bash
curl http://localhost:8080/actuator/health
```

**Test API Endpoint:**
```bash
curl http://localhost:8080/api/greenhouse/messages/recent?limit=5
```

**Test WebSocket Connection:**
See the demo file: [greenhouse-client-demo.html](greenhouse-client-demo.html)

#### MQTT Testing

**Publish Test Message:**
```bash
# Using mosquitto_pub
mosquitto_pub -h localhost -p 1883 \
  -u your_mqtt_username -P your_mqtt_password \
  -t GREENHOUSE \
  -m '{"SENSOR_01":25.5,"SENSOR_02":65.0,"SETPOINT_01":25.0,"SETPOINT_02":60.0,"SETPOINT_03":70.0}'
```

**Subscribe to MQTT Topic:**
```bash
# Using mosquitto_sub
mosquitto_sub -h localhost -p 1883 \
  -u your_mqtt_username -P your_mqtt_password \
  -t GREENHOUSE
```

---

### 🔌 WebSocket & Real-Time Updates


Get real-time sensor updates pushed to your application instantly!

**Think of it like:** Instead of constantly calling the restaurant to ask "Is my order ready?", they call YOU when it's ready.

#### WebSocket Connection

**Endpoint:**
```
ws://localhost:8080/ws/greenhouse
```

**Protocol:** STOMP over WebSocket

#### JavaScript Example

```javascript
// Include SockJS and STOMP libraries
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/@stomp/stompjs@7/bundles/stomp.umd.min.js"></script>

<script>
// Create WebSocket connection
const socket = new SockJS('http://localhost:8080/ws/greenhouse');
const stompClient = Stomp.over(socket);

// Connect to the WebSocket
stompClient.connect({}, function(frame) {
  console.log('Connected: ' + frame);
  
  // Subscribe to greenhouse messages
  stompClient.subscribe('/topic/greenhouse/messages', function(message) {
    const data = JSON.parse(message.body);
    console.log('New sensor reading:', data);
    
    // Update your UI here
    updateDashboard(data);
  });
});

function updateDashboard(data) {
  document.getElementById('temperature').innerText = data.sensor01 + '°C';
  document.getElementById('humidity').innerText = data.sensor02 + '%';
}
</script>
```

#### Complete HTML Example

See [greenhouse-client-demo.html](greenhouse-client-demo.html) for a full working example with:
- Live sensor readings
- Auto-updating charts
- Connection status indicator
- Error handling

#### Topics Available

| Topic | Description | Message Type |
|-------|-------------|--------------|
| `/topic/greenhouse/messages` | Real-time sensor readings | GreenhouseMessageDto |
| `/topic/greenhouse/alerts` | System alerts and warnings | Alert |
| `/topic/greenhouse/status` | Sensor connectivity status | Status |

---

### 📡 MQTT Integration

#### MQTT Message Format

**Topic:** `GREENHOUSE`

**Payload (JSON):**
```json
{
  "SENSOR_01": 24.5,    // Temperature (°C)
  "SENSOR_02": 65.3,    // Humidity (%)
  "SETPOINT_01": 25.0,  // Target temperature
  "SETPOINT_02": 60.0,  // Target humidity
  "SETPOINT_03": 70.0   // Additional setpoint
}
```

**Quality of Service (QoS):** 0 (at most once)

**Non-Technical Explanation:** Your sensors send messages to the "GREENHOUSE" channel, like posting updates to a group chat. The API listens to this channel and processes every message.

#### Connecting Your Sensors

**Using Arduino/ESP32:**
```cpp
#include <PubSubClient.h>
#include <WiFi.h>

const char* mqtt_server = "your-mqtt-broker.com";
const int mqtt_port = 1883;
const char* mqtt_user = "your_username";
const char* mqtt_pass = "your_password";

WiFiClient espClient;
PubSubClient client(espClient);

void setup() {
  client.setServer(mqtt_server, mqtt_port);
}

void publishSensorData() {
  String payload = "{\"SENSOR_01\":";
  payload += String(temperature);
  payload += ",\"SENSOR_02\":";
  payload += String(humidity);
  payload += "}";
  
  client.publish("GREENHOUSE", payload.c_str());
}
```

**Using Python:**
```python
import paho.mqtt.client as mqtt
import json
import time

client = mqtt.Client()
client.username_pw_set("your_username", "your_password")
client.connect("localhost", 1883, 60)

# Publish sensor data
data = {
    "SENSOR_01": 24.5,
    "SENSOR_02": 65.3,
    "SETPOINT_01": 25.0,
    "SETPOINT_02": 60.0,
    "SETPOINT_03": 70.0
}

client.publish("GREENHOUSE", json.dumps(data))
```

**Using MQTT.js (Node.js):**
```javascript
const mqtt = require('mqtt');

const client = mqtt.connect('mqtt://localhost:1883', {
  username: 'your_username',
  password: 'your_password'
});

client.on('connect', () => {
  // Publish sensor data every 5 seconds
  setInterval(() => {
    const data = {
      SENSOR_01: Math.random() * 30,
      SENSOR_02: Math.random() * 100
    };
    client.publish('GREENHOUSE', JSON.stringify(data));
  }, 5000);
});
```

#### EMQX Dashboard

Access the EMQX management dashboard at: **http://localhost:18083**

**Default credentials:**
- Username: `admin`
- Password: Your configured `EMQX_DASHBOARD_PASSWORD`

**Dashboard features:**
- Monitor connected clients
- View message statistics
- Configure access control
- Set up authentication rules
- Analyze MQTT traffic

---

### 🔍 Troubleshooting

#### Common Issues and Solutions

**1. Docker containers won't start**

```bash
# Check if ports are already in use
lsof -i :8080
lsof -i :5432

# View container logs
docker-compose logs api
docker-compose logs timescaledb

# Restart services
docker-compose restart
```

**2. Database connection errors**

```bash
# Check environment variables are set
docker-compose config

# Verify database is healthy
docker-compose exec timescaledb pg_isready

# Reset database (⚠️ deletes all data)
docker-compose down -v
docker-compose up -d
```

**3. MQTT connection failures**

```bash
# Check EMQX is running
docker-compose logs emqx

# Test MQTT connection
mosquitto_pub -h localhost -p 1883 -u username -P password -t test -m "hello"

# Check credentials in .env file
cat .env | grep MQTT
```

**4. Redis connection issues**

```bash
# Test Redis connection
docker-compose exec redis redis-cli ping

# Check if password is required
docker-compose exec redis redis-cli -a your_password ping
```

**5. API returns 500 errors**

```bash
# Check API logs
docker-compose logs api --tail=100

# Verify all services are healthy
curl http://localhost:8080/actuator/health

# Check database connections
docker-compose exec api env | grep DATABASE
```

**6. WebSocket won't connect**

- Verify the API is running: `curl http://localhost:8080/actuator/health`
- Check browser console for CORS errors
- Ensure you're using the correct WebSocket URL
- Try the demo HTML file: [greenhouse-client-demo.html](greenhouse-client-demo.html)

**7. No data appearing**

- Check if MQTT messages are being published
- Verify MQTT credentials are correct
- Look for errors in API logs: `docker-compose logs api`
- Test the connection manually with `mosquitto_pub`

---

#### Debug Mode

Enable debug logging to see detailed information:

```yaml
# In application-local.yaml
logging:
  level:
    com.apptolast.invernaderos: DEBUG
    org.springframework.integration.mqtt: DEBUG
```

Then restart the API:
```bash
docker-compose restart api
docker-compose logs -f api
```

---

#### Getting Help

If you're stuck:

1. **Check logs** for error messages
2. **Review documentation**: [DEPLOYMENT.md](DEPLOYMENT.md), [SECURITY.md](SECURITY.md), [GREENHOUSE_MQTT_IMPLEMENTATION.md](GREENHOUSE_MQTT_IMPLEMENTATION.md)
3. **Search existing issues** on GitHub
4. **Create a new issue** with:
   - Description of the problem
   - Steps to reproduce
   - Relevant logs (remove sensitive info!)
   - Your environment (OS, Docker version, etc.)

---

### 🔒 Security Best Practices

#### Credential Management

**❌ NEVER DO THIS:**
```java
// DON'T hardcode passwords!
String password = "mysecretpassword123";
```

**✅ DO THIS:**
```java
// Use environment variables
String password = System.getenv("DATABASE_PASSWORD");
```

#### Security Checklist

- [ ] **Strong Passwords**: Use `openssl rand -base64 32` to generate passwords
- [ ] **Environment Variables**: Never commit `.env` file to Git
- [ ] **Different Passwords**: Each service should have a unique password
- [ ] **Rotate Credentials**: Change passwords regularly (every 90 days)
- [ ] **Restrict Access**: Use firewall rules to limit database access
- [ ] **Enable SSL/TLS**: Use HTTPS and secure WebSocket (WSS) in production
- [ ] **Update Dependencies**: Run `./gradlew dependencyUpdates` regularly
- [ ] **Security Scanning**: GitHub CodeQL automatically scans for vulnerabilities
- [ ] **Principle of Least Privilege**: Grant only necessary permissions
- [ ] **Audit Logs**: Enable and monitor access logs

#### Pre-Commit Security Check

Before committing code, run the security validation script:

```bash
./scripts/validate-security.sh
```

This checks for:
- Exposed credentials in code
- Sensitive files not in `.gitignore`
- Hardcoded passwords or tokens

#### Production Security

For production deployments:

1. **Use Secrets Manager**: AWS Secrets Manager, HashiCorp Vault, or similar
2. **Enable HTTPS**: Configure SSL certificates with Let's Encrypt
3. **Configure CORS**: Restrict allowed origins (don't use `*`)
4. **Enable Authentication**: Implement JWT or OAuth2
5. **Network Isolation**: Use VPCs and security groups
6. **Backup Encryption**: Encrypt database backups
7. **Monitor Access**: Set up alerts for suspicious activity

See [SECURITY.md](SECURITY.md) for complete security guidelines.

---

### 🤝 Contributing

We welcome contributions! Here's how you can help:

#### Getting Started

1. **Fork the repository**
2. **Clone your fork**
   ```bash
   git clone https://github.com/YOUR_USERNAME/InvernaderosAPI.git
   cd InvernaderosAPI
   ```
3. **Create a branch**
   ```bash
   git checkout -b feature/amazing-feature
   ```
4. **Set up environment**
   ```bash
   cp .env.example .env
   # Edit .env with your credentials
   ```

#### Making Changes

1. **Write code** following existing style
2. **Add tests** for new features
3. **Update documentation** if needed
4. **Run tests** to ensure nothing breaks
   ```bash
   ./gradlew test
   ```
5. **Run security validation**
   ```bash
   ./scripts/validate-security.sh
   ```

#### Code Style Guidelines

- **Kotlin**: Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- **Naming**: Use clear, descriptive names
- **Comments**: Explain WHY, not WHAT
- **Tests**: Write meaningful test names

**Example:**
```kotlin
// ✅ Good
fun calculateAverageTemperatureForLast24Hours(sensorId: String): Double

// ❌ Bad
fun calcAvg(s: String): Double
```

#### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```bash
feat: add endpoint for hourly sensor averages
fix: correct timezone handling in statistics
docs: update API examples in README
test: add integration tests for WebSocket
```

#### Pull Request Process

1. **Update documentation** if you changed APIs
2. **Ensure tests pass** (`./gradlew test`)
3. **Run security checks** (`./scripts/validate-security.sh`)
4. **Create Pull Request** with clear description
5. **Respond to review** comments

#### What We're Looking For

- 🐛 Bug fixes
- ✨ New features
- 📝 Documentation improvements
- 🧪 Additional tests
- 🎨 UI/UX enhancements
- 🔒 Security improvements
- ⚡ Performance optimizations

---

### ❓ FAQ

**Q: Can I use this for multiple greenhouses?**  
A: Yes! The system supports multiple greenhouses. Each sensor message can include a `greenhouseId` field to identify the source.

**Q: How much historical data can I store?**  
A: TimescaleDB can efficiently handle years of data. You can configure retention policies to automatically delete old data (e.g., keep 1 year, delete older).

**Q: What happens if the internet connection drops?**  
A: MQTT supports offline queuing. When the connection is restored, messages will be delivered. Configure QoS level based on your reliability needs.

**Q: Can I add custom sensors?**  
A: Absolutely! Just send additional fields in your MQTT payload (e.g., `"SENSOR_03": 123.4`) and they'll be stored automatically.

**Q: How do I scale for more sensors?**  
A: The system is designed to scale horizontally. You can add more API instances behind a load balancer, and TimescaleDB handles large data volumes efficiently.

**Q: Can I export data to Excel/CSV?**  
A: Yes, query the API and format the response:
```bash
curl "http://localhost:8080/api/greenhouse/messages/range?from=2025-01-01T00:00:00Z&to=2025-01-02T00:00:00Z" | jq -r '.[] | [.timestamp, .sensor01, .sensor02] | @csv' > data.csv
```

**Q: What's the maximum message rate?**  
A: The system can handle thousands of messages per second. MQTT is very lightweight, and Redis caching ensures fast processing.

**Q: Do I need to know Kotlin to use this?**  
A: Not at all! You can use the API via HTTP requests from any programming language. Kotlin is only needed if you want to modify the API itself.

**Q: Can I run this on Raspberry Pi?**  
A: Yes, with some adjustments. Use lighter Docker images and reduce memory allocation in `JAVA_OPTS`.

**Q: How do I backup my data?**  
A: See [DEPLOYMENT.md](DEPLOYMENT.md) for backup strategies. You can use `pg_dump` for databases and volume snapshots for Docker.

---

### 🗺️ Roadmap

#### Upcoming Features

- [ ] **Authentication & Authorization** - JWT-based user authentication
- [ ] **Mobile App** - React Native app for iOS/Android
- [ ] **Alerts & Notifications** - Email/SMS alerts when values exceed thresholds
- [ ] **Machine Learning** - Predictive analytics for optimal growing conditions
- [ ] **Dashboard UI** - Web-based dashboard with charts and graphs
- [ ] **Multi-tenancy** - Support for multiple organizations
- [ ] **Data Export** - Scheduled CSV/Excel exports
- [ ] **Grafana Integration** - Pre-built dashboards for visualization
- [ ] **Rule Engine** - Automated actions based on sensor values
- [ ] **Camera Integration** - Store and display greenhouse images

#### Long-term Vision

- **Edge Computing**: Run lightweight processing on IoT devices
- **Predictive Maintenance**: Alert before sensors fail
- **Climate Optimization**: AI-driven recommendations for best growing conditions
- **Marketplace**: Share sensor configurations and automation rules
- **Integration Hub**: Connect with other agricultural systems

Want to contribute to any of these? Check out our [Contributing Guidelines](#-contributing)!

---

### 📄 License & Credits

#### License

This project is **private** and proprietary to **AppToLast**.  
All rights reserved. Unauthorized copying, modification, distribution, or use is strictly prohibited.

For licensing inquiries, contact: info@apptolast.com

#### Built With

- [Spring Boot](https://spring.io/projects/spring-boot) - Application framework
- [Kotlin](https://kotlinlang.org/) - Programming language
- [TimescaleDB](https://www.timescale.com/) - Time-series database
- [PostgreSQL](https://www.postgresql.org/) - Relational database
- [Redis](https://redis.io/) - In-memory cache
- [EMQX](https://www.emqx.io/) - MQTT broker
- [Docker](https://www.docker.com/) - Containerization
- [Gradle](https://gradle.org/) - Build tool

#### Contributors

Thank you to everyone who has contributed to this project!

- **AppToLast Team** - Initial development and ongoing maintenance
- Community contributors - Bug reports, feature suggestions, and improvements

#### Acknowledgments

- Special thanks to the Spring Boot community
- TimescaleDB team for excellent time-series database
- EMQX for robust MQTT broker
- All open-source contributors whose libraries we depend on

---

### 📞 Support

#### Need Help?

- **📖 Documentation**: Check our comprehensive docs
  - [Deployment Guide](DEPLOYMENT.md)
  - [Security Guide](SECURITY.md)
  - [MQTT Implementation](GREENHOUSE_MQTT_IMPLEMENTATION.md)
  - [Security Audit](SECURITY_AUDIT_REPORT.md)

- **🐛 Bug Reports**: [Create an issue](https://github.com/apptolast/InvernaderosAPI/issues/new)
- **💡 Feature Requests**: [Open a discussion](https://github.com/apptolast/InvernaderosAPI/discussions)
- **📧 Contact**: info@apptolast.com

#### Response Times

- **Critical bugs** (system down): 24 hours
- **Regular bugs**: 3-5 business days
- **Feature requests**: Considered for future releases
- **Questions**: Community support via GitHub Discussions

---

**🌱 Built with ❤️ by AppToLast using Spring Boot, Kotlin, and best practices for IoT and time-series data management.**

---
---
---

## Español

### 📑 Tabla de Contenidos

- [¿Qué es esto?](#-qué-es-esto-1)
- [Características](#-características-1)
- [Cómo Funciona](#%EF%B8%8F-cómo-funciona-1)
- [Inicio Rápido](#-inicio-rápido-1)
- [Arquitectura](#%EF%B8%8F-arquitectura-1)
- [Referencia API](#-referencia-api-1)
- [Estructura de Base de Datos](#%EF%B8%8F-estructura-de-base-de-datos-1)
- [Configuración](#-configuración-1)
- [Despliegue](#-despliegue-1)
- [Pruebas](#-pruebas-1)
- [WebSocket y Actualizaciones en Tiempo Real](#-websocket-y-actualizaciones-en-tiempo-real-1)
- [Integración MQTT](#-integración-mqtt-1)
- [Solución de Problemas](#-solución-de-problemas-1)
- [Mejores Prácticas de Seguridad](#-mejores-prácticas-de-seguridad-1)
- [Contribuir](#-contribuir-1)
- [Preguntas Frecuentes](#-preguntas-frecuentes-1)
- [Hoja de Ruta](#%EF%B8%8F-hoja-de-ruta-1)
- [Licencia y Créditos](#-licencia-y-créditos-1)
- [Soporte](#-soporte-1)

---

### 🎯 ¿Qué es esto?

**InvernaderosAPI** es un sistema inteligente de monitoreo de invernaderos que te ayuda a vigilar la salud de tus plantas en tiempo real.

**Piénsalo así:** Imagina que tienes un invernadero lleno de plantas delicadas y quieres saber la temperatura, humedad y humedad del suelo en todo momento, incluso cuando no estás allí. Esta API es como tener un asistente incansable que constantemente revisa todos los sensores, anota cada lectura en un cuaderno organizado, y te alerta inmediatamente cuando algo interesante sucede.

**En términos técnicos:** Es una API RESTful construida con Spring Boot y Kotlin que recibe datos de sensores vía MQTT, los almacena en una base de datos de series temporales (TimescaleDB), cachea datos recientes en Redis para acceso rápido, y transmite actualizaciones en tiempo real vía WebSocket a clientes conectados.

**Caso de uso del mundo real:** Un agricultor con múltiples invernaderos puede monitorear temperatura, humedad y otros factores ambientales desde su teléfono o computadora, recibir alertas cuando los valores salen de rangos seguros, y analizar tendencias históricas para optimizar las condiciones de cultivo.

---

### ✨ Características

#### 🎨 Lo que Este Sistema Puede Hacer

- **📊 Monitoreo en Tiempo Real** - Como tener una cámara de seguridad para tus plantas, pero en lugar de video, obtienes lecturas de sensores en vivo
- **💾 Almacenamiento de Datos Históricos** - Piénsalo como un diario que recuerda cada lectura de temperatura, cada pico de humedad, para siempre
- **⚡ Acceso Ultra Rápido** - Los datos recientes se cachean como mantener tus herramientas más usadas al alcance de la mano en lugar de en el garaje
- **🔔 Notificaciones en Vivo** - Recibe alertas instantáneas vía WebSocket—como recibir un mensaje de texto en el momento en que algo cambia
- **📈 Estadísticas y Análisis** - Ver patrones fácilmente: "¿Cuál fue la temperatura promedio la semana pasada?" o "¿Cuándo estuvo más alta la humedad?"
- **🌐 API RESTful** - Accede a tus datos desde cualquier lugar: aplicaciones web, móviles, o incluso hojas de cálculo Excel
- **🔐 Configuración Segura** - Todas las contraseñas y datos sensibles se guardan en variables de entorno, nunca hardcodeados
- **🐳 Listo para Docker** - Despliega en cualquier lugar con un comando—como un food truck que puede instalarse en cualquier lugar
- **📡 Protocolo MQTT** - Comunicación IoT estándar de la industria, como tener todos tus dispositivos hablando el mismo idioma
- **🔄 Escalamiento Automático** - TimescaleDB gestiona automáticamente grandes cantidades de datos de series temporales eficientemente

#### 🛠️ Características Técnicas

- ✅ **API REST** con Spring Boot 3.5.7 + Kotlin
- ✅ **Base de Datos de Series Temporales** con TimescaleDB (extensión de PostgreSQL)
- ✅ **Base de Datos de Metadatos** con PostgreSQL
- ✅ **Caché Distribuida** con Redis
- ✅ **Comunicación MQTT** con EMQX Broker
- ✅ **WebSocket** para actualizaciones en tiempo real a clientes
- ✅ **Documentación OpenAPI/Swagger**
- ✅ **Containerización** con Docker
- ✅ **Health Checks** y endpoints de monitoreo
- ✅ **JPA/Hibernate** para persistencia de datos
- ✅ **Spring Integration** para enrutamiento de mensajes
- ✅ **Actuator** para monitoreo en producción

---

### 🏗️ Cómo Funciona

Expliquemos la arquitectura usando una analogía de restaurante:

```
┌─────────────────┐
│ Sensores IoT    │ ← Los "clientes" enviando sus pedidos (datos de sensores)
│  (Invernadero)  │
└────────┬────────┘
         │
         ↓ Protocolo MQTT (como un mesero tomando pedidos)
┌─────────────────┐
│  EMQX Broker    │ ← La "ventana de pedidos" donde llegan todas las solicitudes
└────────┬────────┘
         │
         ↓ Spring Integration (el gerente de cocina distribuyendo tareas)
┌─────────────────┐
│  Spring Boot    │ ← La "cocina" procesando todo
│      API        │
└────────┬────────┘
         │
         ├────→ Caché Redis ← "Nevera de acceso rápido" para pedidos recientes
         │
         ├────→ TimescaleDB ← "Archivo de recetas" almacenando todos los datos históricos
         │
         ├────→ PostgreSQL ← "Sistema de inventario" con metadatos del invernadero
         │
         └────→ WebSocket ← "Intercomunicador" anunciando pedidos al personal del comedor (clientes)
```

**Cómo fluyen los datos:**

1. **Sensores Envían Datos (El Cliente Pide)**: Tus sensores del invernadero miden temperatura, humedad, etc., y envían estos datos vía MQTT—como clientes haciendo pedidos
   
2. **MQTT Broker Recibe (La Ventana de Pedidos)**: EMQX Broker recibe todos los mensajes en el topic "GREENHOUSE"—como una ventana de pedidos recogiendo todas las solicitudes

3. **Spring Boot Procesa (La Cocina)**: Nuestra API escucha el broker y procesa cada mensaje:
   - Valida los datos (se asegura de que el pedido esté correcto)
   - Guarda los últimos 1000 mensajes en Redis (mantiene pedidos recientes en el mostrador para acceso rápido)
   - Guarda TODOS los datos permanentemente en TimescaleDB (los escribe en el libro de registros permanente)
   - Transmite actualizaciones vía WebSocket (anuncia el nuevo pedido a todos los que están esperando)

4. **Clientes Solicitan Datos (Personal del Comedor)**: Tus aplicaciones pueden:
   - Pedir datos recientes desde Redis (acceso rápido a lo que está en el mostrador)
   - Consultar datos históricos desde TimescaleDB (buscar registros antiguos del archivo)
   - Suscribirse a WebSocket para actualizaciones en tiempo real (escuchar el intercomunicador)

**¿Por qué esta arquitectura?**

- **MQTT** → Eficiente para dispositivos IoT (bajo ancho de banda, entrega confiable)
- **Redis** → Acceso rápido a datos recientes (tiempo de respuesta en milisegundos)
- **TimescaleDB** → Optimizado para datos de series temporales (consultas como "temperatura promedio el mes pasado" son ultra rápidas)
- **WebSocket** → Notificaciones push en tiempo real (no necesitas preguntar constantemente "¿hay datos nuevos?")
- **PostgreSQL** → Almacenamiento confiable para invernaderos, inquilinos y configuración

---

### 🚀 Inicio Rápido

¡Pon el sistema en marcha en menos de 5 minutos!

#### Requisitos Previos

- **Docker** y **Docker Compose** (v20.10+ y v2.0+)
- **Git**
- ¡Eso es todo! (Java 21 es opcional, solo necesario para desarrollo local sin Docker)

#### Paso 1: Clonar el Repositorio

```bash
git clone https://github.com/apptolast/InvernaderosAPI.git
cd InvernaderosAPI
```

#### Paso 2: Configurar Variables de Entorno

```bash
# Copiar el archivo de ejemplo
cp .env.example .env

# Generar contraseñas seguras
openssl rand -base64 32
```

Ahora edita el archivo `.env` y reemplaza todos los placeholders `<your_*>` con valores reales:

```env
TIMESCALE_PASSWORD=tu_contraseña_generada_aqui
METADATA_PASSWORD=otra_contraseña_segura_aqui
REDIS_PASSWORD=contraseña_redis_aqui
MQTT_USERNAME=tu_usuario_mqtt
MQTT_PASSWORD=contraseña_mqtt_segura
```

⚠️ **IMPORTANTE**: ¡Nunca uses contraseñas por defecto en producción! Genera contraseñas únicas y fuertes para cada servicio.

#### Paso 3: Iniciar Todos los Servicios

```bash
# Iniciar todo con Docker Compose
docker-compose up -d

# Verificar que los servicios están corriendo
docker-compose ps
```

#### Paso 4: Verificar que Funciona

```bash
# Health check
curl http://localhost:8080/actuator/health

# Respuesta esperada: {"status":"UP"}
```

#### Paso 5: Explorar la API

Abre tu navegador y visita:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Documentación API**: http://localhost:8080/v3/api-docs

🎉 **¡Felicidades!** ¡Tu sistema de monitoreo de invernaderos ya está funcionando!

#### Próximos Pasos

- Revisa la [Referencia API](#-referencia-api-1) para comenzar a consultar datos
- Revisa la [documentación WebSocket](#-websocket-y-actualizaciones-en-tiempo-real-1) para recibir actualizaciones en tiempo real
- Ve [Integración MQTT](#-integración-mqtt-1) para conectar tus sensores

---

### 🏛️ Arquitectura


#### Componentes del Sistema

```
┌──────────────────────────────────────────────────────────────┐
│                  APLICACIONES CLIENTE                         │
│  (Dashboard Web, App Móvil, Herramientas de Análisis)        │
└───────┬──────────────────────────────────────────┬───────────┘
        │ HTTP/REST                    WebSocket/STOMP
        ↓                                          ↓
┌─────────────────────────────────────────────────────────────┐
│              SPRING BOOT API (Puerto 8080)                   │
│  ┌──────────┐  ┌──────────┐  ┌───────────┐  ┌────────────┐ │
│  │Controllers│  │ Servicios│  │Repositorios│  │  WebSocket │ │
│  └──────────┘  └──────────┘  └───────────┘  └────────────┘ │
└───┬────────┬──────────┬──────────┬────────────────┬─────────┘
    │        │          │          │                │
    │        │          │          │                │
    ↓        ↓          ↓          ↓                ↓
┌──────┐ ┌──────┐ ┌────────┐ ┌──────────┐    ┌──────────┐
│EMQX  │ │Redis │ │Timesca │ │PostgreSQL│    │  MQTT    │
│Broker│ │Caché │ │ leDB   │ │ Metadatos│    │Publishers│
│:1883 │ │:6379 │ │ :5432  │ │  :5433   │    └────┬─────┘
└──┬───┘ └──────┘ └────────┘ └──────────┘         │
   │                                               │
   ↑ Suscripción MQTT              Publicación MQTT ↑
   │                                               │
┌──┴───────────────────────────────────────────────┴────┐
│      SENSORES IoT (Dispositivos del Invernadero)      │
│  (Temperatura, Humedad, Humedad del Suelo, etc.)      │
└───────────────────────────────────────────────────────┘
```

#### Stack Tecnológico Explicado

| Componente | Tecnología | Propósito | Analogía |
|-----------|-----------|-----------|----------|
| **Framework API** | Spring Boot 3.5.7 | Maneja todas las solicitudes HTTP y lógica de negocio | La cocina del restaurante donde se preparan los pedidos |
| **Lenguaje** | Kotlin | Lenguaje de programación moderno y conciso | Las recetas y técnicas de cocina |
| **Message Broker** | EMQX | Distribuye mensajes MQTT de sensores | La ventana de pedidos recogiendo todas las solicitudes |
| **Caché Rápida** | Redis | Almacena los últimos 1000 mensajes para acceso rápido | El "mise en place" - ingredientes preparados y listos |
| **BD Series Temporales** | TimescaleDB | Optimizada para lecturas de sensores a lo largo del tiempo | El libro de recetas con cada plato jamás hecho registrado |
| **BD Metadatos** | PostgreSQL | Almacena información de invernaderos, usuarios, config | El manual de empleados y listas de inventario |
| **Tiempo Real** | WebSocket | Empuja actualizaciones en vivo a clientes | La campana de cocina que suena cuando los pedidos están listos |
| **Documentación** | Swagger/OpenAPI | Documentación interactiva de la API | El menú del restaurante con descripciones |
| **Containerización** | Docker | Empaqueta todo para fácil despliegue | El food truck que puede ir a cualquier lugar |

---

### 💻 Referencia API

*(Los endpoints y ejemplos son idénticos a la sección en inglés, por lo que se pueden reutilizar con títulos en español)*

#### URL Base
```
http://localhost:8080/api
```

#### Endpoints Principales

##### 1. Obtener Mensajes Recientes

Recupera los últimos N mensajes recibidos de los sensores.

```http
GET /api/greenhouse/messages/recent?limit=100
```

**Parámetros:**
- `limit` (opcional): Número de mensajes a recuperar (1-1000, por defecto: 100)

**Ejemplo de Solicitud:**
```bash
curl http://localhost:8080/api/greenhouse/messages/recent?limit=10
```

**Explicación No Técnica:** Como preguntar "Muéstrame las últimas 10 lecturas de temperatura" - obtienes una lista de los datos de sensores más recientes.

---

##### 2. Obtener Mensajes por Rango de Tiempo

Consulta mensajes dentro de un período específico.

```http
GET /api/greenhouse/messages/range?from={inicio}&to={fin}
```

**Explicación No Técnica:** Como preguntar "¿Cuáles fueron las temperaturas ayer entre 9 AM y 5 PM?"

---

##### 3. Obtener Último Mensaje

Recupera la lectura de sensor más reciente.

```http
GET /api/greenhouse/messages/latest
```

---

##### 4. Obtener Estadísticas de Sensor

Calcula estadísticas para un sensor específico durante un período de tiempo.

```http
GET /api/greenhouse/statistics/{sensorId}?period={periodo}
```

**Explicación No Técnica:** Como preguntar "¿Cuál fue la temperatura más baja, más alta y promedio hoy?"

---

##### 5. Obtener Resumen de Estadísticas

Obtiene un resumen completo de todos los sensores y setpoints.

```http
GET /api/greenhouse/statistics/summary?period={periodo}
```

---

### 🗄️ Estructura de Base de Datos

Nuestro sistema usa dos bases de datos especializadas, como tener sistemas de archivo separados para diferentes tipos de información:

#### TimescaleDB (Base de Datos de Series Temporales) - Puerto 5432

**Qué almacena:** Todas las lecturas de sensores a lo largo del tiempo

**Piénsalo como:** El cuaderno de bitácora de una estación meteorológica que registra la temperatura cada minuto durante años. Puedes responder rápidamente preguntas como "¿Cuál fue la tendencia de temperatura el mes pasado?" porque los datos están organizados cronológicamente.

**¿Por qué TimescaleDB?**
- **Consultas rápidas** en rangos de tiempo ("muéstrame la semana pasada")
- **Políticas de retención automáticas** (mantener 1 año, eliminar más antiguo)
- **Compresión** para datos antiguos (ahorra espacio de almacenamiento)
- **Agregados continuos** (promedios horarios/diarios pre-calculados)

---

#### PostgreSQL (Base de Datos de Metadatos) - Puerto 5433

**Qué almacena:** Configuración y datos de referencia

**Piénsalo como:** El directorio de la empresa que lista todos los empleados, departamentos y políticas. Estos datos no cambian a menudo, pero los necesitas para entender quién es quién y qué es qué.

---

#### Redis (Caché) - Puerto 6379

**Qué almacena:** Últimos 1000 mensajes para acceso ultra rápido

**Piénsalo como:** El caché de tu navegador web que mantiene sitios visitados recientemente listos para carga instantánea.

**¿Por qué Redis?**
- Tiempo de respuesta en **milisegundos** (vs. consultas de base de datos que pueden tomar segundos)
- Perfecto para solicitudes de "muéstrame las últimas lecturas"
- Reduce la carga en la base de datos principal
- Mantiene automáticamente solo datos recientes

---

### 🔧 Configuración

#### Variables de Entorno

Toda la configuración sensible se gestiona a través de variables de entorno. ¡Nunca hardcodees credenciales!

**Para generar contraseñas seguras:**
```bash
# Generar una contraseña aleatoria
openssl rand -base64 32

# O usa pwgen
pwgen -s 32 1
```

---

### 🐳 Despliegue

#### Desarrollo Local con Docker Compose

**1. Requisitos Previos:**
```bash
# Verificar que Docker está instalado
docker --version  # Debe ser v20.10+
docker-compose --version  # Debe ser v2.0+
```

**2. Configuración:**
```bash
# Clonar y configurar
git clone https://github.com/apptolast/InvernaderosAPI.git
cd InvernaderosAPI
cp .env.example .env
# Edita .env con tus contraseñas
nano .env
```

**3. Iniciar Servicios:**
```bash
# Construir e iniciar todos los servicios
docker-compose up -d

# Ver logs
docker-compose logs -f api

# Verificar estado de servicios
docker-compose ps
```

**4. Detener Servicios:**
```bash
# Detener todos los servicios
docker-compose down

# Detener y eliminar volúmenes (⚠️ elimina todos los datos)
docker-compose down -v
```

---

#### Despliegue en Producción

Para despliegues en producción, consulta la guía comprehensiva [DEPLOYMENT.md](DEPLOYMENT.md), que cubre:

- Despliegue en Kubernetes (entornos de desarrollo y producción)
- CI/CD con GitHub Actions
- Configuración SSL/TLS
- Backup y recuperación ante desastres
- Monitoreo y alertas
- Estrategias de escalamiento

---

### 🧪 Pruebas

#### Ejecutar Pruebas

```bash
# Ejecutar todas las pruebas
./gradlew test

# Ejecutar pruebas con reporte de cobertura
./gradlew test jacocoTestReport

# Ver reporte de cobertura
open build/reports/jacoco/test/html/index.html
```

---

### 🔌 WebSocket y Actualizaciones en Tiempo Real

¡Obtén actualizaciones de sensores en tiempo real enviadas a tu aplicación instantáneamente!

**Piénsalo como:** En lugar de llamar constantemente al restaurante para preguntar "¿Está listo mi pedido?", ellos TE llaman cuando está listo.

#### Conexión WebSocket

**Endpoint:**
```
ws://localhost:8080/ws/greenhouse
```

**Protocolo:** STOMP sobre WebSocket

#### Ejemplo JavaScript

```javascript
// Incluir librerías SockJS y STOMP
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/@stomp/stompjs@7/bundles/stomp.umd.min.js"></script>

<script>
// Crear conexión WebSocket
const socket = new SockJS('http://localhost:8080/ws/greenhouse');
const stompClient = Stomp.over(socket);

// Conectar al WebSocket
stompClient.connect({}, function(frame) {
  console.log('Conectado: ' + frame);
  
  // Suscribirse a mensajes del invernadero
  stompClient.subscribe('/topic/greenhouse/messages', function(message) {
    const data = JSON.parse(message.body);
    console.log('Nueva lectura de sensor:', data);
    
    // Actualiza tu UI aquí
    actualizarDashboard(data);
  });
});

function actualizarDashboard(data) {
  document.getElementById('temperatura').innerText = data.sensor01 + '°C';
  document.getElementById('humedad').innerText = data.sensor02 + '%';
}
</script>
```

Ver [greenhouse-client-demo.html](greenhouse-client-demo.html) para un ejemplo completo.

---

### 📡 Integración MQTT

#### Formato de Mensaje MQTT

**Topic:** `GREENHOUSE`

**Payload (JSON):**
```json
{
  "SENSOR_01": 24.5,    // Temperatura (°C)
  "SENSOR_02": 65.3,    // Humedad (%)
  "SETPOINT_01": 25.0,  // Temperatura objetivo
  "SETPOINT_02": 60.0,  // Humedad objetivo
  "SETPOINT_03": 70.0   // Setpoint adicional
}
```

**Explicación No Técnica:** Tus sensores envían mensajes al canal "GREENHOUSE", como publicar actualizaciones en un chat grupal. La API escucha este canal y procesa cada mensaje.

#### Conectar tus Sensores

**Usando Python:**
```python
import paho.mqtt.client as mqtt
import json

client = mqtt.Client()
client.username_pw_set("tu_usuario", "tu_contraseña")
client.connect("localhost", 1883, 60)

# Publicar datos de sensor
data = {
    "SENSOR_01": 24.5,
    "SENSOR_02": 65.3,
    "SETPOINT_01": 25.0
}

client.publish("GREENHOUSE", json.dumps(data))
```

---

### 🔍 Solución de Problemas

#### Problemas Comunes y Soluciones

**1. Los contenedores Docker no inician**

```bash
# Verificar si los puertos ya están en uso
lsof -i :8080
lsof -i :5432

# Ver logs del contenedor
docker-compose logs api
docker-compose logs timescaledb

# Reiniciar servicios
docker-compose restart
```

**2. Errores de conexión a base de datos**

```bash
# Verificar variables de entorno están configuradas
docker-compose config

# Verificar que la base de datos esté saludable
docker-compose exec timescaledb pg_isready

# Resetear base de datos (⚠️ elimina todos los datos)
docker-compose down -v
docker-compose up -d
```

**3. Fallos de conexión MQTT**

```bash
# Verificar que EMQX esté corriendo
docker-compose logs emqx

# Probar conexión MQTT
mosquitto_pub -h localhost -p 1883 -u usuario -P contraseña -t test -m "hola"

# Verificar credenciales en archivo .env
cat .env | grep MQTT
```

---

### 🔒 Mejores Prácticas de Seguridad

#### Gestión de Credenciales

**❌ NUNCA HAGAS ESTO:**
```java
// ¡NO hardcodees contraseñas!
String password = "misupersecreta123";
```

**✅ HAZ ESTO:**
```java
// Usa variables de entorno
String password = System.getenv("DATABASE_PASSWORD");
```

#### Lista de Verificación de Seguridad

- [ ] **Contraseñas Fuertes**: Usa `openssl rand -base64 32` para generar contraseñas
- [ ] **Variables de Entorno**: Nunca commits el archivo `.env` a Git
- [ ] **Contraseñas Diferentes**: Cada servicio debe tener una contraseña única
- [ ] **Rotar Credenciales**: Cambia contraseñas regularmente (cada 90 días)
- [ ] **Restringir Acceso**: Usa reglas de firewall para limitar acceso a bases de datos
- [ ] **Habilitar SSL/TLS**: Usa HTTPS y WebSocket seguro (WSS) en producción
- [ ] **Actualizar Dependencias**: Ejecuta `./gradlew dependencyUpdates` regularmente
- [ ] **Escaneo de Seguridad**: GitHub CodeQL escanea automáticamente vulnerabilidades
- [ ] **Principio de Privilegio Mínimo**: Otorga solo permisos necesarios
- [ ] **Logs de Auditoría**: Habilita y monitorea logs de acceso

Ver [SECURITY.md](SECURITY.md) para guías completas de seguridad.

---

### 🤝 Contribuir

¡Damos la bienvenida a contribuciones! Aquí está cómo puedes ayudar:

#### Comenzar

1. **Haz fork del repositorio**
2. **Clona tu fork**
   ```bash
   git clone https://github.com/TU_USUARIO/InvernaderosAPI.git
   cd InvernaderosAPI
   ```
3. **Crea una rama**
   ```bash
   git checkout -b feature/caracteristica-increible
   ```
4. **Configura el entorno**
   ```bash
   cp .env.example .env
   # Edita .env con tus credenciales
   ```

#### Hacer Cambios

1. **Escribe código** siguiendo el estilo existente
2. **Agrega pruebas** para nuevas características
3. **Actualiza documentación** si es necesario
4. **Ejecuta pruebas** para asegurar que nada se rompa
   ```bash
   ./gradlew test
   ```
5. **Ejecuta validación de seguridad**
   ```bash
   ./scripts/validate-security.sh
   ```

#### Mensajes de Commit

Sigue [Conventional Commits](https://www.conventionalcommits.org/):

```bash
feat: añadir endpoint para promedios de sensores por hora
fix: corregir manejo de zona horaria en estadísticas
docs: actualizar ejemplos de API en README
test: añadir pruebas de integración para WebSocket
```

---

### ❓ Preguntas Frecuentes

**P: ¿Puedo usar esto para múltiples invernaderos?**  
R: ¡Sí! El sistema soporta múltiples invernaderos. Cada mensaje de sensor puede incluir un campo `greenhouseId` para identificar la fuente.

**P: ¿Cuántos datos históricos puedo almacenar?**  
R: TimescaleDB puede manejar eficientemente años de datos. Puedes configurar políticas de retención para eliminar automáticamente datos antiguos (ej., mantener 1 año, eliminar más antiguo).

**P: ¿Qué pasa si se cae la conexión a internet?**  
R: MQTT soporta cola offline. Cuando la conexión se restaura, los mensajes serán entregados. Configura el nivel de QoS basado en tus necesidades de confiabilidad.

**P: ¿Puedo agregar sensores personalizados?**  
R: ¡Absolutamente! Solo envía campos adicionales en tu payload MQTT (ej., `"SENSOR_03": 123.4`) y se almacenarán automáticamente.

**P: ¿Cómo escalo para más sensores?**  
R: El sistema está diseñado para escalar horizontalmente. Puedes agregar más instancias de API detrás de un balanceador de carga, y TimescaleDB maneja grandes volúmenes de datos eficientemente.

**P: ¿Puedo exportar datos a Excel/CSV?**  
R: Sí, consulta la API y formatea la respuesta:
```bash
curl "http://localhost:8080/api/greenhouse/messages/range?from=2025-01-01T00:00:00Z&to=2025-01-02T00:00:00Z" | jq -r '.[] | [.timestamp, .sensor01, .sensor02] | @csv' > data.csv
```

---

### 🗺️ Hoja de Ruta

#### Características Próximas

- [ ] **Autenticación y Autorización** - Autenticación de usuario basada en JWT
- [ ] **App Móvil** - App React Native para iOS/Android
- [ ] **Alertas y Notificaciones** - Alertas por email/SMS cuando valores exceden umbrales
- [ ] **Machine Learning** - Análisis predictivo para condiciones óptimas de cultivo
- [ ] **Dashboard UI** - Dashboard basado en web con gráficos
- [ ] **Multi-tenancy** - Soporte para múltiples organizaciones
- [ ] **Exportación de Datos** - Exportaciones programadas CSV/Excel
- [ ] **Integración Grafana** - Dashboards pre-construidos para visualización
- [ ] **Motor de Reglas** - Acciones automatizadas basadas en valores de sensores
- [ ] **Integración de Cámaras** - Almacenar y mostrar imágenes del invernadero

---

### 📄 Licencia y Créditos

#### Licencia

Este proyecto es **privado** y propiedad de **AppToLast**.  
Todos los derechos reservados. La copia, modificación, distribución o uso no autorizado está estrictamente prohibido.

Para consultas de licencia, contactar: info@apptolast.com

#### Construido Con

- [Spring Boot](https://spring.io/projects/spring-boot) - Framework de aplicación
- [Kotlin](https://kotlinlang.org/) - Lenguaje de programación
- [TimescaleDB](https://www.timescale.com/) - Base de datos de series temporales
- [PostgreSQL](https://www.postgresql.org/) - Base de datos relacional
- [Redis](https://redis.io/) - Caché en memoria
- [EMQX](https://www.emqx.io/) - Broker MQTT
- [Docker](https://www.docker.com/) - Containerización
- [Gradle](https://gradle.org/) - Herramienta de construcción

#### Contribuidores

¡Gracias a todos los que han contribuido a este proyecto!

- **Equipo AppToLast** - Desarrollo inicial y mantenimiento continuo
- Contribuidores de la comunidad - Reportes de bugs, sugerencias de características y mejoras

---

### 📞 Soporte

#### ¿Necesitas Ayuda?

- **📖 Documentación**: Revisa nuestra documentación completa
  - [Guía de Despliegue](DEPLOYMENT.md)
  - [Guía de Seguridad](SECURITY.md)
  - [Implementación MQTT](GREENHOUSE_MQTT_IMPLEMENTATION.md)
  - [Auditoría de Seguridad](SECURITY_AUDIT_REPORT.md)

- **🐛 Reportes de Bugs**: [Crear un issue](https://github.com/apptolast/InvernaderosAPI/issues/new)
- **💡 Solicitudes de Características**: [Abrir una discusión](https://github.com/apptolast/InvernaderosAPI/discussions)
- **📧 Contacto**: info@apptolast.com

#### Tiempos de Respuesta

- **Bugs críticos** (sistema caído): 24 horas
- **Bugs regulares**: 3-5 días hábiles
- **Solicitudes de características**: Consideradas para versiones futuras
- **Preguntas**: Soporte de la comunidad vía GitHub Discussions

---

**🌱 Construido con ❤️ por AppToLast usando Spring Boot, Kotlin, y las mejores prácticas para IoT y gestión de datos de series temporales.**

---

_Última actualización: 2025-11-11_
