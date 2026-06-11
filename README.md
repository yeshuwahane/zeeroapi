# ⚡ ZeeroStock API Backend

The backend engine powering the ZeeroStock trading terminal. Built as a high-performance **Ktor Server** in Kotlin, utilizing **JetBrains Exposed** ORM connected to a **Neon serverless Postgres** database cluster.

---

## 🛠️ Technology Stack

- **Kotlin & Ktor Server**: Lightweight asynchronous server framework utilizing Kotlin coroutines.
- **Neon PostgreSQL**: Fully-managed serverless Postgres instance for cloud database storage.
- **JetBrains Exposed ORM**: Type-safe SQL library wrapping database operations.
- **Kotlinx Serialization**: JSON engine processing model schemas.
- **Docker**: Containerized deployment configuration.

---

## 📂 Project Structure

```bash
└── src/main/kotlin/com/alien/
    ├── models/       # Data transfer objects (DTOs) and request schemas
    ├── plugins/      # Ktor feature modules (Database tables, Serialization, Routing config)
    ├── routes/       # API endpoints grouped by feature
    │   ├── AuthRouting.kt     # User Directory, registration, and role authentication
    │   └── ProductRouting.kt  # Inventory catalog, bidding engines, and admin moderation
    └── Application.kt # Server start entry point and config setup
```

---

## 🔑 Authentication & Roles

The API implements role-based path validation using custom authorization headers:
- **`X-User-Id` Header**: Verified against user entities inside the database to block/allow restricted administrative procedures.
- **Operations Moderator Credentials**: User `adm_02` is classified as the Operations Manager (Admin role) with access to product approvals and rejections but blocked from user deletions and user edits.

---

## 🖼️ Database-Backed Image Storage (Neon Postgres)

To prevent upload data loss on container restarts (since Railway hosts have ephemeral filesystems):
- **Persistence Workaround**: Uploaded image bytes are converted to Base64 strings and stored in the database (`ImagesTable`).
- **Endpoint**: Uploading images returns a relative URL like `/api/images/{UUID}`. When the mobile client requests it, the backend fetches the Base64 representation from Neon Postgres, decodes it back to raw bytes, and streams it as `image/png`.
- This ensures all catalog uploads remain fully persistent without requiring external storage mounts.

---

## 🚀 Running the API Locally

### 1. Database Connection Configuration
Make sure the PostgreSQL database connection string is properly configured. The connection properties are initialized inside `src/main/kotlin/com/alien/plugins/Database.kt`.

### 2. Startup Server
Run the local dev engine:
```bash
./gradlew run
```
The server will boot up locally at: `http://0.0.0.0:8080`

### 3. Build & Package Production Jar
To compile a single fat jar for server deployments:
```bash
./gradlew buildFatJar
```

---

## 🐳 Deployment with Docker
To build and run in a containerized environment (e.g. Railway or Docker Desktop):
```bash
docker build -t zeero-api .
docker run -p 8080:8080 zeero-api
```
