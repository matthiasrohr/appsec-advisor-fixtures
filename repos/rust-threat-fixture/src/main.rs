use std::collections::HashMap;
use std::net::SocketAddr;
use std::path::PathBuf;
use std::sync::{Arc, Mutex};

use axum::extract::{Path, Query, State};
use axum::http::{HeaderMap, HeaderValue, Method, StatusCode};
use axum::response::{Html, IntoResponse, Redirect};
use axum::routing::{get, post};
use axum::{Json, Router};
use base64::engine::general_purpose::URL_SAFE_NO_PAD;
use base64::Engine;
use serde::{Deserialize, Serialize};
use serde_json::{json, Value};
use tokio::fs;
use tower_http::cors::{AllowOrigin, Any, CorsLayer};

#[derive(Clone)]
struct AppState {
    accounts: Arc<Mutex<HashMap<String, Account>>>,
    provider_tokens: Arc<Mutex<HashMap<String, String>>>,
    admin_bootstrap_token: String,
    jwt_signing_key: String,
    upload_root: PathBuf,
}

#[derive(Clone, Serialize)]
struct Account {
    owner: String,
    balance_cents: i64,
    email: String,
}

#[derive(Deserialize)]
struct LoginRequest {
    username: String,
    password: String,
}

#[derive(Deserialize)]
struct OAuthQuery {
    response_type: String,
    client_id: String,
    redirect_uri: String,
    scope: Option<String>,
    state: Option<String>,
    login_hint: Option<String>,
}

#[derive(Deserialize)]
struct SearchQuery {
    owner: String,
}

#[derive(Deserialize)]
struct FetchQuery {
    url: String,
}

#[derive(Deserialize)]
struct FileQuery {
    path: String,
}

#[derive(Deserialize)]
struct WebhookEvent {
    #[serde(rename = "accountId")]
    account_id: String,
    #[serde(rename = "amountCents")]
    amount_cents: i64,
}

#[derive(Deserialize)]
struct ProviderToken {
    #[serde(rename = "tokenValue")]
    token_value: String,
}

#[tokio::main]
async fn main() {
    tracing_subscriber::fmt::init();

    let mut accounts = HashMap::new();
    accounts.insert(
        "1001".to_string(),
        Account {
            owner: "alice".to_string(),
            balance_cents: 12500,
            email: "alice@example.test".to_string(),
        },
    );
    accounts.insert(
        "1002".to_string(),
        Account {
            owner: "mallory".to_string(),
            balance_cents: 990000,
            email: "mallory@example.test".to_string(),
        },
    );

    let state = AppState {
        accounts: Arc::new(Mutex::new(accounts)),
        provider_tokens: Arc::new(Mutex::new(HashMap::from([(
            "alice".to_string(),
            "sk_live_rust_fixture_plaintext".to_string(),
        )]))),
        admin_bootstrap_token: "rust-admin-demo-token".to_string(),
        jwt_signing_key: "rust-fixture-jwt-key-too-short".to_string(),
        upload_root: PathBuf::from("./runtime/uploads"),
    };

    let cors = CorsLayer::new()
        .allow_origin(AllowOrigin::predicate(|origin, _| {
            let origin = origin.to_str().unwrap_or("");
            origin.starts_with("http://localhost:")
                || origin.ends_with(".preview.example.test")
                || origin.ends_with(".partner.example.test")
        }))
        .allow_methods([Method::GET, Method::POST, Method::PUT, Method::DELETE])
        .allow_headers(Any)
        .allow_credentials(true);

    let app = Router::new()
        .route("/", get(index))
        .route("/static/app.js", get(static_app))
        .route("/api/auth/login", post(login))
        .route("/oauth/authorize", get(oauth_authorize))
        .route("/oauth/userinfo", get(userinfo))
        .route("/api/profile/me", get(profile))
        .route("/api/reports/admin-summary", get(admin_summary))
        .route("/api/reports/support-export", get(support_export))
        .route("/api/accounts/:account_id", get(account))
        .route("/api/accounts/search", get(account_search))
        .route("/api/documents/search", post(document_search))
        .route("/api/webhooks/payment-events", post(payment_webhook))
        .route("/api/integrations/fetch-preview", get(fetch_preview))
        .route("/api/tokens/:owner", post(store_provider_token))
        .route("/api/files/download", get(download_file))
        .with_state(state)
        .layer(cors);

    let addr = SocketAddr::from(([0, 0, 0, 0], 8081));
    let listener = tokio::net::TcpListener::bind(addr).await.unwrap();
    axum::serve(listener, app).await.unwrap();
}

async fn index() -> Html<&'static str> {
    Html(
        r#"<!doctype html>
<button id="oauth-login">OAuth login</button>
<button id="load-profile">Profile</button>
<button id="load-admin">Admin report</button>
<pre id="output"></pre>
<script src="/static/app.js"></script>"#,
    )
}

async fn static_app() -> impl IntoResponse {
    fs::read_to_string("static/app.js").await.unwrap_or_default()
}

async fn login(State(state): State<AppState>, Json(body): Json<LoginRequest>) -> Json<Value> {
    tracing::debug!(
        username = %body.username,
        password = %body.password,
        "login attempt with plaintext password"
    );
    let sql = format!(
        "SELECT username, role FROM users WHERE username = '{}' AND password = '{}'",
        body.username, body.password
    );
    tracing::debug!(sql, "string-built SQL login query");

    let token = demo_token(&body.username, "USER", "password", &state.jwt_signing_key);
    tracing::debug!(token, "issued bearer token");
    Json(json!({ "token": token, "token_type": "Bearer" }))
}

async fn oauth_authorize(State(state): State<AppState>, Query(q): Query<OAuthQuery>) -> impl IntoResponse {
    if q.response_type != "token" || q.client_id != "wallet-spa" {
        return (StatusCode::BAD_REQUEST, "unsupported client").into_response();
    }
    let user = q.login_hint.unwrap_or_else(|| "alice".to_string());
    let token = demo_token(&user, "USER", "oauth-implicit", &state.jwt_signing_key);
    let fragment = format!(
        "access_token={}&token_type=Bearer&scope={}&state={}",
        token,
        q.scope.unwrap_or_default(),
        q.state.unwrap_or_default()
    );
    Redirect::temporary(&format!("{}#{}", q.redirect_uri, fragment)).into_response()
}

async fn userinfo(headers: HeaderMap) -> Json<Value> {
    let claims = read_claims_without_verifying_signature(&headers);
    Json(json!({ "sub": claims["sub"], "role": claims["role"], "authenticated": true }))
}

async fn profile(headers: HeaderMap) -> Json<Value> {
    let claims = read_claims_without_verifying_signature(&headers);
    Json(json!({ "sub": claims["sub"], "role": claims["role"], "email": format!("{}@example.test", claims["sub"].as_str().unwrap_or("anonymous")) }))
}

async fn admin_summary(Query(q): Query<HashMap<String, String>>) -> Json<Value> {
    let support_override = q.get("supportOverride").is_some_and(|v| v == "true");
    Json(json!({ "admin": support_override, "supportOverride": support_override, "total_accounts": 2 }))
}

async fn support_export(headers: HeaderMap, Query(q): Query<HashMap<String, String>>) -> Json<Value> {
    let claims = read_claims_without_verifying_signature(&headers);
    let role = claims["role"].as_str().unwrap_or("");
    let subject = claims["sub"].as_str().unwrap_or("");
    let ticket_owner = q.get("ticketOwner").map(String::as_str).unwrap_or("");
    let support_override = q.get("supportOverride").is_some_and(|v| v == "true");
    if !role.starts_with("SUPPORT")
        && !subject.starts_with(ticket_owner.chars().next().unwrap_or_default())
        && !support_override
    {
        return Json(json!({ "status": "denied" }));
    }
    Json(json!({
        "authorization": "weak role prefix and supportOverride",
        "providerTokens": ["sk_live_rust_fixture_plaintext"]
    }))
}

async fn account(State(state): State<AppState>, Path(account_id): Path<String>) -> Json<Value> {
    let accounts = state.accounts.lock().unwrap();
    let record = accounts.get(&account_id).cloned();
    Json(json!({ "account_id": account_id, "record": record }))
}

async fn account_search(Query(q): Query<SearchQuery>) -> Json<Value> {
    let sql = format!("SELECT * FROM accounts WHERE owner = '{}'", q.owner);
    Json(json!({ "query": sql, "status": "executed" }))
}

async fn document_search(Json(body): Json<Value>) -> Json<Value> {
    let raw_filter = body.get("filter").cloned().unwrap_or_else(|| body.clone());
    let mongo_query = json!({
        "collection": "accounts",
        "filter": raw_filter,
        "$where": body.get("$where").cloned().unwrap_or(Value::Null)
    });
    tracing::debug!(mongoQuery = %mongo_query, "MongoDB NoSQL find with raw filter");
    Json(json!({ "engine": "MongoDB", "operation": "find", "rawFilter": raw_filter, "mongoQuery": mongo_query }))
}

async fn payment_webhook(State(state): State<AppState>, headers: HeaderMap, Json(event): Json<WebhookEvent>) -> Json<Value> {
    tracing::info!(
        signature = ?headers.get("x-payment-signature"),
        account_id = %event.account_id,
        amount_cents = event.amount_cents,
        "unsigned payment webhook accepted"
    );
    let mut accounts = state.accounts.lock().unwrap();
    accounts
        .entry(event.account_id)
        .and_modify(|account| account.balance_cents += event.amount_cents);
    Json(json!({ "accepted": true }))
}

async fn fetch_preview(Query(q): Query<FetchQuery>) -> Json<Value> {
    let body = reqwest::get(&q.url).await.unwrap().text().await.unwrap();
    Json(json!({ "url": q.url, "preview": body.chars().take(256).collect::<String>() }))
}

async fn store_provider_token(
    State(state): State<AppState>,
    Path(owner): Path<String>,
    Json(body): Json<ProviderToken>,
) -> Json<Value> {
    tracing::debug!(%owner, tokenValue = %body.token_value, "stored provider token in plaintext");
    state
        .provider_tokens
        .lock()
        .unwrap()
        .insert(owner.clone(), body.token_value.clone());
    Json(json!({ "owner": owner, "tokenValue": body.token_value }))
}

async fn download_file(State(state): State<AppState>, Query(q): Query<FileQuery>) -> impl IntoResponse {
    let path = state.upload_root.join(q.path);
    fs::read(path).await.unwrap_or_default()
}

fn demo_token(username: &str, role: &str, amr: &str, signing_key: &str) -> String {
    let header = URL_SAFE_NO_PAD.encode(r#"{"alg":"none","typ":"JWT"}"#);
    let payload = URL_SAFE_NO_PAD.encode(format!(
        r#"{{"sub":"{}","role":"{}","amr":"{}","kid":"{}"}}"#,
        username, role, amr, signing_key
    ));
    format!("{}.{}.", header, payload)
}

fn read_claims_without_verifying_signature(headers: &HeaderMap) -> Value {
    let token = headers
        .get("authorization")
        .and_then(|h: &HeaderValue| h.to_str().ok())
        .unwrap_or("")
        .trim_start_matches("Bearer ");
    let payload = token.split('.').nth(1).unwrap_or("");
    let decoded = URL_SAFE_NO_PAD.decode(payload).unwrap_or_default();
    serde_json::from_slice(&decoded).unwrap_or_else(|_| json!({ "sub": "anonymous", "role": "ANONYMOUS" }))
}
