"use client";

import { useEffect, useState, type FormEvent, type ReactNode } from "react";
import { auth, fieldErrors } from "@/lib/api";
import { ErrorMessage } from "@/components/error-message";
import { PageHeader } from "@/components/page-header";
import { TextField } from "@/components/text-field";
import { primaryButton } from "@/components/styles";

// The signed-in user's own account: its email address and its password, each changed with the
// current password. The backend checks every rule. There is no read view to go to afterwards, so
// each form stays, empties its fields and says the change was made; the session stays signed in.
export default function AccountPage() {
  return (
    <>
      <PageHeader title="Your account" />
      <EmailSection />
      <PasswordSection />
    </>
  );
}

function Section({ id, title, children }: { id: string; title: string; children: ReactNode }) {
  return (
    <section aria-labelledby={id} className="mt-8 max-w-sm">
      <h2 id={id} className="text-lg font-semibold">
        {title}
      </h2>
      {children}
    </section>
  );
}

// The address in use is asked for here, not taken from AppShell: AppShell asks once per visit, and
// would still show the old address after a change.
function EmailSection() {
  const [inUse, setInUse] = useState<string | null>();
  const [email, setEmail] = useState("");
  const [currentPassword, setCurrentPassword] = useState("");
  const [error, setError] = useState<unknown>();
  const [saving, setSaving] = useState(false);
  const [changed, setChanged] = useState(false);
  const errors = fieldErrors(error);

  useEffect(() => {
    auth.me().then((user) => setInUse(user.email), setError);
  }, []);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setChanged(false);
    try {
      await auth.changeEmail(currentPassword, email);
      setError(undefined);
      setInUse(email);
      setEmail("");
      setCurrentPassword("");
      setChanged(true);
    } catch (caught) {
      setError(caught);
    }
    setSaving(false);
  }

  return (
    <Section id="email-heading" title="Email address">
      {inUse !== undefined && (
        <p className="mt-2">
          {inUse === null ? (
            "None yet. Without one, a forgotten password cannot be reset."
          ) : (
            <>
              In use: <strong>{inUse}</strong>
            </>
          )}
        </p>
      )}
      {/* noValidate: the browser's own check of an email field would answer before the server. */}
      <form onSubmit={onSubmit} noValidate className="mt-4 flex flex-col gap-4">
        <TextField
          label="New email address"
          name="email"
          value={email}
          onChange={setEmail}
          error={errors.email}
          type="email"
          autoComplete="email"
        />
        <TextField
          label="Current password"
          name="emailCurrentPassword"
          value={currentPassword}
          onChange={setCurrentPassword}
          error={errors.currentPassword}
          type="password"
          autoComplete="current-password"
        />
        {/* An error that belongs to no field - a failure rather than a rule. */}
        {error !== undefined && Object.keys(errors).length === 0 && <ErrorMessage error={error} />}
        {changed && <p role="status">Your email address has been changed.</p>}
        <button type="submit" disabled={saving} className={primaryButton}>
          Change email address
        </button>
      </form>
    </Section>
  );
}

function PasswordSection() {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<unknown>();
  const [saving, setSaving] = useState(false);
  const [changed, setChanged] = useState(false);
  const errors = fieldErrors(error);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setChanged(false);
    try {
      await auth.changePassword(currentPassword, newPassword, confirmPassword);
      setError(undefined);
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setChanged(true);
    } catch (caught) {
      setError(caught);
    }
    setSaving(false);
  }

  return (
    <Section id="password-heading" title="Password">
      <form onSubmit={onSubmit} className="mt-4 flex flex-col gap-4">
        <TextField
          label="Current password"
          name="currentPassword"
          value={currentPassword}
          onChange={setCurrentPassword}
          error={errors.currentPassword}
          type="password"
          autoComplete="current-password"
        />
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
        {changed && <p role="status">Your password has been changed.</p>}
        <button type="submit" disabled={saving} className={primaryButton}>
          Change password
        </button>
      </form>
    </Section>
  );
}
