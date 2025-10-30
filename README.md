# Gnd Ui Lobid

This app was created with Bootify.io, see https://bootify.io/app/ZEPRF7AQQFJF, - tips on working with the code [can be found here](https://bootify.io/next-steps/).

## Development

Run all checks:

```
./gradlew check --info
```

### Webpack DevServer

In addition to the Spring Boot application, the DevServer must also be started - for this [Node.js](https://nodejs.org/) version 22 is required. On first usage and after updates the dependencies have to be installed:

```
npm install
```

The DevServer can be started as follows:

```
npm run devserver
```

Using a proxy the whole application is now accessible under `localhost:8081`. All changes to the templates and JS/CSS files are immediately visible in the browser.

### Boot application

Initial setup, only once: make `./gradlew` executable:

```
chmod u+x ./gradlew
```

Continuously build the Jar, for hot-deployment of the Java/Boot application:

```
./gradlew --continuous bootJar
```

In another terminal, run the application:

```
./gradlew bootRun
```

## Build

The application can be built using the following command:

```
gradlew clean build
```

Node.js is automatically downloaded using the `gradle-node-plugin` and the final JS/CSS files are integrated into the jar.

Start your application with the following command - here with the profile `production`:

(During development it is recommended to use the profile `local`. In IntelliJ `-Dspring.profiles.active=local` can be added in the VM options of the Run Configuration after enabling this property in "Modify options". Create your own `application-local.yml` file to override settings for development.)

```
java -Dspring.profiles.active=production -jar ./build/libs/gnd-ui-lobid-0.0.1-SNAPSHOT.jar
```

If required, a Docker image can be created with the Spring Boot plugin. Add `SPRING_PROFILES_ACTIVE=production` as environment variable when running the container.

```
gradlew bootBuildImage --imageName=org.lobid/gnd-ui-lobid
```

## Further readings

* [Gradle user manual](https://docs.gradle.org/)  
* [Spring Boot reference](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)  
* [Thymeleaf docs](https://www.thymeleaf.org/documentation.html)  
* [Webpack concepts](https://webpack.js.org/concepts/)  
* [npm docs](https://docs.npmjs.com/)  
* [Bootstrap docs](https://getbootstrap.com/docs/5.3/getting-started/introduction/)  
* [Learn Spring Boot with Thymeleaf](https://www.wimdeblauwe.com/books/taming-thymeleaf/)  
