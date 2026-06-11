# ZeeroStock API Backend

This is the backend API engine powering the ZeeroStock trading terminal. Built as a high-performance **Ktor Server** in Kotlin and connected to a serverless **Neon PostgreSQL** database cluster.

- **Base URL**: `https://zeeroapi-production.up.railway.app`
- **Hosting**: Hosted on **Railway**
- **Database**: Serverless **Neon Postgres**

> [!IMPORTANT]
> **Railway Cold Starts**: Since the server is hosted on Railway's hobby tier, it may automatically spin down into an idle state when not in use. The initial request (such as logging in or loading products for the first time) may take 20–30 seconds to respond while the server wakes up. Subsequent requests will be near-instant.

---

##  Technology Stack

- **Kotlin & Ktor Server**: Lightweight asynchronous server framework utilizing Kotlin coroutines.
- **Neon PostgreSQL**: Fully-managed serverless Postgres instance for cloud database storage.
- **JetBrains Exposed ORM**: Type-safe SQL library wrapping database operations.
- **Kotlinx Serialization**: JSON engine processing model schemas.
- **Docker**: Containerized deployment configuration.

---

##  Project Structure

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

##  Authentication & Roles

The API implements role-based path validation using custom authorization headers:
- **`X-User-Id` Header**: Verified against user entities inside the database to block/allow restricted administrative procedures.
- **Operations Moderator Credentials**: User `adm_02` is classified as the Operations Manager (Admin role) with access to product approvals and rejections but blocked from user deletions and user edits.

---

##  Database-Backed Image Storage

To prevent upload data loss on container restarts (since Railway hosts have ephemeral filesystems):
- **Persistence Workaround**: Uploaded image bytes are converted to Base64 strings and stored in the database (`ImagesTable`).
- **Endpoint**: Uploading images returns a relative URL like `/api/images/{UUID}`. When the mobile client requests it, the backend fetches the Base64 representation from Neon Postgres, decodes it back to raw bytes, and streams it as `image/png`.

---

##  API Endpoints Documentation

### Authentication & User Directory (`/api/auth`)

- **`POST /api/auth/login`**: Validates credentials and role (CUSTOMER, SUPPLIER, ADMIN). Returns user details.
- **`POST /api/auth/register`**: Registers a new customer or supplier. Returns created user.
- **`GET /api/auth/users`**: Lists all registered users in the terminal (Admin only).
- **`PUT /api/auth/users/{id}`**: Updates user details such as name, email, password, and role (Chief Admin only).
- **`DELETE /api/auth/users/{id}`**: Deletes a user profile (Chief Admin only).

### Product Inventory & Auction Engine (`/api/products`)

- **`GET /api/products`**: Lists all products.
- **`GET /api/products/last-updated`**: Returns timestamp of last update to support offline-first sync.
- **`GET /api/products/{id}`**: Fetches details for a specific product.
- **`POST /api/products/{id}/update`**: Updates product details (title, description, price, category) (Supplier only).
- **`POST /api/products/{id}/bid`**: Submits a bid on an active auction item (Customer only).
- **`POST /api/products/{id}/approve`**: Approves a pending product listing, moving it to the marketplace (Admin & Operations Manager).
- **`POST /api/products/{id}/reject`**: Rejects and deletes a product listing (Admin & Operations Manager).
- **`POST /api/products/upload-image`**: Uploads image bytes, saves it as Base64 in Neon database, and returns resource path `/api/images/{id}`.

### Image Resource Serving (`/api/images`)

- **`GET /api/images/{id}`**: Serves Base64 images directly from the database, streaming them back as raw bytes (`image/png`).

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

##  Deployment with Docker
To build and run in a containerized environment (e.g. Railway or Docker Desktop):
```bash
docker build -t zeero-api .
docker run -p 8080:8080 zeero-api
```
