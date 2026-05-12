# Banking Management System

Desktop **Java Swing + JDBC + MySQL** application demonstrating layered architecture (MVC-style separation), secure persistence with **PreparedStatement**, and portfolio-ready banking workflows.

## Highlights

- **Authentication**: registration, salted SHA-256 password hashing, role-aware routing (customer vs administrator linked through `admins`), forgot-password via security Q&A.
- **Banking**: account auto-numbering, deposit / withdraw / transfer inside JDBC transactions, receipt UUIDs, mini-statements, searchable transaction history, CSV export.
- **Administration**: user activation toggle, freeze/unfreeze accounts, global ledger filters, activity audit trail.
- **UX**: tabbed dashboards, validation dialogs, **SwingWorker** for non-blocking loads, dark/light theme toggle.

## Architecture

| Layer | Packages |
|-------|-----------|
| Model | `com.bankmgmt.model` — entities & enums |
| View | `com.bankmgmt.ui` — Swing frames/dialogs |
| Controller / orchestration | `com.bankmgmt.service` — business rules |
| Persistence | `com.bankmgmt.dao` — JDBC DAOs behind interfaces |
| Infrastructure | `com.bankmgmt.database`, `com.bankmgmt.utils`, `com.bankmgmt.exceptions` |

Composition root: `com.bankmgmt.AppContext` wires singleton DAOs and services.

UML (PlantUML): open `docs/UML_class_diagram.puml` in your IDE PlantUML plugin or at [plantuml.com](https://www.plantuml.com/plantuml).

## Prerequisites

- **JDK 17+**
- **Apache Maven 3.9+**
- **MySQL 8.x** (local instance)

## Database setup

1. Create schema and seed admin (username **`admin`**, password **`Admin@123`**, recovery answer **`seedanswer`** for the seeded security question):

   ```bash
   mysql -u root -p < sql/schema.sql
   ```

2. Edit JDBC settings in `src/main/resources/database.properties`:

   - `db.url` — host, port, database name (`bank_mgmt`)
   - `db.username` / `db.password` — MySQL credentials

## Build & run

```bash
mvn clean package
```

Dependencies are copied to `target/lib`. Launch from `target` so the manifest classpath resolves:

**Windows (PowerShell)**

```powershell
cd target
java -cp "banking-management-system-1.0.0.jar;lib/*" com.bankmgmt.BankingApp
```

**macOS / Linux**

```bash
cd target
java -cp "banking-management-system-1.0.0.jar:lib/*" com.bankmgmt.BankingApp
```

Main class: `com.bankmgmt.BankingApp`.

During development you can also run:

```bash
mvn exec:java
```

(Uses your Maven classpath including MySQL Connector/J.)

## Default credentials

| Role | Username | Password | Recovery hint |
|------|----------|----------|----------------|
| Admin | `admin` | `Admin@123` | Answer `seedanswer` for the seeded security question |

Register additional customers through the **Register** button on the login screen.

## Transaction semantics

`BankingService` coordinates multi-step operations (`UPDATE accounts` + `INSERT transactions`) with `connection.setAutoCommit(false)` and rollback on failure to keep balances and ledger rows consistent.

## Screenshots

Add portfolio screenshots under `docs/screenshots/` (see `docs/screenshots/SCREENSHOTS_README.txt`).

## Security note for interviews

Password storage uses **salted SHA-256** as an explicit “hash simulation” suitable for coursework; production systems should migrate to **Argon2id**, **bcrypt**, or **PBKDF2** with appropriate parameters and pepper/key-management policies.

## License

Educational / portfolio use.
