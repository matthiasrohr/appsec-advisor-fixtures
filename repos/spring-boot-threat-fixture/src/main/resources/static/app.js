const output = document.getElementById("output");

function show(value) {
    output.textContent = typeof value === "string" ? value : JSON.stringify(value, null, 2);
}

function token() {
    return localStorage.getItem("walletFixtureAccessToken");
}

function authHeaders() {
    const accessToken = token();
    return accessToken ? { Authorization: `Bearer ${accessToken}` } : {};
}

document.getElementById("password-login").addEventListener("click", async () => {
    const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: "alice", password: "password123" })
    });
    const body = await response.json();
    if (body.token) {
        localStorage.setItem("walletFixtureAccessToken", body.token);
    }
    show(body);
});

document.getElementById("oauth-login").addEventListener("click", () => {
    const state = Math.random().toString(16).substring(2);
    const redirectUri = `${window.location.origin}/oauth-callback.html`;
    window.location.href = `/oauth/authorize?response_type=token&client_id=wallet-spa&redirect_uri=${encodeURIComponent(redirectUri)}&scope=accounts:read%20admin:read&state=${state}&login_hint=alice`;
});

document.getElementById("load-profile").addEventListener("click", async () => {
    const response = await fetch("/api/profile/me", { headers: authHeaders() });
    show(await response.json());
});

document.getElementById("load-admin").addEventListener("click", async () => {
    const response = await fetch("/api/reports/admin-summary?supportOverride=true", { headers: authHeaders() });
    show(await response.json());
});

