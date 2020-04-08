# CRM

## Development

### Database - PostgreSQL

A docker container is provided to start using postgresql:

    docker-compose -f src/main/docker/postgresql.yml up -d

To stop it and remove the container, run:

    docker-compose -f src/main/docker/postgresql.yml down

You can also fully dockerize your application and all the services that it depends on.
To achieve this, first build a docker image of your app by running:

    ./mvnw -Pprod verify jib:dockerBuild

Then run:

    docker-compose -f src/main/docker/app.yml up -d

### OAuth 2.0 / OpenID Connect

This application is secured with OAuth 2.0 and uses [Keycloak](https://keycloak.org) as the authentication provider. You can start start Keycloak using the following command:

docker-compose -f src/main/docker/keycloak.yml up

The security settings in src/main/resources/config/application.yml are configured for this image.

yaml
spring:
...
security:
oauth2:
client:
provider:
oidc:
issuer-uri: http://localhost:9080/auth/realms/jhipster
registration:
oidc:
client-id: web_app
client-secret: web_app

### Storage - Minio

Minio es the object storage server used to store all the customers images. A docker container is provided to install it locally:

docker-compose -f src/main/docker/minio.yml up

Note: It is important that, once you have created the container, you access to the Minio server (credentials can be found in the docker-compose file) and add the `Read Only` policy to the bucket `customersimages`, otherwise files won't be available.

## Testing

To launch the application's tests, run:

    ./mvnw verify

### Client tests

Unit tests are run by [Jest][] and written with [Jasmine][]. They're located in [src/test/javascript/](src/test/javascript/) and can be run with:

    npm test

For more information, refer to the [Running tests page][].

### Code quality

Sonar is used to analyse code quality. You can start a local Sonar server (accessible on http://localhost:9001) with:

docker-compose -f src/main/docker/sonar.yml up -d

You can run a Sonar analysis with using the [sonar-scanner](https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner) or by using the maven plugin.

Then, run a Sonar analysis:

./mvnw -Pprod clean verify sonar:sonar

If you need to re-run the Sonar phase, please be sure to specify at least the initialize phase since Sonar properties are loaded from the sonar-project.properties file.

./mvnw initialize sonar:sonar

or

For more information, refer to the [Code quality page][].

## Building for production

### Packaging as war

To package your application as a war in order to deploy it to an application server, run:

## Swagger UI

In order to access swagger-ui you will need to have Node.js installed.

Navigate to `src/main/webapp` and execute the following commands:

```
npm install
npm start
```

Once the server has started, you can access swagger-ui by signing in and choosing the `API` option in the `Administration` tab.
