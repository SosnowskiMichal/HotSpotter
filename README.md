# HotSpotter - Backend API

![Java](https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.6-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-4EA94B?style=for-the-badge&logo=mongodb&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

> **A tool for automating behavioral source code analysis and technical debt detection.**

This repository contains the **Backend** source code for the HotSpotter system. The application processes Version Control System (Git) history, integrates structural metrics with behavioral data, and exposes analysis results via a REST API.

🔗 **Frontend Repository:** [https://github.com/marg4ryn/HotSpotter.git](https://github.com/marg4ryn/HotSpotter.git)

---

## 📖 About the Project

In the era of large-scale, long-term software projects, traditional static analysis is often insufficient to identify high-risk areas. **HotSpotter** integrates structural metrics with repository change history to provide a deeper insight into code evolution.

**Key Features:**

* **Hotspot Detection:** Identifies "hotspots"—complex code fragments that are frequently modified.
* **Knowledge Distribution:** Analyzes the "Bus Factor" and identifies risks associated with knowledge loss or key team members leaving.
* **Temporal Coupling:** Detects files that change together implicitly, revealing hidden architectural dependencies.
* **Code City:** Prepares data for 3D visualization of the project structure (served by the frontend).
* **Quality Metrics:** Calculates complexity, line counts, code smells, bugs and vulnerabilities.

> **Note:** The backend is configured to clone and analyze **public repositories** only.

---

## 🚀 Getting Started (Docker Compose)

The repository includes a `docker-compose` configuration that sets up the Backend, MongoDB, and optionally SonarQube.

### Prerequisites
* Docker & Docker Compose

### Installation Steps
1.  Clone the repository.
2.  Configure environment variables (see the [Configuration](#-configuration) section).
3.  Start the services:

```bash
docker-compose up -d
````

* **Backend API:** `http://localhost:8080`
* **Swagger Documentation:** `http://localhost:8080/api/swagger-ui.html`
* **SonarQube** (if enabled): `http://localhost:9000`

-----

## ⚙️ Configuration

The application is highly configurable via environment variables. You can set these in your `docker-compose.yml` or a `.env` file.

### 🔴 Required Variables

| Variable      | Description                                                               |
|:--------------|:--------------------------------------------------------------------------|
| `MONGODB_URI` | Connection string for MongoDB (e.g., `mongodb://mongo:27017/hotspotter`). |

### 🟡 Optional Variables

If not provided, the application will use default values or disable specific features.

**General Settings:**

| Variable                         | Default                     | Description                                           |
|:---------------------------------|:----------------------------|:------------------------------------------------------|
| `HOST_BASE_URL`                  | `http://localhost:8080/api` | Base URL of the backend API.                          |
| `PROJECT_BASE_DIRECTORY`         | `/tmp/hotspotter`           | Temporary directory for cloning repositories.         |
| `REPOSITORIES_MIN_FREE_SPACE_GB` | `10`                        | Minimum disk space (GB) required to perform analysis. |
| `LOGGING_LEVEL`                  | `INFO`                      | Application logging level.                            |
| `CORS_ALLOWED_ORIGINS`           | `http://localhost:5173/`    | Frontend origins allowed by CORS policy.              |

**External Integrations:**

| Variable                         | Description                                                                                     |
|:---------------------------------|:------------------------------------------------------------------------------------------------|
| `SONAR_HOST_URL`                 | URL of the SonarQube Community Edition instance (if used).                                      |
| `GOOGLE_PROJECT_ID`              | Google Cloud Project ID (required for issue message translation).                               |
| `GOOGLE_APPLICATION_CREDENTIALS` | Path to the JSON key file for Google Cloud. If missing, issue messages will be in English only. |

**Security & Authentication (JWT):**

| Variable               | Default     | Description                                               |
|:-----------------------|:------------|:----------------------------------------------------------|
| `JWT_SECRET`           | -           | Secret key for signing JWTs (`openssl rand -base64 128`). |
| `COOKIE_SAME_SITE`     | `Lax`       | SameSite policy for session cookies.                      |
| `COOKIE_DOMAIN`        | `localhost` | Domain for cookies.                                       |
| `COOKIE_SECURE`        | `false`     | Force HTTPS for cookies (set to `true` in production).    |

**Security & Authentication (Google OAuth2):**
*Required only if Google Login is enabled.*

| Variable               | Default     | Description                                               |
|:-----------------------|:------------|:----------------------------------------------------------|
| `GOOGLE_CLIENT_ID`     | -           | OAuth2 Client ID from Google Cloud Console.               |
| `GOOGLE_CLIENT_SECRET` | -           | OAuth2 Client Secret.                                     |
| `LOGIN_REDIRECT_URL`   | -           | Frontend URL to redirect to after successful login.       |

-----

## 📚 API Documentation

Interactive API documentation (OpenAPI/Swagger) is available after starting the application:

👉 **[http://localhost:8080/api/swagger-ui.html](http://localhost:8080/api/swagger-ui.html)**

-----

## 👥 Project Team

This project was developed as an Engineering Thesis.

**Developers:**

* **Wiktor Piekarski** – *Frontend*
* **Jan Powęski** – *Frontend*
* **Michał Sosnowski** – *Backend*
* **Michał Wąsiński** – *Backend & DevOps*

**Supervisor:**

* **dr inż. Michał Szczepanik**

-----

© 2025 HotSpotter Team

```
```