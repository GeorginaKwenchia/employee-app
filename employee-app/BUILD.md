# The Build Phase

---

## What is DevOps?

Before we write a single command, we need to understand why we are here.

In the early days of software, there were two separate teams:

- **Developers** — wrote the code
- **Operations** — ran the servers and deployed the software

These teams rarely talked to each other. Developers would finish writing code, throw it over the wall to operations, and operations would struggle to get it running. Deployments were slow, painful, and full of surprises. The classic complaint from operations was: *"it works on the developer's machine but not on the server."*

**DevOps** is the practice of breaking down that wall.

It is not a job title. It is not a tool. It is a **culture and a set of practices** that brings development and operations together so that software can be built, tested, and delivered faster and more reliably.

The core idea of DevOps is **automation**. Instead of humans manually building, testing, and deploying software, you automate every step so it happens the same way every time.

```
Without DevOps:
  Developer writes code  →  manually sends to ops  →  ops manually deploys  →  hope it works

With DevOps:
  Developer pushes code  →  automated pipeline builds, tests, packages, deploys  →  done
```

DevOps is built on a framework called the **Software Development Lifecycle**.

---

## What is the SDLC?

The **Software Development Lifecycle (SDLC)** is the structured process that takes an idea from concept to a running application in production. It gives teams a repeatable, predictable way to deliver software.

The SDLC has the following phases:

```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│  PLAN   │───▶│  CODE   │───▶│  BUILD  │───▶│  TEST   │───▶│ RELEASE │───▶│ DEPLOY  │───▶│ MONITOR │
└─────────┘    └─────────┘    └─────────┘    └─────────┘    └─────────┘    └─────────┘    └─────────┘
```

| Phase | What happens |
|-------|-------------|
| **Plan** | Define what to build. Requirements, user stories, sprint planning |
| **Code** | Developers write the source code and commit it to Git |
| **Build** | Source code is turned into a runnable application |
| **Test** | The application is tested automatically to catch bugs |
| **Release** | The tested application is packaged and made ready for deployment |
| **Deploy** | The package is deployed to a server or cloud environment |
| **Monitor** | The running application is observed for errors, performance, and uptime |

This is a **cycle** — not a one-time process. Every time a developer pushes new code, the cycle runs again. In modern DevOps, this cycle can complete multiple times per day.

### Where we are in this course

```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│  PLAN   │    │  CODE   │    │  BUILD  │    │  TEST   │    │ RELEASE │    │ DEPLOY  │    │ MONITOR │
│  done   │    │  done   │    │  ← YOU  │    │  next   │    │ Docker  │    │   K8s   │    │Grafana  │
└─────────┘    └─────────┘    └─────────┘    └─────────┘    └─────────┘    └─────────┘    └─────────┘
```

We have planned the application and written the code. Now we are at the **Build** phase.

---

## What is a Build?

A **build** is the process of taking raw source code and turning it into something that can actually run.

Think of it like cooking. A recipe (source code) is not food. You cannot eat a recipe. You have to follow the steps — gather ingredients, prepare them, cook them — before you have something you can actually consume. The cooking process is the build.

```
Source code  =  the recipe
Build        =  the cooking process
Running app  =  the meal
```

### Why do we need a build?

Source code on its own cannot run. It needs several things before it becomes a working application:

**1. Dependencies**
Your code does not do everything from scratch. It uses libraries written by other people. These libraries need to be downloaded and made available before your code can use them.

**2. Compilation (some languages)**
Some languages cannot be understood directly by a computer. The source code must be translated into a lower-level format the machine can execute. This translation is called compilation.

**3. Configuration**
The application needs to know where the database is, what port to listen on, and other environment-specific settings.

Without the build step, you have a folder of text files. After the build step, you have a running application.

### What happens during a build?

```
Step 1 — Resolve dependencies
  Read the dependency file (requirements.txt / package.json / pom.xml)
  Download all required libraries
  Make them available to your code

Step 2 — Compile (if required)
  Translate source code into executable format
  Python and Node.js skip this step — they are interpreted
  Java requires this step — it is compiled

Step 3 — Run
  Start the application process
  The app listens for requests

Step 4 — Test
  Run automated tests against the running application
  Catch bugs before the code moves to the next phase
```

---

## Interpreted vs Compiled Languages

This is one of the most important concepts in the build phase.

### Interpreted languages

The source code is read and executed line by line at runtime. There is no separate compile step. You write the code, you run it.

Examples: **Python**, **JavaScript (Node.js)**, **Ruby**

```
Python:
  you write  app.py
  you run    python app.py
  Python reads and executes the file directly
```

### Compiled languages

The source code must first be translated into bytecode or machine code by a compiler. Only then can it be run. The output of compilation is a binary file or bytecode file.

Examples: **Java**, **Go**, **C**, **C++**, **Rust**

```
Java:
  you write    Employee.java  (human-readable source)
  you compile  javac Employee.java  →  Employee.class  (bytecode)
  you run      java Employee  (JVM executes the bytecode)
```

### Why does this matter for the build phase?

| | Interpreted | Compiled |
|---|---|---|
| Compile step needed | No | Yes |
| Build artifact | None — code runs directly | Binary file or JAR |
| Error discovery | At runtime | At compile time |
| Example | Python, Node.js | Java, Go |

If you try to run a compiled language without compiling first, you have nothing to run. The build step is not optional.

---

## What is a Dependency?

A dependency is a library — code written by someone else — that your application needs in order to work.

For example, when you write a Python web server, you do not write the HTTP handling code from scratch. You use a library called Flask that already does it. Flask is a dependency.

Every language has:
1. A **dependency file** — where you declare what libraries you need
2. A **package manager** — the tool that reads that file and downloads the libraries

```
You declare what you need  →  package manager downloads it  →  your code can use it
```

| Language | Dependency file | Package manager | Libraries stored in |
|----------|----------------|-----------------|---------------------|
| Python | `requirements.txt` | `pip` | Python site-packages |
| Node.js | `package.json` | `npm` | `node_modules/` folder |
| Java | `pom.xml` | `mvn` (Maven) | `~/.m2/repository` |

The dependency file is always committed to Git. The downloaded libraries are never committed — they are too large and can always be re-downloaded from the internet.

---

## How to Identify a Programming Language

When you open any codebase — in a job, in an interview, in open source — the first thing you do is look for the dependency file. That file tells you the language, the build tool, and how to get the project running.

| File you see | Language | Build tool | How to install |
|---|---|---|---|
| `requirements.txt` | Python | pip | `pip install -r requirements.txt` |
| `package.json` | JavaScript / Node.js | npm | `npm install` |
| `pom.xml` | Java | Maven | `mvn compile` |
| `build.gradle` | Java / Kotlin | Gradle | `gradle build` |
| `go.mod` | Go | Go modules | `go mod download` |
| `Gemfile` | Ruby | Bundler | `bundle install` |
| `Cargo.toml` | Rust | Cargo | `cargo build` |

This is a skill you will use every day as a DevOps engineer. You will clone repositories you have never seen before and need to figure out how to build them. The dependency file is always your starting point.

---

## How Each Language Builds

### Python

```
1. pip install -r requirements.txt    ← download dependencies
2. python app.py                      ← run directly (no compile step)
```

No compilation. No build artifact. The source code is the thing that runs.

### Node.js

```
1. npm install                        ← download dependencies into node_modules/
2. npm start                          ← run directly (no compile step)
```

No compilation. No build artifact. Like Python, the source code runs directly.

### Java

```
1. mvn compile                        ← download dependencies + compile .java → .class
2. mvn package                        ← bundle .class files into a JAR
3. java -jar target/app.jar           ← run the JAR
```

Explicit compilation required. Produces a build artifact — the JAR file. The JAR is a self-contained package with your compiled code and all dependencies bundled inside.

### The Maven lifecycle

Maven has a fixed sequence of phases. Each phase includes all the ones before it:

```
mvn compile    →  download dependencies, compile source code
mvn test       →  compile + run tests
mvn package    →  compile + test + bundle into JAR
mvn clean      →  delete all compiled output (target/ directory)
```

---

## Java and the JVM

Before you can understand Maven, you need to understand how Java actually runs.

Most languages run directly on the operating system. Java does not. Java runs on a virtual machine — the **Java Virtual Machine (JVM)**.

```
Other languages:
  source code  →  machine code  →  runs on OS

Java:
  source code  →  bytecode  →  JVM  →  runs on OS
```

This extra layer is what makes Java platform-independent. The same `.class` bytecode file runs on Windows, Linux, and macOS — as long as a JVM is installed. This is the origin of Java's original promise: *write once, run anywhere*.

### The two steps to run Java

**Step 1 — Compile**

The Java compiler (`javac`) reads your `.java` source files and produces `.class` bytecode files.

```
Employee.java  →  javac Employee.java  →  Employee.class
```

The `.class` file is not human-readable. It is not machine code either. It is bytecode — an intermediate format that only the JVM understands.

**Step 2 — Run**

The JVM reads the `.class` bytecode and executes it.

```
Employee.class  →  java Employee  →  running program
```

### What is a JAR?

A real application has hundreds of `.class` files plus configuration files, templates, and resources. Distributing all of these as loose files is impractical.

A **JAR** (Java ARchive) is a ZIP file that bundles all of it together:

```
employee-backend-1.0.0.jar
  ├── com/landmark/employee/Employee.class
  ├── com/landmark/employee/EmployeeController.class
  ├── com/landmark/employee/EmployeeRepository.class
  ├── application.properties
  └── META-INF/MANIFEST.MF   ← tells Java which class has main()
```

Spring Boot produces a **fat JAR** (also called an uber JAR) — it includes not just your compiled code but all your dependencies bundled inside. The result is a single self-contained file you can run on any machine with Java installed.

```bash
java -jar target/employee-backend-1.0.0.jar
```

No `pip install`. No `npm install`. Everything is already inside the JAR.

---

## Maven

Maven is the build tool for Java. It does three things:

1. **Dependency management** — downloads libraries from the internet and caches them locally
2. **Build automation** — compiles, tests, and packages your code in a fixed, predictable sequence
3. **Project standardisation** — enforces a standard directory layout so every Maven project looks the same

### The standard directory layout

Maven enforces a convention for where files live. You do not configure this — it is fixed:

```
backend-java/
├── pom.xml                          ← project definition and dependencies
└── src/
    ├── main/
    │   ├── java/                    ← your application source code
    │   └── resources/               ← config files (application.properties)
    └── test/
        ├── java/                    ← your test source code
        └── resources/               ← test config (uses H2 instead of Postgres)
```

This convention means any Java developer can clone any Maven project and immediately know where to find the source code, the tests, and the config.

### pom.xml

The `pom.xml` (Project Object Model) is Maven's dependency file. It defines everything about your project:

```xml
<project>
  <!-- Who is this project? -->
  <groupId>com.landmark</groupId>       <!-- your organisation -->
  <artifactId>employee-backend</artifactId>  <!-- project name -->
  <version>1.0.0</version>             <!-- version -->

  <!-- What does it need? -->
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
      <!-- no version — inherited from parent -->
    </dependency>
    <dependency>
      <groupId>com.h2database</groupId>
      <artifactId>h2</artifactId>
      <scope>test</scope>   <!-- only available during tests -->
    </dependency>
  </dependencies>
</project>
```

The `<scope>` field controls when a dependency is available:

| Scope | Available at compile | Available at runtime | Available in tests |
|-------|---------------------|---------------------|--------------------|
| (none / compile) | Yes | Yes | Yes |
| `runtime` | No | Yes | Yes |
| `test` | No | No | Yes |

This is why H2 (the in-memory test database) is never bundled into the production JAR — it has `<scope>test</scope>`.

### The Maven build lifecycle

Maven's build process is a fixed sequence of **phases**. Each phase automatically includes all the phases before it.

```
validate  →  compile  →  test  →  package  →  verify  →  install  →  deploy
```

| Phase | What it does |
|-------|--------------|
| `validate` | Checks the `pom.xml` is valid and all required information is present |
| `compile` | Downloads dependencies, compiles `.java` → `.class` into `target/classes/` |
| `test` | Compiles test code, runs all tests — build fails here if any test fails |
| `package` | Bundles compiled code into a JAR at `target/employee-backend-1.0.0.jar` |
| `verify` | Runs integration checks on the package |
| `install` | Copies the JAR into your local Maven repository (`~/.m2/repository`) |
| `deploy` | Uploads the JAR to a remote repository (Nexus, Artifactory, etc.) |

When you run `mvn package`, Maven runs validate → compile → test → package in sequence. You do not run them individually.

```bash
mvn compile          # stops after compile
mvn test             # runs compile then test
mvn package          # runs compile, test, then package
mvn package -DskipTests   # runs compile and package, skips test phase
mvn clean            # deletes target/ — forces a full rebuild next time
mvn clean package    # delete everything, then build fresh
```

### The local repository

The first time you run `mvn compile`, Maven downloads every dependency from the internet and stores it in `~/.m2/repository/`. Every subsequent build reads from this local cache — no internet required.

```
~/.m2/repository/
  org/springframework/boot/spring-boot-starter-web/3.2.0/
    spring-boot-starter-web-3.2.0.jar
  org/postgresql/postgresql/42.7.1/
    postgresql-42.7.1.jar
```

This is why the first Maven build is slow and all subsequent builds are fast.

### What gets produced

After `mvn package`, Maven creates a `target/` directory:

```
target/
  classes/                          ← compiled .class files
  test-classes/                     ← compiled test .class files
  surefire-reports/                 ← test results (XML + HTML)
  employee-backend-1.0.0.jar        ← the fat JAR (your artifact)
```

The `target/` directory is never committed to Git — it is always regenerated by the build.

---

## npm

npm (Node Package Manager) is the build tool for Node.js. Like Maven, it does two things:

1. **Dependency management** — downloads packages from the npm registry and stores them in `node_modules/`
2. **Script runner** — runs commands defined in `package.json` (start, test, build, etc.)

### package.json

`package.json` is npm's equivalent of `pom.xml`. It defines the project and its dependencies:

```json
{
  "name": "employee-backend-node",
  "version": "1.0.0",
  "main": "app.js",
  "scripts": {
    "start": "node app.js",
    "test": "jest --forceExit"
  },
  "dependencies": {
    "express": "^4.18.2",
    "cors": "^2.8.5",
    "pg": "^8.11.3"
  },
  "devDependencies": {
    "jest": "^29.7.0",
    "supertest": "^6.3.4"
  }
}
```

### dependencies vs devDependencies

This is the npm equivalent of Maven's `<scope>`:

| Field | When used | Installed by default | Bundled in production |
|-------|-----------|---------------------|-----------------------|
| `dependencies` | Runtime — the app needs these to run | Yes | Yes |
| `devDependencies` | Development only — testing, linting, build tools | Yes (locally) | No |

`jest` and `supertest` are in `devDependencies` because you only need them to run tests. A production server does not need a test framework.

When deploying to production, you can install only runtime dependencies:

```bash
npm install --omit=dev
```

### Version ranges

The `^` symbol in `"express": "^4.18.2"` means *compatible with 4.18.2*. npm will install the latest version that does not break compatibility (any `4.x.x` where `x >= 18.2`).

| Symbol | Meaning | Example |
|--------|---------|--------|
| `^4.18.2` | Any compatible version (same major) | `4.18.2`, `4.19.0`, `4.20.1` |
| `~4.18.2` | Patch updates only | `4.18.2`, `4.18.3`, `4.18.9` |
| `4.18.2` | Exact version only | `4.18.2` |

### package-lock.json

Version ranges introduce a problem: two developers running `npm install` at different times might get different versions. `package-lock.json` solves this by recording the exact version of every package that was installed.

```
package.json       →  "express": "^4.18.2"   (range — flexible)
package-lock.json  →  "express": "4.19.2"    (exact — locked)
```

| File | Committed to Git | Purpose |
|------|-----------------|--------|
| `package.json` | Yes | Declares what you need |
| `package-lock.json` | Yes | Locks exact versions for reproducible installs |
| `node_modules/` | No | Downloaded packages — never commit |

### npm scripts

The `scripts` section in `package.json` defines shortcuts for common commands:

```json
"scripts": {
  "start": "node app.js",
  "test": "jest --forceExit"
}
```

You run them with `npm run <name>`, or for the built-in names `start` and `test`, just `npm start` and `npm test`.

This means the person running the project does not need to know the exact command — they just run `npm start` and npm looks up what that means in `package.json`.

### node_modules

After `npm install`, all downloaded packages live in `node_modules/`:

```
node_modules/
  express/
  cors/
  pg/
  jest/
  supertest/
  ... (hundreds of transitive dependencies)
```

This folder can contain hundreds of packages because each package has its own dependencies. It is always excluded from Git via `.gitignore`. Anyone who clones the repo runs `npm install` to recreate it.

---

## Practical — Building the Employee Directory App

The Employee Directory app is a full-stack application:

```
Frontend (HTML/JS in browser)
        │
        │  HTTP calls to /api/*
        ▼
Backend (REST API on port 5000)
        │
        │  SQL queries
        ▼
PostgreSQL database (port 5432)
```

The backend is implemented in three languages so you can practice the build phase in Python, Node.js, and Java. All three expose the exact same API — the frontend works with any of them.

### Project structure

```
employee-app/
├── backend/           ← Python (Flask)
│   ├── requirements.txt
│   ├── app.py
│   └── test_app.py
│
├── backend-node/      ← Node.js (Express)
│   ├── package.json
│   ├── app.js
│   └── app.test.js
│
├── backend-java/      ← Java (Spring Boot + Maven)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/landmark/employee/
│       └── test/java/com/landmark/employee/
│
└── frontend/          ← HTML/JS (shared, works with all backends)
    └── index.html
```

### API endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/health` | Check app and database connectivity |
| `GET` | `/api/employees` | List all employees |
| `POST` | `/api/employees` | Create an employee |
| `PUT` | `/api/employees/<id>` | Update an employee |
| `DELETE` | `/api/employees/<id>` | Delete an employee |
| `GET` | `/api/stats` | Total count, departments, latest hire |

---

## Step 0 — Start the Database

All three backends need a PostgreSQL database. Start it once with Docker:

```bash
docker run -d --name pg \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=employees \
  -p 5432:5432 \
  postgres:15
```

Verify it is running:

```bash
docker ps
```

You should see a container named `pg` with port `5432` listed. Leave this running for all three practicals.

---

## Practical 1 — Python Build

### Identify the language

```bash
ls employee-app/backend/
```

You see `requirements.txt` → this is a Python project, build tool is `pip`.

### Read the dependency file

```bash
cat employee-app/backend/requirements.txt
```

| Package | Version | Purpose |
|---------|---------|---------|
| `flask` | 3.1.0 | Web framework |
| `flask-cors` | 5.0.0 | Allows browser to call the API across ports |
| `flask-sqlalchemy` | 3.1.1 | ORM — write Python instead of raw SQL |
| `psycopg2-binary` | 2.9.9 | PostgreSQL driver |
| `boto3` | 1.35.0 | AWS SDK for S3 and CloudWatch |
| `gunicorn` | 23.0.0 | Production web server (used in Docker, not locally) |
| `watchtower` | 3.3.0 | Sends logs to AWS CloudWatch |
| `prometheus-flask-exporter` | 0.23.1 | Exposes `/metrics` for monitoring |
| `pytest` | 8.3.0 | Test runner |
| `pytest-flask` | 1.3.0 | Flask test helpers |
| `moto` | 5.0.0 | Mocks AWS services in tests |

### Install dependencies

```bash
cd employee-app/backend
pip install -r requirements.txt
```

### Run the application

```bash
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/employees python app.py
```

### Verify it is running

```bash
curl http://localhost:5000/api/health
```

```json
{"status": "healthy", "db": "connected"}
```

```bash
curl http://localhost:5000/api/employees
```

```json
[]
```

### Add an employee

```bash
curl -X POST http://localhost:5000/api/employees \
  -F "name=Jane Smith" \
  -F "email=jane@example.com" \
  -F "role=Engineer" \
  -F "department=Engineering"
```

### Run the tests

Stop the running app first (`Ctrl+C`), then:

```bash
pytest
```

Tests use SQLite in memory — no Postgres needed. Each test creates a fresh database, runs, and tears it down.

---

## Practical 2 — Node.js Build

### Identify the language

```bash
ls employee-app/backend-node/
```

You see `package.json` → this is a Node.js project, build tool is `npm`.

### Read the dependency file

```bash
cat employee-app/backend-node/package.json
```

**Runtime dependencies:**

| Package | Version | Purpose |
|---------|---------|---------|
| `express` | ^4.18.2 | Web framework |
| `cors` | ^2.8.5 | Allows browser to call the API across ports |
| `pg` | ^8.11.3 | PostgreSQL client |

**Dev dependencies (test only):**

| Package | Version | Purpose |
|---------|---------|---------|
| `jest` | ^29.7.0 | Test runner |
| `supertest` | ^6.3.4 | HTTP testing without a real server |

Note the difference between `dependencies` and `devDependencies`. Runtime dependencies are needed to run the app. Dev dependencies are only needed during development and testing — they are not included in production builds.

### Install dependencies

```bash
cd employee-app/backend-node
npm install
```

After this runs, you will see:
- `node_modules/` folder created — contains all downloaded packages
- `package-lock.json` created — locks exact versions of every package

| File / Folder | Committed to Git | Purpose |
|---------------|-----------------|---------|
| `package.json` | Yes | Declares what you need |
| `package-lock.json` | Yes | Locks exact versions |
| `node_modules/` | No | Downloaded code — never commit this |

### Run the application

```bash
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/employees npm start
```

### Verify it is running

```bash
curl http://localhost:5000/api/health
```

```json
{"status": "healthy", "db": "connected"}
```

```bash
curl http://localhost:5000/api/employees
```

```json
[]
```

### Add an employee

```bash
curl -X POST http://localhost:5000/api/employees \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com","role":"Manager","department":"Sales"}'
```

### Run the tests

Stop the running app first (`Ctrl+C`), then:

```bash
npm test
```

---

## Practical 3 — Java Build

### Identify the language

```bash
ls employee-app/backend-java/
```

You see `pom.xml` → this is a Java project, build tool is Maven.

### Read the dependency file

```bash
cat employee-app/backend-java/pom.xml
```

**Runtime dependencies:**

| Dependency | Purpose |
|------------|---------|
| `spring-boot-starter-web` | Embedded Tomcat web server + HTTP routing |
| `spring-boot-starter-data-jpa` | ORM — write Java instead of raw SQL |
| `postgresql` | PostgreSQL JDBC driver |

**Test-only dependencies:**

| Dependency | Purpose |
|------------|---------|
| `h2` | In-memory database for tests — no Postgres needed |
| `spring-boot-starter-test` | JUnit 5 + MockMvc test framework |

### Compile and package

```bash
cd employee-app/backend-java
mvn package -DskipTests
```

Watch the output. Maven will:
1. Download all dependencies on the first run (cached after that)
2. Compile `.java` source files into `.class` bytecode
3. Bundle everything into a JAR file

After it completes:

```bash
ls target/
```

You will see `employee-backend-1.0.0.jar` — this is the build artifact. This file contains your compiled code and all dependencies. It is self-contained and can run on any machine with Java installed.

### Run the application

```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/employees \
DB_USER=postgres \
DB_PASS=postgres \
java -jar target/employee-backend-1.0.0.jar
```

### Verify it is running

```bash
curl http://localhost:5000/api/health
```

```json
{"status": "healthy", "db": "connected"}
```

```bash
curl http://localhost:5000/api/employees
```

```json
[]
```

### Add an employee

```bash
curl -X POST http://localhost:5000/api/employees \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice Brown","email":"alice@example.com","role":"Director","department":"Engineering"}'
```

### Run the tests

Stop the running app first (`Ctrl+C`), then:

```bash
mvn test
```

Tests use H2 in memory — no Postgres needed.

---

## Build Summary

| | Python | Node.js | Java |
|---|---|---|---|
| Identify by | `requirements.txt` | `package.json` | `pom.xml` |
| Install | `pip install -r requirements.txt` | `npm install` | `mvn compile` |
| Run | `python app.py` | `npm start` | `java -jar target/*.jar` |
| Test | `pytest` | `npm test` | `mvn test` |
| Compile step | No | No | Yes |
| Build artifact | None | None | `target/*.jar` |
| Dependencies stored | site-packages | `node_modules/` | `~/.m2/repository` |
| Test database | SQLite in-memory | PostgreSQL | H2 in-memory |

---

## What Happens if You Skip the Build Step

| Language | Command | Error |
|----------|---------|-------|
| Python | `python app.py` without `pip install` | `ModuleNotFoundError: No module named 'flask'` |
| Node.js | `npm start` without `npm install` | `Error: Cannot find module 'express'` |
| Java | `java -jar ...` without `mvn package` | `Error: Unable to access jarfile target/employee-backend-1.0.0.jar` |

The build step is the gate. Nothing runs without it.

---

## How to Run, Test and Access Each Backend

### Prerequisites — start the database

```bash
docker run -d --name pg \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=employees \
  -p 5432:5432 \
  postgres:15
```

Leave this running for all three backends.

---

### Python

**Run:**
```bash
cd employee-app/backend
pip install -r requirements.txt
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/employees python app.py
```

Windows shortcut:
```bat
run.bat
```

**Test:**
```bash
pytest
```
Tests use SQLite in-memory — no Postgres needed.

**Access:**

| URL | Description |
|-----|-------------|
| `http://localhost:5000/api/health` | Health check |
| `http://localhost:5000/api/employees` | List employees |
| `http://localhost:5000/api/stats` | Stats |
| `http://localhost:5000/metrics` | Prometheus metrics |

---

### Node.js

**Run:**
```bash
cd employee-app/backend-node
npm install
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/employees npm start
```

Windows shortcut:
```bat
run.bat
```

**Test:**
```bash
npm test
```
Tests connect to the real Postgres database — make sure it is running.

**Access:**

| URL | Description |
|-----|-------------|
| `http://localhost:5000/api/health` | Health check |
| `http://localhost:5000/api/employees` | List employees |
| `http://localhost:5000/api/stats` | Stats |

---

### Java

**Run:**
```bash
cd employee-app/backend-java
mvn package -DskipTests
DATABASE_URL=jdbc:postgresql://localhost:5432/employees DB_USER=postgres DB_PASS=postgres java -jar target/employee-backend-1.0.0.jar
```

**Test:**
```bash
mvn test
```
Tests use H2 in-memory — no Postgres needed.

**Access:**

| URL | Description |
|-----|-------------|
| `http://localhost:5000/api/health` | Health check |
| `http://localhost:5000/api/employees` | List employees |
| `http://localhost:5000/api/stats` | Stats |

---

---

## EC2 Deployment — Run the App on Amazon Linux 2023

### Why EC2 for this stage?

Before containers and Kubernetes, applications ran directly on virtual machines. Running the app on EC2 teaches you what Docker and Kubernetes are actually automating — the manual steps of provisioning a server, installing runtimes, managing processes, and opening ports.

On EC2 you run the app directly — no Docker required. Docker is the next phase.

### Recommended Instance

| Setting | Value | Reason |
|---------|-------|--------|
| Instance type | `t3.medium` | 2 vCPU, 4 GB RAM — enough for all three backends + PostgreSQL |
| AMI | Amazon Linux 2023 | AWS-maintained, ships with `dnf`, modern glibc |
| Storage | 20 GB gp3 | Enough for OS, runtimes, Maven local repo (`~/.m2`) |
| Region | `us-east-1` | Matches the rest of the project |

### Security Group

| Port | Source | Purpose |
|------|--------|---------|
| 22 | Your IP | SSH |
| 80 | 0.0.0.0/0 | Nginx (full stack) |
| 5000 | 0.0.0.0/0 | Backend API |
| 8080 | 0.0.0.0/0 | Frontend (Python HTTP server) |

---

### Prerequisites — Install All Dependencies

Once connected to the instance via SSH, run the following.

#### System update

```bash
sudo dnf update -y
```

#### Git

```bash
sudo dnf install -y git
git --version
```

#### Java 17

```bash
sudo dnf install -y java-17-amazon-corretto-headless
java -version
# openjdk version "17.x.x"
```

#### Maven

```bash
sudo dnf install -y maven
mvn -version
# Apache Maven 3.x.x
```

#### Node.js 18

```bash
curl -fsSL https://rpm.nodesource.com/setup_18.x | sudo bash -
sudo dnf install -y nodejs
node --version   # v18.x.x
npm --version    # 9.x.x
```

#### Python 3

```bash
sudo dnf install -y python3 python3-pip
python3 --version   # Python 3.11.x
```

#### PostgreSQL 15

```bash
sudo dnf install -y postgresql15-server postgresql15
sudo postgresql-setup --initdb
sudo systemctl enable --now postgresql

# Allow password authentication
sudo sed -i 's/ident/md5/g' /var/lib/pgsql/data/pg_hba.conf
sudo systemctl restart postgresql

# Create database and user
sudo -u postgres psql <<EOF
CREATE USER postgres WITH PASSWORD 'postgres';
CREATE DATABASE employees OWNER postgres;
GRANT ALL PRIVILEGES ON DATABASE employees TO postgres;
EOF
```

#### Nginx (for full-stack routing)

```bash
sudo dnf install -y nginx
```

---

### Clone the Repository

```bash
cd ~
git clone https://github.com/LandmakTechnology/employee-app.git
cd employee-app/employee-app
```

---

### Run the Python Backend

```bash
cd ~/employee-app/employee-app/backend
pip3 install -r requirements.txt
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/employees python3 app.py
```

**Test:**
```bash
pytest
```

**Access:**
```
http://<EC2_PUBLIC_IP>:5000/api/health
http://<EC2_PUBLIC_IP>:5000/api/employees
```

---

### Run the Node.js Backend

```bash
cd ~/employee-app/employee-app/backend-node
npm install
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/employees npm start
```

**Test:**
```bash
npm test
```

**Access:**
```
http://<EC2_PUBLIC_IP>:5000/api/health
http://<EC2_PUBLIC_IP>:5000/api/employees
```

---

### Run the Java Backend

```bash
cd ~/employee-app/employee-app/backend-java
mvn package -DskipTests
DATABASE_URL=jdbc:postgresql://localhost:5432/employees \
DB_USER=postgres \
DB_PASS=postgres \
java -jar target/employee-backend-1.0.0.jar
```

**Test (H2 in-memory — no Postgres needed):**
```bash
mvn test
```

**Access:**
```
http://<EC2_PUBLIC_IP>:5000/api/health
http://<EC2_PUBLIC_IP>:5000/api/employees
```

---

### Serve the Frontend

```bash
cd ~/employee-app/employee-app/frontend
python3 -m http.server 8080
```

**Access:**
```
http://<EC2_PUBLIC_IP>:8080
```

---

### Full Stack via Nginx (port 80)

To route both frontend and backend through a single URL:

```bash
sudo tee /etc/nginx/conf.d/employee-app.conf > /dev/null <<'EOF'
server {
    listen 80;

    location /api/ {
        proxy_pass http://localhost:5000;
        proxy_set_header Host $host;
    }

    location / {
        root /home/ec2-user/employee-app/employee-app/frontend;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
}
EOF

sudo nginx -t
sudo systemctl enable --now nginx
```

**Access:**
```
http://<EC2_PUBLIC_IP>
```

---

### Access Summary

| URL | Description |
|-----|-------------|
| `http://<EC2_PUBLIC_IP>:5000/api/health` | Backend health check |
| `http://<EC2_PUBLIC_IP>:5000/api/employees` | Employee list |
| `http://<EC2_PUBLIC_IP>:5000/api/stats` | Stats |
| `http://<EC2_PUBLIC_IP>:8080` | Frontend (standalone) |
| `http://<EC2_PUBLIC_IP>` | Full stack via Nginx |

---

## What Comes Next — Docker

Right now the app runs on your machine because you manually installed Python, Node.js, or Java and ran the build steps yourself.

The problem: this only works on your machine. If someone else clones the repo, they need to install the same version of Python/Node.js/Java, run the same build steps, and configure the same environment variables. This is fragile and slow.

**Docker** solves this by packaging the application together with everything it needs — the runtime, the dependencies, the code, the configuration — into a single image. That image runs the same way on any machine, in any environment.

```
Build phase (now):
  source code  →  install dependencies  →  run on your machine

Docker phase (next):
  source code  →  Dockerfile  →  image  →  container that runs anywhere
```

The Dockerfile is the build steps written down as instructions. Instead of you running `pip install` manually, Docker runs it for you inside a container during the image build.

This is how DevOps automates the build phase — the same steps you just ran manually become automated, repeatable, and environment-independent.
