FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install -r requirements.txt
COPY . .
ENV OPENAI_API_KEY=sk-live-langchain-fixture-inline
ENV ADMIN_BOOTSTRAP_TOKEN=llm-admin-demo-token
USER root
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8010"]

