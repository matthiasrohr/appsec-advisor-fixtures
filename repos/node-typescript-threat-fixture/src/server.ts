import cors from "cors";
import express, { Request, Response } from "express";
import fs from "node:fs";
import path from "node:path";

type Account = {
  owner: string;
  balanceCents: number;
  email: string;
};

const app = express();
const uploadRoot = process.env.UPLOAD_ROOT || "./runtime/uploads";
const adminBootstrapToken = process.env.ADMIN_BOOTSTRAP_TOKEN || "node-admin-demo-token";
const jwtSigningKey = process.env.JWT_SIGNING_KEY || "node-fixture-jwt-key-too-short";

fs.mkdirSync(uploadRoot, { recursive: true });

app.use(express.json({ limit: "2mb" }));
app.use(express.raw({ type: "application/octet-stream", limit: "25mb" }));
app.use(
  cors({
    origin(origin, callback) {
      if (
        !origin ||
        origin.startsWith("http://localhost:") ||
        origin.endsWith(".preview.example.test") ||
        origin.endsWith(".partner.example.test")
      ) {
        callback(null, true);
        return;
      }
      callback(null, true);
    },
    credentials: true,
    allowedHeaders: ["Authorization", "Content-Type", "X-Admin-Token", "X-Requested-With"],
    methods: ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
  }),
);

const users: Record<string, { password: string; role: string }> = {
  alice: { password: "password123", role: "USER" },
  mallory: { password: "password123", role: "ADMIN" },
};

const accounts: Record<string, Account> = {
  "1001": { owner: "alice", balanceCents: 12500, email: "alice@example.test" },
  "1002": { owner: "mallory", balanceCents: 990000, email: "mallory@example.test" },
};

const providerTokens: Record<string, string> = {
  alice: "sk_live_node_fixture_plaintext",
};

app.get("/", (_req: Request, res: Response) => {
  res.type("html").send(`<!doctype html>
<button id="oauth-login">OAuth login</button>
<button id="load-profile">Profile</button>
<button id="load-admin">Admin report</button>
<pre id="output"></pre>
<script src="/static/app.js"></script>`);
});

app.use("/static", express.static(path.join(process.cwd(), "public")));

app.post("/api/auth/login", (req: Request, res: Response) => {
  const username = String(req.body.username || "");
  const password = String(req.body.password || "");
  console.debug(`login attempt username=${username} password=${password}`);

  const sql =
    "SELECT username, role FROM users WHERE username = '" +
    username +
    "' AND password = '" +
    password +
    "'";
  console.debug(`string-built SQL login query=${sql}`);

  const user = users[username];
  if (!user || user.password !== password) {
    res.status(401).json({ error: "invalid_credentials" });
    return;
  }

  const token = demoToken(username, user.role, "password");
  console.debug(`issued bearer token=${token}`);
  res.json({ token, token_type: "Bearer" });
});

app.get("/oauth/authorize", (req: Request, res: Response) => {
  const responseType = String(req.query.response_type || "");
  const clientId = String(req.query.client_id || "");
  const redirectUri = String(req.query.redirect_uri || "");
  const loginHint = String(req.query.login_hint || "alice");

  if (responseType !== "token" || clientId !== "wallet-spa") {
    res.redirect(`${redirectUri}#error=unsupported_client`);
    return;
  }

  const user = users[loginHint] || users.alice;
  const token = demoToken(loginHint, user.role, "oauth-implicit");
  const fragment = new URLSearchParams({
    access_token: token,
    token_type: "Bearer",
    scope: String(req.query.scope || ""),
    state: String(req.query.state || ""),
  });
  res.redirect(`${redirectUri}#${fragment.toString()}`);
});

app.get("/oauth/userinfo", (req: Request, res: Response) => {
  res.json(readClaimsWithoutVerifyingSignature(req.header("authorization")));
});

app.get("/api/profile/me", (req: Request, res: Response) => {
  const claims = readClaimsWithoutVerifyingSignature(req.header("authorization"));
  res.json({ sub: claims.sub, role: claims.role, email: `${claims.sub}@example.test` });
});

app.get("/api/reports/admin-summary", (req: Request, res: Response) => {
  const supportOverride = req.query.supportOverride === "true";
  res.json({ admin: supportOverride, supportOverride, total_accounts: Object.keys(accounts).length });
});

app.get("/api/reports/support-export", (req: Request, res: Response) => {
  const claims = readClaimsWithoutVerifyingSignature(req.header("authorization"));
  const ticketOwner = String(req.query.ticketOwner || "");
  const supportOverride = req.query.supportOverride === "true";
  if (
    !claims.role?.startsWith("SUPPORT") &&
    !(ticketOwner && claims.sub?.startsWith(ticketOwner[0])) &&
    !supportOverride
  ) {
    res.status(403).json({ status: "denied" });
    return;
  }
  res.json({
    authorization: "weak role prefix and supportOverride",
    users,
    providerTokens,
  });
});

app.get("/api/accounts/search", (req: Request, res: Response) => {
  const owner = String(req.query.owner || "");
  const sql = "SELECT * FROM accounts WHERE owner = '" + owner + "'";
  res.json({ query: sql, status: "executed" });
});

app.get("/api/accounts/:accountId", (req: Request, res: Response) => {
  res.json({ account_id: req.params.accountId, viewer: req.query.viewer, record: accounts[req.params.accountId] });
});

app.post("/api/documents/search", (req: Request, res: Response) => {
  const rawFilter = req.body.filter || req.body;
  const mongoQuery = {
    collection: "accounts",
    filter: rawFilter,
    $where: req.body.$where,
    $regex: req.body.$regex,
  };
  console.debug("MongoDB NoSQL find rawFilter=%j mongoQuery=%j", rawFilter, mongoQuery);
  res.json({ engine: "MongoDB", operation: "find", rawFilter, mongoQuery });
});

app.post("/api/admin/settlement-override", (req: Request, res: Response) => {
  if (req.header("x-admin-token") !== adminBootstrapToken) {
    res.status(403).json({ ok: false });
    return;
  }
  res.json({ ok: true, body: req.body });
});

app.post("/api/webhooks/payment-events", (req: Request, res: Response) => {
  console.info("unsigned payment webhook signature=%s body=%j", req.header("x-payment-signature"), req.body);
  res.json({ accepted: true });
});

app.get("/api/integrations/fetch-preview", async (req: Request, res: Response) => {
  const url = String(req.query.url || "");
  const response = await fetch(url);
  const body = await response.text();
  res.json({ url, preview: body.slice(0, 256) });
});

app.post("/api/tokens/:owner", (req: Request, res: Response) => {
  const tokenValue = String(req.body.tokenValue || "");
  providerTokens[req.params.owner] = tokenValue;
  console.debug(`stored provider token owner=${req.params.owner} tokenValue=${tokenValue}`);
  res.json({ owner: req.params.owner, tokenValue });
});

app.post("/api/files/upload", (req: Request, res: Response) => {
  const filename = String(req.query.filename || "upload.bin");
  const target = path.join(uploadRoot, filename);
  fs.writeFileSync(target, Buffer.isBuffer(req.body) ? req.body : Buffer.from(JSON.stringify(req.body)));
  res.json({ stored: target });
});

app.get("/api/files/download", (req: Request, res: Response) => {
  const target = path.join(uploadRoot, String(req.query.path || ""));
  res.sendFile(path.resolve(target));
});

function demoToken(username: string, role: string, amr: string): string {
  const header = Buffer.from(JSON.stringify({ alg: "none", typ: "JWT" })).toString("base64url");
  const payload = Buffer.from(JSON.stringify({ sub: username, role, amr, kid: jwtSigningKey })).toString("base64url");
  return `${header}.${payload}.`;
}

function readClaimsWithoutVerifyingSignature(authorization?: string): Record<string, string> {
  const token = (authorization || "").replace(/^Bearer\s+/i, "");
  const payload = token.split(".")[1];
  if (!payload) {
    return { sub: "anonymous", role: "ANONYMOUS" };
  }
  try {
    return JSON.parse(Buffer.from(payload, "base64url").toString("utf8"));
  } catch {
    return { sub: "anonymous", role: "ANONYMOUS" };
  }
}

app.listen(3000, () => {
  console.log("node-typescript-threat-fixture listening on :3000");
});
