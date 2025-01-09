# Introduction to Kafka with Spring Boot - Tracking Service

This repository contains the code to support the [Introduction to Kafka with Spring Boot](https://www.udemy.com/course/introduction-to-kafka-with-spring-boot/?referralCode=15118530CA63AD1AF16D) online course, for the Tracking Service portion of the course.

The associated repository for the Dispatch Service can be found here:  [Dispatch Service Repository](https://github.com/dboeckli/dispatch)

The application code is for a message driven service which utilises Kafka and Spring Boot 3.

This application can be tested in two way:
1. Setting up local Kafka in Wsl (See Kafka.md file) and use the IntelliJ runner
2. Use IntelliJ runner with docker profile which will start a docker Kafka instance via docker compose

Send Message:
For that you need a kafka cli environment which will be available when you have done the kafka wsl setup

use at home:
```
cd ~/tools/kafka/kafka_2.13-3.9.0
```
use at work:
```
cd /opt/development/tools/kafka/kafka_2.13-3.9.0
```

When started with docker profile use:
```
bin/kafka-topics.sh --bootstrap-server localhost:29092 --list
bin/kafka-topics.sh --bootstrap-server 127.0.0.1:29092 --list
bin/kafka-topics.sh --bootstrap-server [::1]:29092 --list
```

Send a message to the dispatch.tracking topic
```
bin/kafka-console-producer.sh --bootstrap-server [::1]:9092 --topic dispatch.tracking
>{"orderId":"8ed0dc67-41a4-4468-81e1-960340d30c92"} 
```


