FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install -r requirements.txt
COPY . .
ENV ADMIN_BOOTSTRAP_TOKEN=py-admin-demo-token
ENV JWT_SIGNING_KEY=python-fixture-jwt-key-too-short
USER root
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]

