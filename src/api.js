const API_BASE = import.meta.env.VITE_API_URL || "";

function guestKey() {
  let key = localStorage.getItem("converter_guest_key");
  if (!key) {
    key = crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random()}`;
    localStorage.setItem("converter_guest_key", key);
  }
  return key;
}

export function getTokens() {
  return {
    access: localStorage.getItem("converter_access"),
    refresh: localStorage.getItem("converter_refresh"),
  };
}

export async function api(path, options = {}) {
  const tokens = getTokens();
  const headers = new Headers(options.headers || {});
  headers.set("X-Guest-Key", guestKey());
  if (tokens.access) headers.set("Authorization", `Bearer ${tokens.access}`);
  if (!(options.body instanceof FormData)) headers.set("Content-Type", options.contentType || "application/json");
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });
  if (response.status === 401 && tokens.refresh && path !== "/api/auth/refresh") {
    const refreshResponse = await fetch(`${API_BASE}/api/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken: tokens.refresh }),
    });
    if (refreshResponse.ok) {
      const renewed = await refreshResponse.json();
      saveAuth(renewed);
      return api(path, options);
    }
  }
  if (!response.ok) {
    let message = `Ошибка сервера (${response.status})`;
    try {
      const data = await response.json();
      message = data.message || message;
    } catch {}
    throw new Error(message);
  }
  if (response.headers.get("content-type")?.includes("application/json")) return response.json();
  return response.blob();
}

export function saveAuth(data) {
  localStorage.setItem("converter_access", data.accessToken);
  localStorage.setItem("converter_refresh", data.refreshToken);
  localStorage.setItem("converter_email", data.email);
  localStorage.setItem("converter_role", data.role);
}

export function clearAuth() {
  ["converter_access", "converter_refresh", "converter_email", "converter_role"].forEach((key) => localStorage.removeItem(key));
}
