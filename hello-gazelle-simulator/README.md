# Hello Gazelle Simulator 

A starter project to run a Simulator for the Gazelle Test Environment as a Quarkus application. 
The simulator implements the required interfaces to be integrated in the Gazelle Test Environment.
The Hello Gazelle Simulator is for educational purposes only and it's simulated action is to 
LOG the message send by the system under test (SUT) to the console. 

For local testing you may run this application from the terminal or in a docker container together with the
[iua-ru-mock](https://github.com/msmock/iua-ru-mock) application which mocks the reporting and registry interfaces 
of the Gazelle Test Environment. The project also contains a [bruno 4.0.0](https://www.usebruno.com/downloads) 
collection of http transactions to simulate the api calls for test setup and resume and the simulation sequences.  

## How the simulator is put together

A simulator implements two business interfaces of the Gazelle simulation framework, both in
`HelloGazelleSimulationService`:

- `SimulationSequenceService` -- the sequences the simulator offers, with their parameters;
- `SimulationService` -- `setup` prepares a session for a request, `runSimulation` performs it and
  hands the report to the `SimulationCallback` it is given.

The Simulation Service API itself comes from the framework: the REST endpoints and their status
codes, the sequences checksum, the session ids, posting the report to the callback URL and
enforcing the timeout requested at setup. `HelloGazelleSimulatorAPI` extends `SimulationController`
for that, `HelloGazelleSimulationManagerFactory` connects the service to the framework's
`SimulationManager`, and `HelloGazelleMetadataService` names the service in the reports.

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
