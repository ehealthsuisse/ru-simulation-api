# Hello Gazelle Simulator 

A starter project to run a Simulator for the Gazelle Test Environment as a Quarkus application. 
The simulator implements the required interfaces to be integrated in the Gazelle Test Environment.
The Hello Gazelle Simulator is for educational purposes only and it's simulated action is to 
LOG the message send by the system under test (SUT) to the console. 

For local testing you may run this application from the terminal or in a docker container together with the
[iua-ru-mock](https://github.com/msmock/iua-ru-mock) application which mocks the reporting and registry interfaces 
of the Gazelle Test Environment. The project also contains a [bruno 4.0.0](https://www.usebruno.com/downloads) 
collection of http transactions to simulate the api calls for test setup and resume and the simulation sequences.  

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

**_NOTE:_**  Quarkus ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Run docker image in bridge network

create network (if not exists):
```
docker network create my_network
```

Compile: 
```
./mvnw clean package -Dmaven.javadoc.skip=true
```

Build the image iua-sim-client:
```
docker build -f src/main/docker/Dockerfile.jvm -t hello-gazelle-simulator .
```

Then run the container:
```
docker run -d --name hello-gazelle-simulator -p 8080:8080 --network my_network hello-gazelle-simulator
```
