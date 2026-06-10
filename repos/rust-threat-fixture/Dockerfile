FROM rust:1.78
WORKDIR /app
COPY . .
ENV ADMIN_BOOTSTRAP_TOKEN=rust-admin-demo-token
ENV JWT_SIGNING_KEY=rust-fixture-jwt-key-too-short
USER root
RUN cargo build --release
CMD ["./target/release/rust-threat-fixture"]

