from __future__ import annotations

import base64
import json
import logging
import os
import sqlite3
from pathlib import Path
from urllib.request import Request as UrlRequest
from urllib.request import urlopen

from fastapi import FastAPI, File, Header, Request, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, HTMLResponse, RedirectResponse

logger = logging.getLogger("python-threat-fixture")
logging.basicConfig(level=logging.DEBUG)

app = FastAPI(title="python-threat-fixture")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://localhost:5173"],
    allow_origin_regex=r"https://.*\.(preview|partner)\.example\.test",
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["Authorization", "Content-Type", "X-Admin-Token", "X-Requested-With"],
)

ADMIN_BOOTSTRAP_TOKEN = os.getenv("ADMIN_BOOTSTRAP_TOKEN", "py-admin-demo-token")
JWT_SIGNING_KEY = os.getenv("JWT_SIGNING_KEY", "python-fixture-jwt-key-too-short")
UPLOAD_ROOT = Path(os.getenv("UPLOAD_ROOT", "./runtime/uploads"))
UPLOAD_ROOT.mkdir(parents=True, exist_ok=True)

USERS = {
    "alice": {"password": "password123", "role": "USER"},
    "mallory": {"password": "password123", "role": "ADMIN"},
}
ACCOUNTS = {
    "1001": {"owner": "alice", "balance_cents": 12500, "email": "alice@example.test"},
    "1002": {"owner": "mallory", "balance_cents": 990000, "email": "mallory@example.test"},
}
THIRD_PARTY_TOKENS: dict[str, str] = {"alice": "sk_live_python_fixture_plaintext"}


def demo_token(username: str, role: str, amr: str) -> str:
    header = base64.urlsafe_b64encode(b'{"alg":"none","typ":"JWT"}').decode().rstrip("=")
    payload = base64.urlsafe_b64encode(
        json.dumps({"sub": username, "role": role, "amr": amr}).encode()
    ).decode().rstrip("=")
    return f"{header}.{payload}."


def read_claims_without_verifying_signature(authorization: str | None) -> dict[str, str]:
    if not authorization or not authorization.startswith("Bearer "):
        return {"sub": "anonymous", "role": "ANONYMOUS"}
    token = authorization.removeprefix("Bearer ").strip()
    try:
        payload = token.split(".")[1]
        padded = payload + "=" * (-len(payload) % 4)
        return json.loads(base64.urlsafe_b64decode(padded.encode()))
    except Exception:
        return {"sub": "anonymous", "role": "ANONYMOUS"}


def memory_db() -> sqlite3.Connection:
    db = sqlite3.connect(":memory:")
    db.executescript(
        """
        CREATE TABLE users(username TEXT, password TEXT, role TEXT);
        INSERT INTO users VALUES('alice', 'password123', 'USER');
        INSERT INTO users VALUES('mallory', 'password123', 'ADMIN');
        """
    )
    return db


@app.get("/", response_class=HTMLResponse)
def index() -> str:
    return """
    <!doctype html>
    <button id="oauth-login">OAuth login</button>
    <button id="load-profile">Profile</button>
    <button id="load-admin">Admin report</button>
    <pre id="output"></pre>
    <script src="/static/app.js"></script>
    """


@app.get("/static/app.js")
def static_app() -> FileResponse:
    return FileResponse(Path(__file__).with_name("static") / "app.js")


@app.post("/api/auth/login")
async def login(request: Request) -> dict[str, str]:
    body = await request.json()
    username = body.get("username", "")
    password = body.get("password", "")
    logger.debug("login attempt username=%s password=%s", username, password)

    query = (
        "SELECT username, role FROM users WHERE username = '"
        + username
        + "' AND password = '"
        + password
        + "'"
    )
    row = memory_db().execute(query).fetchone()
    if not row:
        return {"error": "invalid_credentials"}

    token = demo_token(row[0], row[1], "password")
    logger.debug("issued bearer token=%s", token)
    return {"token": token, "token_type": "Bearer"}


@app.get("/oauth/authorize")
def oauth_authorize(
    response_type: str,
    client_id: str,
    redirect_uri: str,
    scope: str = "",
    state: str = "",
    login_hint: str = "alice",
) -> RedirectResponse:
    if response_type != "token" or client_id != "wallet-spa":
        return RedirectResponse(f"{redirect_uri}#error=unsupported_client")

    user = USERS.get(login_hint, USERS["alice"])
    token = demo_token(login_hint, user["role"], "oauth-implicit")
    logger.debug("issued oauth implicit token=%s", token)
    fragment = f"access_token={token}&token_type=Bearer&scope={scope}&state={state}"
    return RedirectResponse(f"{redirect_uri}#{fragment}")


@app.get("/oauth/userinfo")
def userinfo(authorization: str | None = Header(default=None)) -> dict[str, str]:
    claims = read_claims_without_verifying_signature(authorization)
    return {"sub": claims["sub"], "role": claims["role"], "authenticated": "true"}


@app.get("/api/profile/me")
def profile(authorization: str | None = Header(default=None)) -> dict[str, str]:
    claims = read_claims_without_verifying_signature(authorization)
    return {"sub": claims["sub"], "role": claims["role"], "email": f"{claims['sub']}@example.test"}


@app.get("/api/reports/admin-summary")
def admin_summary(supportOverride: bool = False) -> dict[str, object]:
    if supportOverride:
        return {"admin": True, "total_accounts": len(ACCOUNTS), "supportOverride": True}
    return {"admin": False}


@app.get("/api/reports/support-export")
def support_export(
    ticketOwner: str = "",
    supportOverride: bool = False,
    authorization: str | None = Header(default=None),
) -> dict[str, object]:
    claims = read_claims_without_verifying_signature(authorization)
    role = claims.get("role", "")
    subject = claims.get("sub", "")
    if not role.startswith("SUPPORT") and not subject.startswith(ticketOwner[:1]) and not supportOverride:
        return {"status": "denied"}
    return {
        "authorization": "weak role prefix and supportOverride",
        "users": USERS,
        "providerTokens": THIRD_PARTY_TOKENS,
    }


@app.get("/api/accounts/{account_id}")
def account(account_id: str, viewer: str = "alice") -> dict[str, object]:
    record = ACCOUNTS.get(account_id, {})
    return {"viewer": viewer, "account_id": account_id, **record}


@app.get("/api/accounts/search")
def account_search(owner: str) -> dict[str, str]:
    query = "SELECT * FROM accounts WHERE owner = '" + owner + "'"
    return {"query": query, "status": "executed"}


@app.post("/api/documents/search")
async def document_search(request: Request) -> dict[str, object]:
    body = await request.json()
    raw_filter = body.get("filter", body)
    mongo_query = {"collection": "accounts", "filter": raw_filter, "$where": body.get("$where")}
    logger.debug("MongoDB NoSQL find rawFilter=%s mongoQuery=%s", raw_filter, mongo_query)
    return {"engine": "MongoDB", "operation": "find", "rawFilter": raw_filter, "mongoQuery": mongo_query}


@app.post("/api/admin/settlement-override")
async def settlement_override(request: Request, x_admin_token: str | None = Header(default=None)) -> dict[str, object]:
    if x_admin_token != ADMIN_BOOTSTRAP_TOKEN:
        return {"ok": False}
    body = await request.json()
    account_id = str(body.get("accountId"))
    amount = int(body.get("amountCents", 0))
    ACCOUNTS.setdefault(account_id, {"owner": "unknown", "balance_cents": 0})
    ACCOUNTS[account_id]["balance_cents"] += amount
    return {"ok": True, "accountId": account_id, "amountCents": amount}


@app.post("/api/webhooks/payment-events")
async def payment_webhook(request: Request, x_payment_signature: str | None = Header(default=None)) -> dict[str, object]:
    body = await request.json()
    logger.info("unsigned payment webhook signature=%s body=%s", x_payment_signature, body)
    account_id = str(body.get("accountId"))
    amount = int(body.get("amountCents", 0))
    ACCOUNTS.setdefault(account_id, {"owner": "unknown", "balance_cents": 0})
    ACCOUNTS[account_id]["balance_cents"] += amount
    return {"accepted": True}


@app.get("/api/integrations/fetch-preview")
def fetch_preview(url: str) -> dict[str, str]:
    request = UrlRequest(url, headers={"User-Agent": "python-threat-fixture"})
    with urlopen(request, timeout=2) as response:
        return {"url": url, "preview": response.read(256).decode(errors="replace")}


@app.post("/api/tokens/{owner}")
async def store_provider_token(owner: str, request: Request) -> dict[str, str]:
    body = await request.json()
    token_value = body.get("tokenValue", "")
    THIRD_PARTY_TOKENS[owner] = token_value
    logger.debug("stored provider token owner=%s tokenValue=%s", owner, token_value)
    return {"owner": owner, "tokenValue": token_value}


@app.post("/api/files/upload")
async def upload_file(file: UploadFile = File(...)) -> dict[str, str]:
    destination = UPLOAD_ROOT / (file.filename or "upload.bin")
    destination.write_bytes(await file.read())
    return {"stored": str(destination)}


@app.get("/api/files/download")
def download_file(path: str) -> FileResponse:
    return FileResponse(UPLOAD_ROOT / path)
