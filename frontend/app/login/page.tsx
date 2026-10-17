"use client";

import { Suspense, useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { ApiError, auth } from "@/lib/api";
import { afterSignIn } from "@/lib/return-to";
import { useHydrated } from "@/lib/use-hydrated";

export default function LoginPage() {
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const hydrated = useHydrated();

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    try {
      await auth.login(username, password);
      // Read here rather than with useSearchParams: this runs only in the browser, so the page
      // stays prerendered without a Suspense boundary.
      router.push(afterSignIn(new URLSearchParams(window.location.search).get("next")));
    } catch (caught) {
      // The backend answers a wrong password and an unknown user alike, on purpose.
      setError(caught instanceof ApiError ? caught.message : "Sign in failed. Please try again.");
    }
  }

  return (
    <main className="mx-auto mt-24 w-full max-w-sm px-8">
      <h1 className="text-2xl font-bold">Sign in</h1>
      {/* Only the notice waits for the URL; the rest of the page stays prerendered. */}
      <Suspense>
        <ResetNotice />
      </Suspense>
      <form onSubmit={onSubmit} className="mt-6">
        <fieldset disabled={!hydrated} className="flex min-w-0 flex-col gap-4">
          <label className="flex flex-col gap-1">
            Username
            <input
              name="username"
              autoComplete="username"
              required
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              className="rounded border px-3 py-2"
            />
          </label>
          <label className="flex flex-col gap-1">
            Password
            <input
              name="password"
              type="password"
              autoComplete="current-password"
              required
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="rounded border px-3 py-2"
            />
          </label>
          {error && (
            <p role="alert" className="text-sm text-red-700">
              {error}
            </p>
          )}
          <button type="submit" className="rounded bg-gray-900 px-3 py-2 text-white hover:bg-gray-700">
            Sign in
          </button>
        </fieldset>
      </form>
      <p className="mt-6 text-sm">
        <Link href="/forgot-password" className="underline">
          Forgot your password?
        </Link>
      </p>
      <p className="mt-2 text-sm">
        No account yet?{" "}
        <Link href="/register" className="underline">
          Create an account
        </Link>
      </p>
    </main>
  );
}

// After a reset, from /reset-password, which comes here with ?reset. useSearchParams, not
// window.location, which a client-side navigation updates only after rendering the new page.
function ResetNotice() {
  return useSearchParams().has("reset") ? (
    <p role="status" className="mt-6">
      Your password has been reset. Sign in with the new one.
    </p>
  ) : null;
}
