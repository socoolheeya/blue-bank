# Centralize Gradle Wrapper Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep one Gradle 9.6.1 wrapper at the repository root and build every application and data module through that wrapper.

**Architecture:** The existing root `settings.gradle.kts` remains the sole multi-project registry. Module-local build scripts remain intact, while duplicated wrapper launchers, properties, and JARs are removed from every child module.

**Tech Stack:** Gradle 9.6.1, Gradle Wrapper, Kotlin DSL, Kotlin/JVM, Spring Boot

## Global Constraints

- The only wrapper version is Gradle 9.6.1.
- Preserve all unrelated uncommitted application and build-script changes.
- Keep every module registered in the root `settings.gradle.kts`.
- The full project must compile and its tests must pass before completion.

---

### Task 1: Establish the centralized-wrapper structural check

**Files:**
- Verify: `gradle/wrapper/gradle-wrapper.properties`
- Verify absence: `app/*/gradlew`, `app/*/gradlew.bat`, `app/*/gradle/wrapper/*`
- Verify absence: `data/*/gradlew`, `data/*/gradlew.bat`, `data/*/gradle/wrapper/*`

**Interfaces:**
- Consumes: Current repository file layout
- Produces: A repeatable shell assertion proving the root wrapper is 9.6.1 and no child wrapper exists

- [ ] **Step 1: Run the structural assertion before changing files**

```bash
test "$(rg -l 'gradle-9\.6\.1-bin\.zip' gradle/wrapper/gradle-wrapper.properties | wc -l | tr -d ' ')" = "1" && test -z "$(find app data -mindepth 2 -type f \( -name gradlew -o -name gradlew.bat -o -name gradle-wrapper.jar -o -name gradle-wrapper.properties \) -print)"
```

Expected: FAIL because child module wrappers still exist.

### Task 2: Remove child wrappers and regenerate the root wrapper

**Files:**
- Modify: `gradle/wrapper/gradle-wrapper.properties`
- Modify: `gradle/wrapper/gradle-wrapper.jar`
- Modify: `gradlew`
- Modify: `gradlew.bat`
- Delete: `app/*/gradlew`, `app/*/gradlew.bat`, `app/*/gradle/wrapper/gradle-wrapper.jar`, `app/*/gradle/wrapper/gradle-wrapper.properties`
- Delete: `data/*/gradlew`, `data/*/gradlew.bat`, `data/*/gradle/wrapper/gradle-wrapper.jar`, `data/*/gradle/wrapper/gradle-wrapper.properties`

**Interfaces:**
- Consumes: Root Gradle build and the Gradle 9.6.1 distribution
- Produces: Root-only `./gradlew` launcher and project-path module builds

- [ ] **Step 1: Generate the complete root Gradle 9.6.1 wrapper**

```bash
./gradlew wrapper --gradle-version 9.6.1 --distribution-type bin
./gradlew wrapper --gradle-version 9.6.1 --distribution-type bin
```

Expected: Both invocations end with `BUILD SUCCESSFUL` and the second invocation refreshes scripts/JAR using Gradle 9.6.1.

- [ ] **Step 2: Remove only child wrapper files**

Use a repository patch to delete the four wrapper files from each child module. Do not delete module `build.gradle.kts`, source code, or `.gitignore` files.

- [ ] **Step 3: Re-run the structural assertion**

```bash
test "$(rg -l 'gradle-9\.6\.1-bin\.zip' gradle/wrapper/gradle-wrapper.properties | wc -l | tr -d ' ')" = "1" && test -z "$(find app data -mindepth 2 -type f \( -name gradlew -o -name gradlew.bat -o -name gradle-wrapper.jar -o -name gradle-wrapper.properties \) -print)"
```

Expected: PASS with exit code 0.

### Task 3: Verify all modules through the root wrapper

**Files:**
- Verify: `settings.gradle.kts`
- Verify: all `app/*/build.gradle.kts`
- Verify: all `data/*/build.gradle.kts`

**Interfaces:**
- Consumes: Root-only Gradle 9.6.1 wrapper
- Produces: Fresh evidence that the multi-project build compiles and tests successfully

- [ ] **Step 1: Verify the running Gradle version**

```bash
./gradlew --version
```

Expected: Output contains `Gradle 9.6.1`.

- [ ] **Step 2: Verify module registration and task addressing**

```bash
./gradlew projects
```

Expected: All `:app:*` and `:data:*` projects, including `:app:batch`, are listed.

- [ ] **Step 3: Compile and test the complete project**

```bash
./gradlew build
```

Expected: `BUILD SUCCESSFUL` with no compilation or test failures.

- [ ] **Step 4: Inspect the final change scope**

```bash
git diff --check
git status --short
```

Expected: No whitespace errors; child wrapper deletions and root wrapper updates are present while unrelated user changes remain intact.
