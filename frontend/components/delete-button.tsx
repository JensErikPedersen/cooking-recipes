"use client";

import { useId, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { ErrorMessage } from "@/components/error-message";
import { ButtonLabel } from "@/components/icons";
import { iconButton, primaryButton, secondaryButton } from "@/components/styles";

// Delete, confirmed in a native <dialog> on the page - never window.confirm, which blocks the page
// and is the browser's, not the application's. When the server refuses, as for a category recipes
// still use, its reason is shown in the dialog. After a delete it goes to the list.
export function DeleteButton({
  what,
  name,
  remove,
  listHref,
}: {
  /** The kind of entity, such as "category". */
  what: string;
  name: string;
  remove: () => Promise<void>;
  listHref: string;
}) {
  const router = useRouter();
  const dialog = useRef<HTMLDialogElement>(null);
  const titleId = useId();
  const [error, setError] = useState<unknown>();
  const [deleting, setDeleting] = useState(false);

  function open() {
    setError(undefined);
    dialog.current?.showModal();
  }

  async function confirm() {
    setDeleting(true);
    try {
      await remove();
      router.push(listHref);
    } catch (caught) {
      setError(caught);
      setDeleting(false);
    }
  }

  return (
    <>
      <button type="button" onClick={open} className={`${secondaryButton} ${iconButton}`}>
        <ButtonLabel icon="delete">Delete</ButtonLabel>
      </button>
      {/* m-auto: Tailwind's preflight zeroes the margin the browser centres a modal dialog with. The
          buttons keep their words: this is where the choice is confirmed. */}
      <dialog
        ref={dialog}
        aria-labelledby={titleId}
        className="m-auto max-w-[min(28rem,calc(100%-2rem))] rounded-md bg-cream p-6 text-olive shadow-[0_10px_30px] shadow-olive/25 backdrop:bg-olive/40"
      >
        <h2 id={titleId} className="text-xl">
          Delete {what} &quot;{name}&quot;?
        </h2>
        {error === undefined ? (
          <>
            <p className="mt-2">This cannot be undone.</p>
            <div className="mt-6 flex gap-3">
              <button type="button" onClick={confirm} disabled={deleting} className={primaryButton}>
                Delete
              </button>
              <button type="button" onClick={() => dialog.current?.close()} className={secondaryButton}>
                Cancel
              </button>
            </div>
          </>
        ) : (
          // Refused: offering Delete again would only repeat the refusal.
          <>
            <ErrorMessage error={error} />
            <div className="mt-6 flex gap-3">
              <button type="button" onClick={() => dialog.current?.close()} className={primaryButton}>
                OK
              </button>
            </div>
          </>
        )}
      </dialog>
    </>
  );
}
