# lobid-gnd-ui

This is a prototype for a lobid-gnd UI implementation based on Spring Boot. It builds with Gradle and uses Thymeleaf, Bootstrap, and Webpack for the frontend. It is based on the reactive Spring WebFlux stack, using the functional/HTTP-oriented WebFlux.fn style. It fetches JSON data from the lobid-gnd API.

[![Build](https://github.com/hbz/lobid-gnd-ui/workflows/Build/badge.svg)](https://github.com/hbz/lobid-gnd-ui/actions?query=workflow%3ABuild)

## Prerequisites

- Git, install with e.g.: `sudo apt install git-all`
- Java 21, install with e.g.: `sudo apt install openjdk-21-jdk`
- Get the code repo: `git clone https://github.com/hbz/lobid-gnd-ui.git`
- Change into the repo directory: `cd lobid-gnd-ui`
- Make the Gradle wrapper executable: `chmod u+x ./gradlew`

## Start server

To start the local server, run:

```bash
./gradlew bootRun
```

The server should be running at http://localhost:8080/gnd.

## Run checks

To run all automated tests, compiler and formatter checks:

```
./gradlew check
```

The tests use JUnit Jupiter with AssertJ and [HtmlUnit](https://www.htmlunit.org/gettingStarted.html). The Gradle build is configured to fail on Java compiler errors. The formatter checks are based on [Spotless](https://github.com/diffplug/spotless/tree/main/plugin-gradle), which is configured in the Gradle build file (`build.gradle`).

The `check` task runs in our CI setup, so any new code must pass those checks (i.e. when contributing new code make sure `./gradlew check` is successful, if required apply all Spotless formatter rules with `./gradlew spotlessApply`).

## Backend development

For local development, we want to be able to test code changes without restarting the server.

For backend hot-deployment, in a separate terminal, run:

```
./gradlew --continuous bootJar
```

With this, changes to the Java backend code are visible at http://localhost:8080/gnd *after* a browser reload.

## Frontend development

To see changes to the HTML/JS/CSS frontend code *without* a browser reload, we can run a WebPack [DevServer](https://github.com/webpack/webpack-dev-server) in another terminal. Prerequisite: Node.js 22 (install e.g. via [NVM](https://github.com/nvm-sh/nvm?tab=readme-ov-file#installing-and-updating): `nvm install 22 ; nvm use 22`)

Install dependencies (initially, and after changes to `package.json`):

```
npm install
```

Start the DevServer:

```
npm run devserver
```

The application with live reloading is now accessible at http://localhost:8081/gnd.

## Production deployment

Build the application:

```
./gradlew clean build
```

Start the application with the `production` profile:

```
java -Dspring.profiles.active=production -jar ./build/libs/lobid-gnd-ui-0.0.1-SNAPSHOT.jar
```

The server should be running at http://localhost:8080/gnd.

## Reference manuals

- [WebFlux](https://docs.spring.io/spring-boot/reference/web/reactive.html)
- [Thymeleaf](https://www.thymeleaf.org/documentation.html)
- [Bootstrap](https://getbootstrap.com/docs/5.3/getting-started/introduction/)
- [Webpack](https://webpack.js.org/concepts/)
- [HtmlUnit](https://www.htmlunit.org/gettingStarted.html)
- [Spotless](https://github.com/diffplug/spotless/tree/main/plugin-gradle)

