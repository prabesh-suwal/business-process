#!/bin/bash

cd cas-server && mvn spring-boot:run &
cd gateway-product && mvn spring-boot:run &
cd workflow-service && mvn spring-boot:run &
cd memo-service && mvn spring-boot:run &

wait
