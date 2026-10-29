/**
 * Address of the deployed Java backend (no trailing slash), for example
 * "https://markethub-api.onrender.com".
 *
 * The website never calls this address from the browser. It calls its own "/api/..." paths and
 * the server (src/server.ts) forwards them here, so login cookies stay on one domain and no
 * CORS setup is needed. Leave it empty to run in sample-data preview mode.
 *
 * A BACKEND_URL variable (Cloudflare runtime variable or process environment) overrides this value.
 */
export const BACKEND_URL = "http://127.0.0.1:8080";
