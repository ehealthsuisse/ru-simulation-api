# IUA Authorization Server Mock

Mocks an CH:IUA Authorization Server actor implementing a Get Access Token [ITI-71] transaction for the client credential
and authorization code flow as specified in the [Swiss extension of the IUA profile](http://build.fhir.org/ig/ehealthsuisse/ch-epr-fhir/). Its purpose is to handle the 
required parameter of the Get Access Token [ITI-71] transaction for the client credential and authorization code 
flow as specified in the [Swiss extension of the IUA profile](http://build.fhir.org/ig/ehealthsuisse/ch-epr-fhir/).

The resulting JWT token is signed with a RSA key stored in the _resources_ folder.

This mock comes with the following simplifications: 
- It has no connection to an application management and accepts any client id and client secret.
- It has no connection to user management and accepts any user.

To avoid any key exchange problems, the implementation: 
- Does not verify the http signature of the token request.
- Does not verify the signature of the IdP token sent with the token request in the authorization code flow.

The implementation comes with the following limitations:
- It does not parse the IdP token for the _user_name_ or _user_id_. It uses fixed values instead.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.


## Run docker image in bridge network

create network (if not exists):
```
docker network create my_network
```

compile:
```
./mvnw clean package -Dmaven.javadoc.skip=true
```

build image:
```
docker build -t iua-sim-serv .
```

run container:
```
docker run -d --name iua-sim-serv -p 9000:9000 --network my_network iua-sim-serv 
```