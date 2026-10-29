import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "@tanstack/react-router";
import { ArrowRight, Mail } from "lucide-react";
import { Button } from "@/components/ui/button";
import { messageOf } from "@/lib/api";
import { useStore } from "./store";
import { useMarket, workspaceOf } from "./market";

/** Password shared by every seeded demo account in the Java backend (see DemoData.java). */
const DEMO_PASSWORD = "Demo@1234";
const DEMO_ACCOUNTS = [
  { label: "Buyer", email: "olivia@example.com" },
  { label: "Seller", email: "hello@example.com" },
  { label: "Admin", email: "admin@example.com" },
];

/** Sign in / create account. Talks to the Java API when it is connected, otherwise shows the preview. */
export function AuthPage({ register = false }: { register?: boolean }) {
  const [role, setRole] = useState("buyer");
  const [submitted, setSubmitted] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const store = useStore();
  const market = useMarket();
  const navigate = useNavigate();

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!market.live) {
      setSubmitted(true);
      return;
    }
    const data = new FormData(event.currentTarget);
    const email = String(data.get("email") ?? "");
    const password = String(data.get("password") ?? "");
    setBusy(true);
    setError("");
    try {
      const account = register
        ? await market.register(String(data.get("name") ?? ""), email, password, role)
        : await market.login(email, password);
      store.notify(`Welcome, ${account.name}`);
      navigate({
        to: "/dashboard/$role",
        params: { role: workspaceOf(account) },
        search: { section: "overview" },
      });
    } catch (e) {
      setError(messageOf(e));
    } finally {
      setBusy(false);
    }
  }

  if (market.user) {
    return (
      <main className="auth-page">
        <h1>You're signed in</h1>
        <p>
          {market.user.name} · {market.user.email} · {workspaceOf(market.user)}
        </p>
        <div className="auth-form">
          <Button asChild className="w-full" size="lg">
            <Link
              to="/dashboard/$role"
              params={{ role: workspaceOf(market.user) }}
              search={{ section: "overview" }}
            >
              Open my workspace
              <ArrowRight />
            </Link>
          </Button>
          <Button
            variant="outline"
            className="w-full"
            onClick={async () => {
              await market.logout();
              store.notify("Signed out");
            }}
          >
            Sign out
          </Button>
        </div>
      </main>
    );
  }

  return (
    <main className="auth-page">
      <h1>{register ? "Find your place at MarketHub" : "Welcome back"}</h1>
      <p>
        {register
          ? "Good finds and great possibilities start here."
          : "A world of good finds is waiting for you."}
      </p>
      <form className="auth-form" onSubmit={onSubmit}>
        {register && (
          <label>
            Full name
            <input name="name" placeholder="Your full name" required autoComplete="name" />
          </label>
        )}
        <label>
          Email address
          <input
            name="email"
            type="email"
            placeholder="you@example.com"
            required
            autoComplete="email"
          />
        </label>
        <label>
          Password
          <input
            name="password"
            type="password"
            placeholder="At least 8 characters"
            minLength={8}
            required
            autoComplete={register ? "new-password" : "current-password"}
          />
        </label>
        {register && (
          <>
            <label>I'm here to</label>
            <div className="role-options">
              {["buyer", "seller"].map((r) => (
                <Button
                  key={r}
                  type="button"
                  variant="outline"
                  className={role === r ? "selected" : ""}
                  onClick={() => setRole(r)}
                >
                  {r === "buyer" ? "Shop" : "Sell"}
                </Button>
              ))}
            </div>
          </>
        )}
        {!register && (
          <div className="mb-4 text-right">
            <Button
              type="button"
              variant="link"
              size="sm"
              onClick={() => store.notify("Password recovery is not connected yet.")}
            >
              Forgot password?
            </Button>
          </div>
        )}
        <Button type="submit" className="w-full" size="lg" disabled={busy}>
          {busy ? "Please wait…" : register ? "Create account" : "Sign in"}
          <ArrowRight />
        </Button>
        {error && (
          <p role="alert" className="demo-note">
            {error}
          </p>
        )}
        {submitted && (
          <p role="status" className="demo-note">
            <Mail size={15} className="inline mr-2" />
            This is a frontend preview.{" "}
            {register ? "No account was created." : "Sign-in is not connected."} Explore a sample
            workspace below.
          </p>
        )}
      </form>
      <p className="auth-link">
        {register ? "Already part of MarketHub?" : "New around here?"}{" "}
        <Link to={register ? "/login" : "/register"}>
          {register ? "Sign in" : "Create an account"}
        </Link>
      </p>
      {market.live ? (
        <div className="preview-link">
          <span className="sample-label">Demo accounts · password {DEMO_PASSWORD}</span>
          <div>
            {DEMO_ACCOUNTS.map((a) => (
              <span key={a.email} className="muted" style={{ display: "block", fontSize: 13 }}>
                {a.label}: {a.email}
              </span>
            ))}
          </div>
        </div>
      ) : (
        <div className="preview-link">
          <span className="sample-label">Sample workspaces · No sign-in required</span>
          <div>
            {["buyer", "seller", "admin"].map((r) => (
              <Button asChild key={r} variant="outline" size="sm">
                <Link to="/dashboard/$role" params={{ role: r }} search={{ section: "overview" }}>
                  {r.charAt(0).toUpperCase() + r.slice(1)}
                  <ArrowRight />
                </Link>
              </Button>
            ))}
          </div>
        </div>
      )}
    </main>
  );
}
