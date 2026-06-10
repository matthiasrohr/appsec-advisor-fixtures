# Architecture Notes

This fixture models a Python FastAPI service that wraps a LangChain prompt and
tool flow for banking support.

## Actors

- Browser SPA user: calls the chat API directly.
- Tenant user: supplies `tenantId`, `userId`, prompt, retrieved context, and
  tool inputs.
- Support operator: replays prompt traces through an admin endpoint.
- External website or internal metadata endpoint: reachable through the HTTP
  fetch tool.

## Components

- FastAPI API: exposes `/api/llm/**` routes.
- LangChain prompt template: mixes system text, retrieved context, and raw user
  prompts without instruction hierarchy enforcement.
- Tool router: selects `requests_get`, `python_exec`, and `file_read` from
  caller-controlled JSON.
- Memory store: keyed by caller-supplied user IDs.
- CORS middleware: allows localhost and preview/partner origins with
  credentials and no CSRF boundary.
- CI workflow: installs unpinned Python dependencies without hashes or SCA.

## Trust Boundaries

- Browser SPA to LLM API without a BFF boundary.
- Tenant prompt and retrieved context to system prompt construction.
- LLM tool router to arbitrary URLs, local code execution, and local files.
- Caller-supplied user IDs to cross-tenant memory.

