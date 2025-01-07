# Introduction to Kafka with Spring Boot - Tracking Service

This repository contains the code to support the [Introduction to Kafka with Spring Boot](https://www.udemy.com/course/introduction-to-kafka-with-spring-boot/?referralCode=15118530CA63AD1AF16D) online course, for the Tracking Service portion of the course.

The associated repository for the Dispatch Service can be found here:  [Dispatch Service Repository](https://github.com/dboeckli/dispatch)

The application code is for a message driven service which utilises Kafka and Spring Boot 3.

Send Message:
For that you need a kafka cli environment which will be available when you have done the kafka wsl setup
```
bin/kafka-console-producer.sh --bootstrap-server [::1]:9092 --topic order.created
>{"orderId":"8ed0dc67-41a4-4468-81e1-960340d30c92","item":"first-item"} 
```


