# AGENTS.md – TypeScript Generator Developer Guide

This file describes the project architecture, module layout, technologies, and workflow conventions for AI agents and human contributors working on this repository.

---

## Project Overview

**typescript-generator** is a Java tool that generates TypeScript definition files (`.d.ts`) from Java classes that are serialized as JSON. It works by analyzing compiled Java bytecode (via reflection/ClassGraph), building an internal model, and emitting TypeScript interfaces and types. It supports multiple JSON libraries (Jackson 2 & 3, Gson, JSON-B) and multiple REST frameworks (JAX-RS, Spring Web MVC).

The tool is distributed in two forms:
- **Maven plugin** – integrated into a Maven build via the `generate` goal.
- **Gradle plugin** – integrated into a Gradle build via the `generateTypeScript` task.

---

## Base Technologies

| Technology | Version | Purpose |
|---|---|---|
| Java | 17 | Language and compilation target |
| Maven | 3.9+ | Primary build tool and plugin distribution |
| Gradle | 8.x (wrapper) | Alternative build tool and plugin distribution |
| Jackson | 2.x / 3.x | JSON serialization library support (both versions) |
| Spring Boot | 4.x | Spring REST framework support |
| Jakarta APIs | 4.x+ | JAX-RS, JSON-B, JAXB support |
| JUnit | 5 (Jupiter) | Testing framework |
| Kotlin | 2.x | JVM interop and Kotlin-class generation support |
| Spotless | 3.x | Code formatting (Eclipse formatter) |

---

## Repository Layout

```
typescript-generator/
├── typescript-generator-core/          # Core generation engine (JAR)
├── typescript-generator-maven-plugin/  # Maven plugin wrapper
├── typescript-generator-gradle-plugin/ # Gradle plugin wrapper
├── typescript-generator-spring/        # Spring framework support (JAR)
├── sample-maven/                       # Example: Maven + Jackson
├── sample-gradle/                      # Example: Gradle + Jackson
├── sample-maven-spring/                # Example: Maven + Spring
├── sample-gradle-spring/               # Example: Gradle + Spring
├── build/                              # Release scripts and docs
├── .github/workflows/                  # GitHub Actions CI/CD
├── eclipse-formatter-typescript-generator.xml  # Code style config
├── pom.xml                             # Parent Maven POM
└── README.md                           # User-facing documentation
```

---

## Module Descriptions

### `typescript-generator-core`

The central engine. Contains:
- **`TypeScriptGenerator`** – main public API entry point.
- **`Settings`** – all configuration parameters.
- **`parser/`** – library-specific model parsers (`Jackson2Parser`, `Jackson3Parser`, `GsonParser`, `JsonbParser`, `RestApplicationParser`).
- **`compiler/`** – transforms the Java model into a TypeScript model (`TsModel`).
- **`emitter/`** – renders the TypeScript model as `.d.ts` text, `info.json`, or `package.json`.
- **`ext/`** – extension / customisation hooks.
- **`type/`** – Java type abstractions for generics, wildcards, and nullability.

### `typescript-generator-maven-plugin`

Thin Maven Mojo wrapper around the core. Accepts configuration as XML inside `<configuration>`. The main goal is `generate`, bound to the `process-classes` lifecycle phase.

### `typescript-generator-gradle-plugin`

Gradle plugin (ID: `cz.habarta.typescript-generator`). Exposes the `generateTypeScript` task. Supports both Groovy and Kotlin DSLs.

### `typescript-generator-spring`

Optional add-on module that adds support for generating typed HTTP clients from Spring `@RestController` / `@RequestMapping` annotations. Depends on `typescript-generator-core`.

---

## Three-Stage Architecture

```
ModelParser  →  ModelCompiler  →  Emitter
     ↓               ↓
TypeProcessor   TypeProcessor
```

1. **ModelParser** – Reads compiled Java classes via reflection/ClassGraph. Produces a library-agnostic `Model`. `TypeProcessor` implementations discover additional dependent types (e.g., following `List<Person>` to include `Person`).
2. **ModelCompiler** – Maps Java types to TypeScript types (`int` → `number`, `List<T>` → `T[]`, etc.). Resolves generics. Produces a `TsModel`.
3. **Emitter** – Serialises the `TsModel` to `.d.ts` output (or other output formats).

All three stages are pluggable via `TypeProcessor` implementations.

---

## Building the Project

The project is built with **Maven**. The parent POM is at the repository root. **Always run Maven commands from the repository root** unless you are working exclusively inside the Gradle plugin.

```bash
# Build everything and run all tests
mvn clean install

# Build without running tests
mvn clean install -DskipTests

# Build a single module (e.g. core only)
mvn -pl typescript-generator-core clean install
```

For the Gradle plugin specifically:

```bash
cd typescript-generator-gradle-plugin
./gradlew build
./gradlew test
```

---

## Running Tests

```bash
# All tests (Maven modules)
mvn test

# Tests in a specific module
mvn -pl typescript-generator-core test

# A specific test class
mvn test -Dtest=Jackson2ParserTest
```

Test classes live under `src/test/java/` inside each module. The project uses JUnit 5 (Jupiter).

---

## ⚠️ Code Formatting – IMPORTANT

The project enforces code style via **Spotless** (Eclipse formatter). **The CI build will fail if code is not properly formatted.**

Before committing or opening a pull request, always run:

```bash
mvn spotless:apply
```

Run this command from the **repository root**. Running it inside a single module subdirectory does not apply the formatting correctly across all modules.

To verify formatting without modifying files:

```bash
mvn spotless:check
```

The formatter configuration is in `eclipse-formatter-typescript-generator.xml`. Import this file into your IDE (IntelliJ IDEA or Eclipse) to keep formatting consistent while you edit.

Import ordering convention:
- Normal imports come first (alphabetical).
- Static imports come last.
- Wildcard and module imports are **forbidden**.

---

## Dependency Policy

Do **not** add new dependencies without first opening an issue for discussion. The project deliberately keeps its dependency footprint small.

---

## Pull Request Guidelines

- Keep PRs small and focused on a single concern.
- Run `mvn spotless:apply` before pushing (see above).
- Make sure `mvn clean install` passes locally before opening a PR.
- Do not remove or modify existing tests unless the test itself is the subject of the change.
- The project targets Java 17 as the compilation baseline.

---

## CI / CD

| Workflow | Trigger | Purpose |
|---|---|---|
| Appveyor | Every push / PR | Runs `mvn clean install spotless:check` – **must pass before merging** |
| `release.yml` | Manual dispatch | Builds, signs, and uploads to Maven Central |
| `release-gradle-plugin.yml` | Manual dispatch | Publishes Gradle plugin to Gradle Plugin Portal |

Releases are signed with GPG. Credentials are stored as GitHub repository secrets.

---
