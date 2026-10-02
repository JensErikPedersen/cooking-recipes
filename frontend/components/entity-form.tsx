"use client";

import { useState, type FormEvent, type ReactNode } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { fieldErrors, type Audited } from "@/lib/api";
import { DetailList } from "@/components/detail-list";
import { ErrorMessage } from "@/components/error-message";
import { primaryButton, secondaryButton } from "@/components/styles";

// The frame of every create and edit form; the entity's own form supplies only its fields.
//
// After a save it navigates to the read view, which loads the entity with a GET of its own - the
// save response is used for its id and nothing else. A read view that showed the response, or the
// form's own state, would look right even over a backend that dropped a field.
export function EntityForm({
  entity,
  path,
  save,
  children,
}: {
  /** The entity being edited; absent when creating one. */
  entity?: Audited;
  /** The list route, such as "/categories"; the read view is path/id. */
  path: string;
  save: () => Promise<Audited>;
  /** The fields, given the server's errors by field name. */
  children: (errors: Record<string, string>) => ReactNode;
}) {
  const router = useRouter();
  const [error, setError] = useState<unknown>();
  const [saving, setSaving] = useState(false);
  const errors = fieldErrors(error);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    try {
      const saved = await save();
      router.push(`${path}/${saved.id}`);
    } catch (caught) {
      setError(caught);
      setSaving(false);
    }
  }

  return (
    <form onSubmit={onSubmit}>
      {entity && (
        <DetailList
          rows={[
            ["Created", `${entity.created} UTC`],
            ["Created by", entity.createdBy],
          ]}
        />
      )}
      <div className="mt-6 flex flex-col gap-4">{children(errors)}</div>
      {/* An error that belongs to no field - not found, a conflict without a field, a failure. */}
      {error !== undefined && Object.keys(errors).length === 0 && <ErrorMessage error={error} />}
      <div className="mt-6 flex gap-3">
        <button type="submit" disabled={saving} className={primaryButton}>
          Save
        </button>
        <Link href={entity ? `${path}/${entity.id}` : path} className={secondaryButton}>
          Cancel
        </Link>
      </div>
    </form>
  );
}
