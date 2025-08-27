# Book transfers Demo application

Source code in this repository is to support a Finite State Machine implementation
The idea behind the project is to show how SSM can be used to implement a [Distributed Saga](https://github.com/aphyr/dist-sagas/blob/master/sagas.pdf) 
and orchestrate a business paymentResponse spanning multiple microservices.


# Implementation notes
1. The project is implemented as a multi-module maven project to simplify development (in real life I would expect 
   that each service is implemented by a separate team as a standalone project, or perhaps it is 3rd party services, 
   and BOM to be defined on the organization/tribe level that reflects common versions of the most important
   dependencies. It also can be the case that BOM is not used at all). 

# API

The workflow is set up in the following way:
- earmark your daily paymentResponse limit, if it is successful:
- go for debit leg, if it is successful:
- go for credit leg, if it is successful:
- if it is successful, return the overall result,
- if there is an error on any of the stage, try cancelling previous actions

Value of parameters and overall result:

| limit   | deposit | credit | Result                                                              |
|---------|---------|--------|---------------------------------------------------------------------|
| Monthly | 1312134 | 123222 | OK                                                                  |
| Yearly  | 1312134 | 123222 | NOK, reversal on limit management                                   |
| Monthly | 1312133 | 123222 | NOK, reversal on deposit after the limit is success                 |
| Daily   | 1312134 | 123111 | NOK, reversal on credit after the limit and the deposit are success |

# How to run
## Option 1
- Navigate to the ./docker folder and run > docker-compose up there. It will spin up ActiveMQ, Zipkin and MySQL
- Run eureka service
- Wait a minute before the services will spin up
- Run config-server
- Run api-gateway
- Run deposit-banking-service
- Run limit-management-service
- Run credit-card-banking-service
- Finally, run book-transfers-service

## Option 2
WIP. Please note, the following steps are in WIP phase and not available yet
- TODO: build docker images for all services in the project
- TODO: Navigate to the ./docker folder and run > docker-compose with docker-compose-all.yml up there.
- Wait a couple of minutes until the system will spin up all services 

# Services

## Infrastructural services
| Service name | Port | Comments |
|---|---|---|
| eureka | 8761 | Service Discovery |
| api-gateway | 9090 | API Gateway |
| config-server | 8888 | Externalized configuration |
| Zipkin | 9411 | Distributed tracing |
| ActiveMQ | 8161, 61616 | JMS |
| Mongo | 27017 | | 
| Mongo Express | 8091 | Mongo Admin UI |
| MySQL | 3306 | RDBMS |

## Business Services
| Service name                | Port | Comments                    |
|-----------------------------|---|-----------------------------|
| deposit-banking-service     | 8081 | Deposit Banking Service     |
| limit-management-service    | 8082 | Limit Management Service    |
| credit-card-banking-service | 8083 | Credit Card Banking Service |

## Orchestration
| Service name           | Port | Comments |
|------------------------|---|---|
| book-transfers-service | 8080 | Orchestrator |