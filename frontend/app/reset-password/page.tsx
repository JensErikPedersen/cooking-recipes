"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { auth, fieldErrors } from "@/lib/api";
import { ErrorMessage } from "@/components/error-message";
import { TextField } from "@/components/text-field";
import { primaryButton } from "@/components/styles";
import { useHydrated } from "@/lib/use-hydrated";

// Where the link in a reset mail leads: a new password for the account, with the token from the
// link. The backend checks the token and every rule. Success signs no one in - the new password
// does that, on the sign-in page - and a token that no longer works leads back to ask for a new one.
export default function ResetPasswordPage() {
  const router = useRouter();
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<unknown>();
  const [saving, setSaving] = useState(false);
  const errors = fieldErrors(error);
  const hydrated = useHydrated();

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    try {
      // Read here rather than with useSearchParams, as the login page reads ?next=: this runs only
      // in the browser, so the page stays prerendered without a Suspense boundary.
      const token = new URLSearchParams(window.location.search).get("token") ?? "";
      await auth.confirmPasswordReset(token, newPassword, confirmPassword);
      router.push("/login?reset");
    } catch (caught) {
      setError(caught);
      setSaving(false);
    }
  }

  return (
    <main className="mx-auto mt-24 w-full max-w-sm px-8">
      <h1 className="text-2xl font-bold">Choose a new password</h1>
      <form onSubmit={onSubmit} className="mt-6">
        <fieldset disabled={!hydrated} className="flex min-w-0 flex-col gap-4">
          {/* The link's own problem: unknown, expired or used. No field shows it; the way on does. */}
          {errors.token && (
            <p role="alert" className="text-red-700">
              {errors.token}.{" "}
              <Link href="/forgot-password" className="underline">
                Ask for a new link
              </Link>
            </p>
          )}
          <TextField
            label="New password"
            name="newPassword"
            value={newPassword}
            onChange={setNewPassword}
            error={errors.newPassword}
            type="password"
            autoComplete="new-password"
          />
          <TextField
            label="Confirm new password"
            name="confirmPassword"
            value={confirmPassword}
            onChange={setConfirmPassword}
            error={errors.confirmPassword}
            type="password"
            autoComplete="new-password"
          />
          {/* An error that belongs to no field - a failure rather than a rule. */}
          {error !== undefined && Object.keys(errors).length === 0 && <ErrorMessage error={error} />}
          <button type="submit" disabled={saving} className={primaryButton}>
            Set new password
          </button>
        </fieldset>
      </form>
    </main>
  );
}
