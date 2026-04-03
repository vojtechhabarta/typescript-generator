# How to release typescript-generator

## Maven artifacts

1. Run the **"Release to Maven Central"** GitHub Action (`release.yml`):
   - Go to **Actions** → **Release to Maven Central** → **Run workflow**
   - Enter the version to release (e.g. `4.1.0`)
   - The workflow will set the version, build, sign, deploy to OSSRH staging, and tag the commit

2. Promote the release on OSSRH:
   - Go to https://oss.sonatype.org → **Staging Repositories**
   - Select the repository, click **Close** and wait for the closing activities to finish
   - Click **Release**

3. Wait for the release to appear in Maven Central:
   - https://repo1.maven.org/maven2/cz/habarta/typescript-generator/

## Gradle plugin

4. Run the **"Release to Gradle plugin portal"** GitHub Action (`release-gradle-plugin.yml`):
   - Go to **Actions** → **Release to Gradle plugin portal** → **Run workflow**
   - Enter the same version number

## After release

5. Write release notes on GitHub (create a release from the tag `vX.Y.Z`)
6. Close or update relevant issues and PRs
