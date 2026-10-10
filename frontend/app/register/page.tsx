"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { auth, fieldErrors } from "@/lib/api";
import { ErrorMessage } from "@/components/error-message";
import { TextField } from "@/components/text-field";
import { primaryButton } from "@/components/styles";

// A new account, for a visitor who has none. The backend checks every rule - the confirmation
// included - and signs the new account in, so success goes straight to the Welcome page.
export default function RegisterPage() {
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<unknown>();
  const [saving, setSaving] = useState(false);
  const errors = fieldErrors(error);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    try {
      await auth.register(username, password, confirmPassword);
      router.push("/");
    } catch (caught) {
      setError(caught);
      setSaving(false);
    }
  }

  return (
    <main className="mx-auto mt-24 w-full max-w-sm px-8">
      <h1 className="text-2xl font-bold">Create an account</h1>
      <form onSubmit={onSubmit} className="mt-6 flex flex-col gap-4">
        <TextField
          label="Username"
          name="username"
          value={username}
          onChange={setUsername}
          error={errors.username}
          autoComplete="username"
        />
        <TextField
          label="Password"
          name="password"
          value={password}
          onChange={setPassword}
          error={errors.password}
          type="password"
          autoComplete="new-password"
        />
        <TextField
          label="Confirm password"
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
          Register
        </button>
      </form>
      <p className="mt-6 text-sm">
        <Link href="/login" className="underline">
          Back to sign in
        </Link>
      </p>
    </main>
  );
}
