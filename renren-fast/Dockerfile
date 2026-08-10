FROM java:17
EXPOSE 8080

VOLUME /tmp
ADD renren-fast.jar  /app.jar
RUN bash -c 'touch /app.jar'
ENTRYPOINT ["java","-jar","/app.jar"]
