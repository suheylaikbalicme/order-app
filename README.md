# Order App (mr-CRM)

A Spring Boot + Thymeleaf + PostgreSQL CRM-style application for managing **customers**, **offers**, and **orders** with **audit logging**, **revision history**, and **Logo ERP integration** capabilities.

> Built as an internship/staj project and evolved into a CRM-like platform with structured workflows and admin tooling.

---

## ✨ Key Features

- **Authentication & Authorization**
  - Role-based access control (e.g., ADMIN / USER / VIEWER)
  - Secure login and protected admin areas

- **CRM Modules**
  - Customer management (profiles, CRM fields, interactions)
  - Offer management (create/edit, workflow transitions)
  - Order management (create/edit, workflow transitions)

- **Traceability**
  - **Revision history** for offers and orders
  - **Audit logging** for critical actions

- **Integrations**
  - **Logo ERP integration** (token service + sync jobs)
  - Sync status tracking (e.g., PENDING / SYNCED / FAILED)

- **Operational / Admin Tools**
  - Admin user/role management
  - Settings pages
  - Import/export utilities

---

## 🧱 Tech Stack

- **Java** (Spring Boot)
- **Spring Security**
- **Thymeleaf**
- **PostgreSQL**
- **Flyway** (DB migrations)
- **Maven**

---

## 📁 Project Structure (High Level)

- `src/main/java/...`
  - `auth/` → login, roles, user management
  - `customer/` → customer domain & pages
  - `offer/` → offer domain, revisions, workflow
  - `order/` → order domain, revisions, workflow
  - `audit/` → audit entities + service
  - `sync/` → Logo sync jobs & status
  - `admin/` → admin pages (settings, audit, export, sync)

- `src/main/resources/`
  - `templates/` → Thymeleaf views
  - `static/` → CSS/JS assets
  - `db/migration/` → Flyway migration scripts

---

## ▶️ Getting Started (Local)

### 1) Requirements
- Java (recommended: 17+)
- PostgreSQL
- Maven (or use `./mvnw`)

### 2) Configure Database
Create a PostgreSQL database and update:

`src/main/resources/application.properties`

Example:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/order_app
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD