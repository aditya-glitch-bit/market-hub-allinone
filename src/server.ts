import "./lib/error-capture";

import { consumeLastCapturedError } from "./lib/error-capture";
import { renderErrorPage } from "./lib/error-page";
import { BACKEND_URL } from "./config";

type ServerEntry = {
  fetch: (request: Request, env: unknown, ctx: unknown) => Promise<Response> | Response;
};

let serverEntryPromise: Promise<ServerEntry> | undefined;

async function getServerEntry(): Promise<ServerEntry> {
  if (!serverEntryPromise) {
    serverEntryPromise = import("@tanstack/react-start/server-entry").then(
      (m) => (m.default ?? m) as ServerEntry,
    );
  }
  return serverEntryPromise;
}

// h3 swallows in-handler throws into a normal 500 Response with body
// {"unhandled":true,"message":"HTTPError"} — try/catch alone never fires for those.
async function normalizeCatastrophicSsrResponse(response: Response): Promise<Response> {
  if (response.status < 500) return response;
  const contentType = response.headers.get("content-type") ?? "";
  if (!contentType.includes("application/json")) return response;

  const body = await response.clone().text();
  if (!isH3SwallowedErrorBody(body)) return response;

  console.error(consumeLastCapturedError() ?? new Error(`h3 swallowed SSR error: ${body}`));
  return new Response(renderErrorPage(), {
    status: 500,
    headers: { "content-type": "text/html; charset=utf-8" },
  });
}

function isH3SwallowedErrorBody(body: string): boolean {
  try {
    const payload = JSON.parse(body) as { unhandled?: unknown; message?: unknown };
    return payload.unhandled === true && payload.message === "HTTPError";
  } catch {
    return false;
  }
}

function jsonError(status: number, message: string): Response {
  return new Response(JSON.stringify({ error: message, status }), {
    status,
    headers: { "content-type": "application/json" },
  });
}

/**
 * Forwards "/api/..." to the Java backend so the browser only ever talks to this site.
 * That keeps the login cookie first-party (works in every browser) and avoids CORS.
 */
async function proxyToBackend(request: Request, env: unknown): Promise<Response> {
  // Priority: Worker binding, then process.env (Cloudflare "nodejs_compat"), then src/config.ts.
  const fromBinding = (env as { BACKEND_URL?: string } | null | undefined)?.BACKEND_URL;
  const fromProcess = (globalThis as { process?: { env?: Record<string, string | undefined> } })
    .process?.env?.["BACKEND_URL"];
  const base = (fromBinding || fromProcess || BACKEND_URL).replace(/\/+$/, "");
  if (!base) {
    return jsonError(503, "The backend is not configured for this deployment.");
  }

  const url = new URL(request.url);
  const headers = new Headers(request.headers);
  headers.delete("host");

  const init: RequestInit = { method: request.method, headers, redirect: "manual" };
  if (request.method !== "GET" && request.method !== "HEAD") {
    init.body = await request.arrayBuffer();
  }

  try {
    const upstream = await fetch(base + url.pathname + url.search, init);
    const responseHeaders = new Headers(upstream.headers);
    // fetch() already decoded the body, so these two would now be wrong.
    responseHeaders.delete("content-encoding");
    responseHeaders.delete("content-length");
    return new Response(upstream.body, { status: upstream.status, headers: responseHeaders });
  } catch (error) {
    console.error("Backend request failed", error);
    return jsonError(502, "The MarketHub server is not reachable right now.");
  }
}

export default {
  async fetch(request: Request, env: unknown, ctx: unknown) {
    const { pathname } = new URL(request.url);
    if (pathname === "/api" || pathname.startsWith("/api/")) {
      return proxyToBackend(request, env);
    }
    try {
      const handler = await getServerEntry();
      const response = await handler.fetch(request, env, ctx);
      return await normalizeCatastrophicSsrResponse(response);
    } catch (error) {
      console.error(error);
      return new Response(renderErrorPage(), {
        status: 500,
        headers: { "content-type": "text/html; charset=utf-8" },
      });
    }
  },
};
