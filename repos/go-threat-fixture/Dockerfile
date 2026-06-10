FROM golang:1.22
WORKDIR /app
COPY . .
ENV ADMIN_BOOTSTRAP_TOKEN=go-admin-demo-token
ENV JWT_SIGNING_KEY=go-fixture-jwt-key-too-short
USER root
RUN go build -o /usr/local/bin/go-threat-fixture ./cmd/server
CMD ["/usr/local/bin/go-threat-fixture"]

