FROM eclipse-temurin:17.0.5_8-jdk AS builder

WORKDIR /root/

COPY . .

RUN ./gradlew --no-daemon shadowJar

FROM eclipse-temurin:17.0.5_8-jre

WORKDIR /root/

COPY --from=builder /root/build/libs/AzisabaReportBot.jar /root/

CMD ["java", "-jar", "/root/AzisabaReportBot.jar"]
