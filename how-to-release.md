# How to release typescript-generator

## Maven artifacts

- Run the **"Release to Maven Central"** GitHub Action (`release.yml`):
   - Go to **Actions** → **Release to Maven Central** → **Run workflow**
   - Enter the version to release (e.g. `4.1.0`)
   - The workflow builds, signs, publishes to Maven Central, and tags the commit automatically
   - Wait for the workflow to finish — it waits until the artifacts are fully available in Central

- Verify the release appeared in Maven Central:
   - https://repo1.maven.org/maven2/cz/habarta/typescript-generator/

## Gradle plugin

- Run the **"Release to Gradle plugin portal"** GitHub Action (`release-gradle-plugin.yml`):
   - Go to **Actions** → **Release to Gradle plugin portal** → **Run workflow**
   - Enter the same version number

## After release

- Write release notes on GitHub (create a release from the tag `vX.Y.Z`)
- Close or update relevant issues and PRs
