FROM node:20
WORKDIR /app
COPY package.json ./
RUN npm install
COPY . .
ENV ADMIN_BOOTSTRAP_TOKEN=node-admin-demo-token
ENV JWT_SIGNING_KEY=node-fixture-jwt-key-too-short
USER root
RUN npm run build
CMD ["npm", "start"]
