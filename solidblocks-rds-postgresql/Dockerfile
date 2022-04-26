FROM alpine:3.15

ENV GOMPLATE_CHECKSUM=603539aac4e09f98a8ca5b6e5da0c21213221206dc7175a5644255c7a22b936d
ENV GOMPLATE_VERSION=3.10.0

ENV USER=rds
ENV UID=10000

ENV GROUP=${USER}
ENV GID=10000

ENV LOCAL_STORAGE_DIR=/storage/local
ENV DATA_DIR=${LOCAL_STORAGE_DIR}/data
ENV BACKUP_DIR=${LOCAL_STORAGE_DIR}/backup

RUN addgroup -g $GID $GROUP \
  && adduser $USER \
  -h /rds \
  -D \
  -s /bin/bash \
  -g "Unprivileged application user" \
  -G $GROUP \
  -u $UID

ENV LANG en_US.utf8

RUN apk upgrade --available
RUN apk add \
    postgresql \
    jq \
    bash \
    curl \
    pgbackrest

#RUN chown -R ${USER}:${USER} /rds
#RUN apt-get purge -y ${BUILD_PACKAGES} && rm -rf /tmp/* && apt-get clean autoclean && apt-get autoremove --yes && rm -rf /var/lib/{apt,dpkg,cache,log}/

#RUN mkdir -p ${DATA_DIR} && chown -R ${USER}:${USER} ${DATA_DIR} && chmod -R 700 ${DATA_DIR}
#RUN mkdir -p ${BACKUP_DIR} && chown -R ${USER}:${USER} ${BACKUP_DIR}

RUN mkdir -p ${LOCAL_STORAGE_DIR} && chown -R ${USER}:${USER} ${LOCAL_STORAGE_DIR}

RUN curl -L https://github.com/hairyhenderson/gomplate/releases/download/v${GOMPLATE_VERSION}/gomplate_linux-amd64-slim -o /usr/bin/gomplate && \
  echo "${GOMPLATE_CHECKSUM}  /usr/bin/gomplate" | sha256sum -c && \
  chmod +x /usr/bin/gomplate

USER ${USER}

#EXPOSE 5432
WORKDIR /rds

#COPY templates /rds/templates
RUN mkdir -p config/templates
RUN mkdir bin
RUN mkdir log
RUN mkdir socket

COPY bin/run.sh bin/

CMD [ "/rds/bin/run.sh" ]