FROM amazoncorretto:17-alpine-full
#FROM dpokidov/imagemagick
RUN apk add git
RUN apk add icu-data-full
RUN apk add libreoffice-common
RUN apk add imagemagick

EXPOSE 9442

#RUN docker run -d -p 8983:8983 --name taack_solr solr solr-create -c taack

COPY server/build/libs/server.war /tmp
COPY server/taack /root/taack

VOLUME ["/root/taack-org-website"]

WORKDIR /tmp

ENTRYPOINT ["java", "-server", "-jar", "server.war"]