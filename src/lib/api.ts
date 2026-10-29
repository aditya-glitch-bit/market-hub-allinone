/**
 * Tiny client for the MarketHub Java API.
 *
 * Every call goes to the same origin ("/api/...") and the session cookie is sent automatically.
 * When the backend is not reachable the error has `offline === true`, which lets the interface
 * fall back to its sample-data preview instead of breaking.
 */
export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }

  /** True when the server could not be reached (as opposed to a normal 4xx answer). */
  get offline(): boolean {
    return this.status === 0 || this.status >= 502;
  }
}

interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "DELETE";
  body?: unknown;
  /** Abort after this many milliseconds (default 20 s). */
  timeoutMs?: number;
}

export async function api<T = unknown>(path: string, options: RequestOptions = {}): Promise<T> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), options.timeoutMs ?? 20000);
  let response: Response;
  try {
    const init: RequestInit = {
      method: options.method ?? "GET",
      credentials: "same-origin",
      signal: controller.signal,
    };
    if (options.body !== undefined) {
      init.headers = { "Content-Type": "application/json" };
      init.body = JSON.stringify(options.body);
    }
    response = await fetch(`/api${path}`, init);
  } catch {
    throw new ApiError(0, "Cannot reach the MarketHub server.");
  } finally {
    clearTimeout(timer);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  // The backend always answers JSON. Anything else (for example an HTML 404 page when no
  // backend is configured) means the API is not really there.
  const isJson = (response.headers.get("content-type") ?? "").includes("application/json");
  if (!isJson) {
    throw new ApiError(502, "The MarketHub server is not available right now.");
  }

  const data = (await response.json().catch(() => null)) as { error?: string } | null;
  if (!response.ok) {
    throw new ApiError(response.status, data?.error ?? `Request failed (${response.status}).`);
  }
  return data as T;
}

/** Turns anything thrown into text that is safe to show to the person using the site. */
export function messageOf(error: unknown): string {
  return error instanceof Error ? error.message : "Something went wrong.";
}
