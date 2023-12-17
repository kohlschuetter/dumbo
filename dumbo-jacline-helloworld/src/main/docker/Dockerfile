# Replace dumbo_image and dumbo_app with your own identifiers
#
## Build with:
# docker build -t dumbo_image .
#
## Run with:
# docker run -it --env PORT=8080 -p 127.0.0.1:8082:8080/tcp --rm --name dumbo_app dumbo_image
 # (forwards Dumbo server to our host's port 8082)

FROM openjdk:21

WORKDIR /app
COPY . .
ENV DUMBO_SERVER_ADDR=*
ENV DUMBO_CONTENT_SOURCE=.
CMD ["java", "-jar", "${project.name}-${project.version}-main.jar"]
