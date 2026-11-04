# Library REST API

University of Moratuwa, Faculty of Information Technology  
IN3101 - Enterprise Application Development  
Lab Sheet 02 - Building a REST API with Spring Boot

A small library CRUD API with the foundational project setup normally completed in Lab 01. Books are persisted in local MySQL through Spring Data JPA and Hibernate. The service and controller use constructor injection.

## Technology and prerequisites

- Java 21 JDK (project compiler release: 21).
- Spring Boot 4.1.2, Maven, official Maven Wrapper 3.3.4 (Maven 3.10.0).
- Spring Web MVC, Spring Data JPA/Hibernate, Jakarta Bean Validation.
- MySQL Server on localhost:3306 and MySQL Connector/J.
- springdoc-openapi-starter-webmvc-ui 3.1.1.
- JUnit Jupiter and Mockito; tests use a mocked repository/service and real MVC request validation.
- Postman and a browser for the lab evidence.

Boot manages versions of its starters, Hibernate, MySQL driver, validation, JUnit and Mockito; only springdoc is independently pinned. See `pom.xml` and `mvnw.cmd dependency:tree` for resolved versions. Spring Boot 4.1.2 is an official release ([release announcement](https://spring.io/blog/2026/10/23/spring-boot-4-1-2-available-now/)); springdoc 3.x supports Boot 4 ([compatibility documentation](https://springdoc.org/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot)).

## Project structure

```text
.
|-- .gitignore
|-- .gitattributes
|-- .mvn/wrapper/maven-wrapper.properties
|-- mvnw
|-- mvnw.cmd
|-- pom.xml
|-- README.md
|-- database/setup.sql
|-- postman/Library-API.postman_collection.json
`-- src
    |-- main
    |   |-- java/lk/ac/mrt/fit/library_api
    |   |   |-- LibraryApiApplication.java
    |   |   |-- model/Book.java
    |   |   |-- repository/BookRepository.java
    |   |   |-- service/BookService.java
    |   |   `-- controller/BookController.java
    |   `-- resources/application.properties
    `-- test/java/lk/ac/mrt/fit/library_api
        |-- service/BookServiceTests.java
        `-- controller/BookControllerTests.java
```

The wrapper uses the official script-only distribution, so no wrapper JAR is needed. Its Windows cache-path handling includes a small null-target guard for ordinary directories on Windows PowerShell.

## Database setup

Install/configure MySQL yourself if necessary, and start its server before starting the application. No database is created or deleted automatically by this project. In MySQL Workbench, open and execute `database/setup.sql`, which contains only:

```sql
CREATE DATABASE IF NOT EXISTS library_db;
```

Alternatively, if the MySQL client is on PATH, start it from the project root:

```powershell
mysql -u root -p
```

Enter the password at the client prompt, then run:

```sql
SOURCE database/setup.sql;
```

Hibernate creates or updates the `book` table on application startup with `spring.jpa.hibernate.ddl-auto=update`. SQL output is enabled as required by the lab. Existing database contents are retained.

## Windows PowerShell configuration and commands

Open a terminal in this project root. Check the JDK:

```powershell
java -version
```

Use a Java 21 JDK for the lab. If needed, point the current terminal to your actual installed JDK directory (replace the path):

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

Set the connection details in the same terminal that will run Spring Boot. URL and username below are already the application defaults, so these two assignments are optional:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/library_db'
$env:DB_USERNAME = 'root'
```

Prompt for the password without putting it in source files or terminal history:

```powershell
$databaseSecret = Read-Host 'MySQL password' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $databaseSecret).Password
Remove-Variable databaseSecret
```

`DB_PASSWORD` has no source-code default and must be supplied locally. Spring Boot does not automatically read `.env` files. Do not print the environment password, put it in commands, or include it in screenshots.

Build and test (these isolated tests do not need MySQL or DB_PASSWORD):

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd package
```

Start after MySQL is running and the database exists:

```powershell
.\mvnw.cmd spring-boot:run
```

Alternatively, run the packaged JAR in the configured terminal:

```powershell
java -jar .\target\library-api-0.0.1-SNAPSHOT.jar
```

Stop with Ctrl+C. Remove the password from this terminal when finished:

```powershell
Remove-Item Env:DB_PASSWORD
```

On macOS/Linux, use `sh ./mvnw clean test` and `sh ./mvnw spring-boot:run`, with equivalent environment variables. The first wrapper/build run needs network access to Maven Central. If a network policy blocks the wrapper download, installed Maven is an alternative: `mvn clean test`, `mvn package`, `mvn spring-boot:run`.

## Endpoints and validation

| Method | URL | Success | Other expected status |
| --- | --- | --- | --- |
| GET | `/api/books` | 200, JSON array | Empty library returns `[]` |
| GET | `/api/books/{id}` | 200, book | 404 if absent |
| POST | `/api/books` | 201, created book and generated id | 400 for invalid input |
| PUT | `/api/books/{id}` | 200, updated book | 404 if absent; 400 for invalid input |
| DELETE | `/api/books/{id}` | 204, no body | 404 if absent |

Title and author must be nonblank. ISBN and price are optional; price must be zero or positive when supplied. PUT replaces title, author, ISBN and price; omitted optional fields become null. The path id identifies an update, and a supplied body id cannot change it. POST ignores a supplied id so that a new record is inserted. Validation runs before the service is called.

Example request body (replace YOUR_INDEX_NUMBER):

```json
{
  "title": "YOUR_INDEX_NUMBER - Clean Code",
  "author": "Robert C. Martin",
  "isbn": "9780132350884",
  "price": 4500.00
}
```

PowerShell read-only checks after startup:

```powershell
Invoke-RestMethod -Uri 'http://localhost:8080/api/books' -Method Get
Invoke-RestMethod -Uri 'http://localhost:8080/v3/api-docs' -Method Get
```

## Postman workflow

1. Choose Import in Postman and select `postman/Library-API.postman_collection.json`.
2. Collection variables default to `baseUrl=http://localhost:8080`, `bookId=1`, `deletedId=1`. IDs are generated by MySQL and may differ; set them manually. No script changes variables.
3. Replace YOUR_INDEX_NUMBER in S1 and S4 with your actual index number. Send S1 and capture its 201 response. Set `bookId` to its generated id.
4. Send both extra create requests (Effective Java and The Pragmatic Programmer). There should now be at least three books. Set `deletedId` to the third book's generated id so the index-number book is retained.
5. Send S2 and capture the three records before deletion. Then send S3 and S4, capturing each response.
6. Review `deletedId` before manually sending S5. Send S6 for that same id, then S7 with an empty title.
7. Each request includes a status assertion. Do not run the entire collection blindly: S5 deletes a record. Repeated create requests add more records, and repeated deletion returns 404.

## Swagger / OpenAPI

With the application running, visit:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

All five book operations appear under **Books**. Expand POST `/api/books` and verify **Create a book**, description **Adds a book and returns it with its new id.**, and responses **201 Book created** and **400 Invalid input**. Capture this expanded view for S8. Swagger's Try it out sends real requests to MySQL.

## Automated verification and limits

`BookServiceTests` exercises creation, reads, all update fields, clearing optional values, deletion and missing records. `BookControllerTests` exercises the five HTTP operations, their statuses, generated id output, missing records, blank/missing required values, negative price, optional values, zero price, and malformed JSON. Invalid POST and PUT requests must never call the service.

Tests use no replacement database. They verify isolated Java/MVC behavior; a successful test build does not prove MySQL connectivity, persistence, or live Swagger rendering. Verify those after completing the local database setup. No test starts the full application or changes a real database.

### Verification in this workspace

- `java -version` and `mvn -version`: Java 25.0.1 and installed Maven 3.9.11 were found. A Java 21 runtime was not found; builds used JDK 25 with compiler release 21. `javap` confirmed class-file major version 65 (Java 21).
- Official Initializr project downloaded successfully with network access. Maven Wrapper 3.3.4 launches Maven 3.10.0 successfully after the Windows null-target guard.
- `mvnw.cmd clean test`: 20 tests passed. Review then corrected a whitespace-only JSON test input; `mvnw.cmd package` reran all 20 tests successfully, with zero failures/errors/skips, and created the executable JAR.
- The build commands used `MAVEN_USER_HOME=C:\Users\USER\.m2` and `-Dmaven.repo.local=C:\Users\USER\.m2\repository` to access the existing user cache in this environment.
- `mvnw.cmd dependency:tree` resolved Hibernate ORM 7.4.13.Final, Hibernate Validator 9.1.5.Final, Spring MVC 7.0.10, MySQL Connector/J 9.7.0, JUnit Jupiter 6.0.3, and Mockito 5.23.0.
- Maven XML, Postman JSON structure, all nine requests, request-body JSON, and fixed collection variables were checked locally.
- No MySQL service or client was discovered. No application startup or real database integration test was attempted; MySQL persistence and live Swagger UI remain unverified.
- No Git commands were executed. No credentials, screenshots, or submission PDF were generated.

## Submission checklist

Submit **one PDF** containing your **full name**, **actual index number**, **GitHub repository link**, and these eight labeled screenshots:

| Label | Required evidence |
| --- | --- |
| S1 | POST `/api/books`, 201 Created, request JSON and response with generated id |
| S2 | GET `/api/books`, 200 OK, at least three books; one title contains your actual index number |
| S3 | GET `/api/books/{id}`, 200 OK |
| S4 | PUT `/api/books/{id}`, 200 OK; changed request JSON and updated response values |
| S5 | DELETE `/api/books/{id}`, 204 No Content, empty response body |
| S6 | GET the exact id deleted in S5, 404 Not Found |
| S7 | POST with `"title": ""`, 400 Bad Request |
| S8 | Swagger UI with POST expanded, its summary and documented 201/400 responses visible |

Capture full Postman windows for S1-S7: method, URL, request body and response status must be visible as applicable. For S2 ensure all three records can be seen. Take real screenshots after running the application. Replace the index placeholder before capturing evidence. The PDF filename must be `<INDEX_NUMBER>_IN3101_Lab02.pdf`. No fictional screenshots or submission PDF are generated by this project.

## Troubleshooting

- **Java not found / invalid target release 21:** install a Java 21 JDK manually and set this terminal's JAVA_HOME/PATH. No global settings need to change.
- **Wrapper download/dependency failure:** check network/proxy access to Maven Central; try installed Maven as shown above. Do not substitute unrelated dependency versions.
- **Local Maven repository resolves to an unwritable path:** set `$env:MAVEN_USER_HOME = "$env:USERPROFILE\.m2"` for the wrapper. You can explicitly pass `"-Dmaven.repo.local=$env:USERPROFILE\.m2\repository"` to a Maven command if necessary.
- **Could not resolve DB_PASSWORD:** set it in the terminal used to start the app.
- **Communications link failure / connection refused:** start MySQL and check host/port in DB_URL.
- **Unknown database library_db:** execute `database/setup.sql` manually.
- **Access denied:** verify your local username/password and schema privileges. Do not share credentials in logs.
- **Port 8080 already in use:** stop the conflicting application or set `$env:SERVER_PORT = '8081'` and update Postman's baseUrl and browser URLs.
- **400 response:** check JSON syntax, Content-Type, nonblank title/author and nonnegative price. Error-body details can differ; the required status is 400.
- **404 response:** use the actual generated id from POST; MySQL IDs need not start at 1 or be consecutive.
- **Swagger page unavailable:** first ensure application startup succeeded with MySQL, then visit `/swagger-ui.html` (which redirects to the UI).

## Manual Git and GitHub steps

Git initialization, staging, commits, remotes and pushes are left entirely to you. Review files and ensure credentials/build output are excluded before you publish. When ready, create an empty repository in your own GitHub account, then run the following yourself from this root (skip initialization/remote creation if already configured; replace the URL):

```powershell
git init
git add .
git commit -m "Complete IN3101 Lab 02 Library REST API"
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPOSITORY.git
git push -u origin HEAD
```

Use your actual repository URL in the PDF. No Git commands are run by the implementation workflow.
