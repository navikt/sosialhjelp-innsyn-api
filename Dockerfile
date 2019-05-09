FROM navikt/java:8

ENV LC_ALL="no_NB.UTF-8"
ENV LANG="no_NB.UTF-8"
ENV TZ="Europe/Oslo"

COPY build/libs/sosialhjelp-innsyn-api-*-all.jar app.jar

ENV JAVA_OPTS='-Dlogback.configurationFile=logback-remote.xml'