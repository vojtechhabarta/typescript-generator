# How to release typescript-generator

- change version in `pom.xml` files and `appveyor.yml` (if not already changed)
- wait for the build
- run "Release to Maven Central" GitHub Action which releases last build
- go to https://oss.sonatype.org and promote the release
    - "Staging Repositories"
    - "Close" the repo
    - wait for closing activities
    - "Release" the repo
- wait for the release to appear in Maven Central - https://repo1.maven.org/maven2/cz/habarta/typescript-generator/
- write release notes
- run "Release to Gradle plugin portal" GitHub Action
- close/update relevant issues and PRs
- remove unused tags
