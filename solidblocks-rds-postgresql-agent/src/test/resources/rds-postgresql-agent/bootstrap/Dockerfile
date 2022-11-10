FROM nginx

ARG SOLIDBLOCKS_GREEN_VERSION
ARG SOLIDBLOCKS_BLUE_VERSION

ARG BLUE_HTML_DIR="/etc/nginx/html/solidblocks-rds-controller/solidblocks-rds-postgresql-agent/${SOLIDBLOCKS_BLUE_VERSION}"
ARG GREEN_HTML_DIR="/etc/nginx/html/solidblocks-rds-controller/solidblocks-rds-postgresql-agent/${SOLIDBLOCKS_GREEN_VERSION}"

COPY nginx.conf /etc/nginx/nginx.conf

RUN mkdir -p ${BLUE_HTML_DIR}
RUN mkdir -p ${GREEN_HTML_DIR}

COPY "artefacts/solidblocks-rds-postgresql-agent-${SOLIDBLOCKS_BLUE_VERSION}.tar" "${BLUE_HTML_DIR}/solidblocks-rds-postgresql-agent-${SOLIDBLOCKS_BLUE_VERSION}.tar"
COPY "artefacts/solidblocks-rds-postgresql-agent-${SOLIDBLOCKS_GREEN_VERSION}.tar" "${GREEN_HTML_DIR}/solidblocks-rds-postgresql-agent-${SOLIDBLOCKS_GREEN_VERSION}.tar"
