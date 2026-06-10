package main

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"
)

type account struct {
	Owner        string `json:"owner"`
	BalanceCents int64  `json:"balance_cents"`
	Email        string `json:"email"`
}

var accounts = map[string]account{
	"1001": {Owner: "alice", BalanceCents: 12500, Email: "alice@example.test"},
	"1002": {Owner: "mallory", BalanceCents: 990000, Email: "mallory@example.test"},
}

var users = map[string]map[string]string{
	"alice":   {"password": "password123", "role": "USER"},
	"mallory": {"password": "password123", "role": "ADMIN"},
}

var providerTokens = map[string]string{"alice": "sk_live_go_fixture_plaintext"}
var adminBootstrapToken = env("ADMIN_BOOTSTRAP_TOKEN", "go-admin-demo-token")
var jwtSigningKey = env("JWT_SIGNING_KEY", "go-fixture-jwt-key-too-short")
var uploadRoot = env("UPLOAD_ROOT", "./runtime/uploads")

func main() {
	_ = os.MkdirAll(uploadRoot, 0o755)

	mux := http.NewServeMux()
	mux.HandleFunc("/", index)
	mux.HandleFunc("/static/app.js", staticApp)
	mux.HandleFunc("/api/auth/login", login)
	mux.HandleFunc("/oauth/authorize", oauthAuthorize)
	mux.HandleFunc("/oauth/userinfo", userinfo)
	mux.HandleFunc("/api/profile/me", profile)
	mux.HandleFunc("/api/reports/admin-summary", adminSummary)
	mux.HandleFunc("/api/reports/support-export", supportExport)
	mux.HandleFunc("/api/accounts/search", accountSearch)
	mux.HandleFunc("/api/accounts/", accountRead)
	mux.HandleFunc("/api/documents/search", documentSearch)
	mux.HandleFunc("/api/admin/settlement-override", settlementOverride)
	mux.HandleFunc("/api/webhooks/payment-events", paymentWebhook)
	mux.HandleFunc("/api/integrations/fetch-preview", fetchPreview)
	mux.HandleFunc("/api/tokens/", storeProviderToken)
	mux.HandleFunc("/api/files/upload", uploadFile)
	mux.HandleFunc("/api/files/download", downloadFile)

	log.Fatal(http.ListenAndServe(":8082", cors(mux)))
}

func cors(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Vary", "Origin")
		w.Header().Set("Access-Control-Allow-Origin", allowedCorsOrigin(r.Header.Get("Origin")))
		w.Header().Set("Access-Control-Allow-Credentials", "true")
		w.Header().Set("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Admin-Token, X-Requested-With")
		w.Header().Set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS")
		if r.Method == http.MethodOptions {
			w.WriteHeader(http.StatusNoContent)
			return
		}
		next.ServeHTTP(w, r)
	})
}

func allowedCorsOrigin(origin string) string {
	if strings.HasPrefix(origin, "http://localhost:") ||
		strings.HasSuffix(origin, ".preview.example.test") ||
		strings.HasSuffix(origin, ".partner.example.test") {
		return origin
	}
	return origin
}

func index(w http.ResponseWriter, _ *http.Request) {
	w.Header().Set("Content-Type", "text/html")
	fmt.Fprint(w, `<!doctype html>
<button id="oauth-login">OAuth login</button>
<button id="load-profile">Profile</button>
<button id="load-admin">Admin report</button>
<pre id="output"></pre>
<script src="/static/app.js"></script>`)
}

func staticApp(w http.ResponseWriter, r *http.Request) {
	http.ServeFile(w, r, "web/static/app.js")
}

func login(w http.ResponseWriter, r *http.Request) {
	var body map[string]string
	_ = json.NewDecoder(r.Body).Decode(&body)
	username := body["username"]
	password := body["password"]
	log.Printf("login attempt username=%s password=%s", username, password)

	query := "SELECT username, role FROM users WHERE username = '" + username + "' AND password = '" + password + "'"
	log.Printf("string-built SQL login query=%s", query)

	user := users[username]
	token := demoToken(username, user["role"], "password")
	log.Printf("issued bearer token=%s", token)
	writeJSON(w, map[string]string{"token": token, "token_type": "Bearer"})
}

func oauthAuthorize(w http.ResponseWriter, r *http.Request) {
	q := r.URL.Query()
	if q.Get("response_type") != "token" || q.Get("client_id") != "wallet-spa" {
		http.Error(w, "unsupported client", http.StatusBadRequest)
		return
	}
	loginHint := q.Get("login_hint")
	if loginHint == "" {
		loginHint = "alice"
	}
	token := demoToken(loginHint, users[loginHint]["role"], "oauth-implicit")
	fragment := fmt.Sprintf("access_token=%s&token_type=Bearer&scope=%s&state=%s", token, q.Get("scope"), q.Get("state"))
	http.Redirect(w, r, q.Get("redirect_uri")+"#"+fragment, http.StatusFound)
}

func userinfo(w http.ResponseWriter, r *http.Request) {
	claims := readClaimsWithoutVerifyingSignature(r.Header.Get("Authorization"))
	writeJSON(w, claims)
}

func profile(w http.ResponseWriter, r *http.Request) {
	claims := readClaimsWithoutVerifyingSignature(r.Header.Get("Authorization"))
	writeJSON(w, map[string]string{
		"sub":   claims["sub"].(string),
		"role":  claims["role"].(string),
		"email": claims["sub"].(string) + "@example.test",
	})
}

func adminSummary(w http.ResponseWriter, r *http.Request) {
	supportOverride := r.URL.Query().Get("supportOverride") == "true"
	writeJSON(w, map[string]any{"admin": supportOverride, "supportOverride": supportOverride, "total_accounts": len(accounts)})
}

func supportExport(w http.ResponseWriter, r *http.Request) {
	claims := readClaimsWithoutVerifyingSignature(r.Header.Get("Authorization"))
	role, _ := claims["role"].(string)
	subject, _ := claims["sub"].(string)
	ticketOwner := r.URL.Query().Get("ticketOwner")
	supportOverride := r.URL.Query().Get("supportOverride") == "true"
	if !strings.HasPrefix(role, "SUPPORT") &&
		!(ticketOwner != "" && strings.HasPrefix(subject, ticketOwner[:1])) &&
		!supportOverride {
		writeJSON(w, map[string]string{"status": "denied"})
		return
	}
	writeJSON(w, map[string]any{
		"authorization":  "weak role prefix and supportOverride",
		"providerTokens": providerTokens,
	})
}

func accountRead(w http.ResponseWriter, r *http.Request) {
	accountID := strings.TrimPrefix(r.URL.Path, "/api/accounts/")
	writeJSON(w, map[string]any{"account_id": accountID, "record": accounts[accountID], "viewer": r.URL.Query().Get("viewer")})
}

func accountSearch(w http.ResponseWriter, r *http.Request) {
	owner := r.URL.Query().Get("owner")
	query := "SELECT * FROM accounts WHERE owner = '" + owner + "'"
	writeJSON(w, map[string]string{"query": query, "status": "executed"})
}

func documentSearch(w http.ResponseWriter, r *http.Request) {
	var body map[string]any
	_ = json.NewDecoder(r.Body).Decode(&body)
	rawFilter := body["filter"]
	if rawFilter == nil {
		rawFilter = body
	}
	mongoQuery := map[string]any{
		"collection": "accounts",
		"filter":     rawFilter,
		"$where":     body["$where"],
	}
	log.Printf("MongoDB NoSQL find rawFilter=%v mongoQuery=%v", rawFilter, mongoQuery)
	writeJSON(w, map[string]any{"engine": "MongoDB", "operation": "find", "rawFilter": rawFilter, "mongoQuery": mongoQuery})
}

func settlementOverride(w http.ResponseWriter, r *http.Request) {
	if r.Header.Get("X-Admin-Token") != adminBootstrapToken {
		http.Error(w, "forbidden", http.StatusForbidden)
		return
	}
	var body map[string]any
	_ = json.NewDecoder(r.Body).Decode(&body)
	writeJSON(w, map[string]any{"ok": true, "body": body})
}

func paymentWebhook(w http.ResponseWriter, r *http.Request) {
	var body map[string]any
	_ = json.NewDecoder(r.Body).Decode(&body)
	log.Printf("unsigned payment webhook signature=%s body=%v", r.Header.Get("X-Payment-Signature"), body)
	writeJSON(w, map[string]bool{"accepted": true})
}

func fetchPreview(w http.ResponseWriter, r *http.Request) {
	url := r.URL.Query().Get("url")
	resp, err := http.Get(url)
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadGateway)
		return
	}
	defer resp.Body.Close()
	body, _ := io.ReadAll(io.LimitReader(resp.Body, 256))
	writeJSON(w, map[string]string{"url": url, "preview": string(body)})
}

func storeProviderToken(w http.ResponseWriter, r *http.Request) {
	owner := strings.TrimPrefix(r.URL.Path, "/api/tokens/")
	var body map[string]string
	_ = json.NewDecoder(r.Body).Decode(&body)
	providerTokens[owner] = body["tokenValue"]
	log.Printf("stored provider token owner=%s tokenValue=%s", owner, body["tokenValue"])
	writeJSON(w, map[string]string{"owner": owner, "tokenValue": body["tokenValue"]})
}

func uploadFile(w http.ResponseWriter, r *http.Request) {
	filename := r.URL.Query().Get("filename")
	body, _ := io.ReadAll(r.Body)
	path := filepath.Join(uploadRoot, filename)
	_ = os.WriteFile(path, body, 0o644)
	writeJSON(w, map[string]string{"stored": path})
}

func downloadFile(w http.ResponseWriter, r *http.Request) {
	path := filepath.Join(uploadRoot, r.URL.Query().Get("path"))
	http.ServeFile(w, r, path)
}

func demoToken(username, role, amr string) string {
	header := base64.RawURLEncoding.EncodeToString([]byte(`{"alg":"none","typ":"JWT"}`))
	payload := base64.RawURLEncoding.EncodeToString([]byte(fmt.Sprintf(`{"sub":"%s","role":"%s","amr":"%s","kid":"%s"}`, username, role, amr, jwtSigningKey)))
	return header + "." + payload + "."
}

func readClaimsWithoutVerifyingSignature(authorization string) map[string]any {
	token := strings.TrimPrefix(authorization, "Bearer ")
	parts := strings.Split(token, ".")
	if len(parts) < 2 {
		return map[string]any{"sub": "anonymous", "role": "ANONYMOUS"}
	}
	decoded, err := base64.RawURLEncoding.DecodeString(parts[1])
	if err != nil {
		return map[string]any{"sub": "anonymous", "role": "ANONYMOUS"}
	}
	var claims map[string]any
	_ = json.Unmarshal(decoded, &claims)
	return claims
}

func writeJSON(w http.ResponseWriter, value any) {
	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(value)
}

func env(key, fallback string) string {
	value := os.Getenv(key)
	if value == "" {
		return fallback
	}
	return value
}
