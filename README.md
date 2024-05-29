# mt-ethereum-product-service

## Setup

1. `mkdir src/main/resources`
1. `cp Product.sol src/main/resources/`
1. `mkdir src/test/java/at/schunker/mt/ethereumproductservice`
1. `mkdir src/main/java/at/schunker/mt/ethereumproductservice`
1. `mv src/test/java/at/schunker/mt/AppTest.java src/test/java/at/schunker/mt/ethereumproductservice/`
1. `mv src/main/java/at/schunker/mt/App.java src/main/java/at/schunker/mt/ethereumproductservice/`

## Generate Sources

1. Place the Solidity files (.sol) in `/src/main/resources`
2. Execute `mvn web3j:generate-sources`

## Compile

1. `mvn clean package [-P<profile-name>]`

## Run

1. `java -jar target/mt-ethereum-product-service-1.0-SNAPSHOT.jar`

## Build and Run Container

1. `docker build -t ps/product-service .`
2. `docker run --name product-service -p 443:443 ps/product-service`

## Push Container

1. `docker tag ps/product-service <registry-name>/ps/product-service:latest`
2. `docker login <registry-name>`
3. `docker push <registry-name>/ps/product-service:latest`
4. `docker logout <registry-name>`

## Start and Stop Container

1. `docker start product-service`
2. `docker stop product-service`

## Create Certificate

`keytool -genkeypair -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore product-service.p12 -validity 730`

## Dependencies

`mvn -X dependency:tree`
`mvn dependency:tree | grep okhttp`

## Configuration

### Config Ordering

From highest to lowest priority

1. Command line arguments
1. Java System properties (`System.getProperties()`)
1. OS environment variables (`System.getenv()`)
1. `@PropertySource` annotations on your `@Configuration` classes
1. Application properties outside of your packaged jar (`application.yml` including YAML and profile variants)
1. Application properties packaged inside your jar (`application.yml` including YAML and profile variants)
1. Default properties (specified using `SpringApplication.setDefaultProperties`)

Override via command line switch: `java -jar app.jar --server.port=8443`

#### Application property file configuration

The SpringApplication will load properties from files named as application.properties or application.yml in the following locations:

1. A `/config` subdir of the current directory
1. The current directory
1. A classpath `/config` package
1. The classpath root

### Profiles

1. Environment variable: `export spring_profiles_active=dev`
2. JVM System Parameter: `java -jar app.jar -Dspring.profiles.active=dev`

## Formatting

### Check Format
`mvn prettier:check`

### Rewrite Format
`mvn prettier:write`

## MongoDB Database

##### Setup MongoDB container
```
docker run -dt --name product-service-mongodb -e MONGO_INITDB_DATABASE=walletdb -e MONGO_INITDB_ROOT_USERNAME=mongoadmin -e MONGO_INITDB_ROOT_PASSWORD=mongosecret -p 27017:27017 mongo:latest
```

## Start and Stop MongoDB Container

1. `docker start product-service-mongodb`
2. `docker stop product-service-mongodb`

##### Execute bash in container
```
docker exec -it product-service-mongodb bash
```

##### Connect to the MongoDB instance
```
mongo --port 27017
```

##### Switch to admin database
`use admin`

##### Show users
`db.getUsers();` or `show users`

##### Create user administrator
```
use admin
db.createUser(
  {
    user: "myUserAdmin",
    pwd: passwordPrompt(), // or cleartext password
    roles: [ { role: "userAdminAnyDatabase", db: "admin" }, "readWriteAnyDatabase" ]
  }
)
```

##### Restart MongoDB with access control
```
mongod --auth --port 27017 --dbpath /var/lib/mongodb
```

##### Connect and authenticate as user administrator
```
mongo --authenticationDatabase "admin" -u "mongoadmin" -p "mongosecret"
```

##### Create additional users
```
use walletdb

db.createUser(
  {
    user: "tester",
    pwd:  passwordPrompt(),   // or cleartext password
    roles: [ { role: "readWrite", db: "walletdb" },
             { role: "read", db: "test" } ]
  }
)
```

## API Examples
```
curl -iSs http://localhost:8080/productid
curl -iSs http://localhost:8080/product/1
curl -iSs http://localhost:8080/product/2
curl -iSs "http://localhost:8080/product?id=1&name=Cheese"
curl -iSs "http://localhost:8080/product?id=2&name=Wine"
```
