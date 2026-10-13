"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { auth, fieldErrors } from "@/lib/api";
import { ErrorMessage } from "@/components/error-message";
import { TextField } from "@/components/text-field";
import { primaryButton } from "@/components/styles";
import { useHydrated } from "@/lib/use-hydrated";

// Asks for a reset link by email, for a visitor who cannot sign in. The backend answers alike
// whether or not an account has the address, and so does this page: the same message, whatever was
// typed. noValidate, as on the register page: the server's message for a malformed address, not
// the browser's.
export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [error, setError] = useState<unknown>();
  const [saving, setSaving] = useState(false);
  const [sent, setSent] = useState(false);
  const errors = fieldErrors(error);
  const hydrated = useHydrated();

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    try {
      await auth.requestPasswordReset(email);
      setSent(true);
    } catch (caught) {
      setError(caught);
      setSaving(false);
    }
  }

  return (
    <main className="mx-auto mt-24 w-full max-w-sm px-8">
      <h1 className="text-2xl font-bold">Forgot your password?</h1>
      {sent ? (
        <p role="status" className="mt-6">
          If an account has that address, a mail with a link to choose a new password is on its way.
        </p>
      ) : (
        <form onSubmit={onSubmit} noValidate className="mt-6">
          <fieldset disabled={!hydrated} className="flex min-w-0 flex-col gap-4">
            <p>Enter your account&apos;s email address, and you will get a link to choose a new password.</p>
            <TextField
              label="Email address"
              name="email"
              value={email}
              onChange={setEmail}
              error={errors.email}
              type="email"
              autoComplete="email"
            />
            {/* An error that belongs to no field - a failure rather than a rule. */}
            {error !== undefined && Object.keys(errors).length === 0 && <ErrorMessage error={error} />}
            <button type="submit" disabled={saving} className={primaryButton}>
              Send reset link
            </button>
          </fieldset>
        </form>
      )}
      <p className="mt-6 text-sm">
        <Link href="/login" className="underline">
          Back to sign in
        </Link>
      </p>
    </main>
  );
}
