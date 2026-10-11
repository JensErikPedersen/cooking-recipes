"use client";

import { useState, type FormEvent } from "react";
import { auth, fieldErrors } from "@/lib/api";
import { ErrorMessage } from "@/components/error-message";
import { PageHeader } from "@/components/page-header";
import { TextField } from "@/components/text-field";
import { primaryButton } from "@/components/styles";

// The signed-in user's own password. The backend checks every rule, the current password and the
// confirmation included. There is no read view to go to afterwards, so the page stays, empties its
// fields and says the change was made; the session stays signed in.
export default function ChangePasswordPage() {
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
    <>
      <PageHeader title="Change password" />
      <form onSubmit={onSubmit} className="mt-6 flex max-w-sm flex-col gap-4">
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
    </>
  );
}
