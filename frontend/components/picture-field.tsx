"use client";

import { useEffect, useId, useRef, useState, type FormEvent } from "react";
import { createPortal } from "react-dom";
import { fieldErrors, pictures, type Picture as PictureData } from "@/lib/api";
import { danish } from "@/lib/sort";
import { None } from "@/components/detail-list";
import { ErrorMessage, FieldError } from "@/components/error-message";
import { ButtonLabel, TickIcon } from "@/components/icons";
import { Loading } from "@/components/loading";
import { Picture } from "@/components/picture";
import { PictureFields, uploadDraft, type PictureDraft } from "@/components/picture-form";
import { iconButton, primaryButton, secondaryButton } from "@/components/styles";

// The picture field of a recipe's or an ingredient's form (docs/PLAN_V7.md, "The picture field on
// a form"): the chosen picture as a thumbnail with its name, Choose and Remove. Choose opens the
// picker; Remove only clears the field. Nothing is saved until the form is.
export function PictureField({
  picture,
  onChange,
  error,
}: {
  picture?: PictureData;
  onChange: (picture?: PictureData) => void;
  error?: string;
}) {
  const labelId = useId();
  const errorId = useId();
  const [picking, setPicking] = useState(false);

  return (
    <div role="group" aria-labelledby={labelId} aria-describedby={error ? errorId : undefined} className="flex flex-col gap-2">
      <span id={labelId} className="font-semibold">
        Picture
      </span>
      {picture ? (
        <div className="flex items-center gap-3">
          <div className="w-[120px] flex-none bg-tan p-1.5">
            <Picture picture={picture} place="thumbnail" className="aspect-[3/2] w-full object-cover" />
          </div>
          <p className="min-w-0 break-words">{picture.name}</p>
        </div>
      ) : (
        <p>
          <None />
        </p>
      )}
      <div className="flex gap-2 md:gap-3">
        <button type="button" onClick={() => setPicking(true)} className={secondaryButton}>
          Choose
        </button>
        {picture && (
          <button type="button" onClick={() => onChange(undefined)} className={`${secondaryButton} ${iconButton}`}>
            <ButtonLabel icon="delete">Remove</ButtonLabel>
          </button>
        )}
      </div>
      {error && <FieldError id={errorId}>{error}</FieldError>}
      {picking && (
        <PicturePicker
          chosenId={picture?.id}
          onChoose={(chosen) => {
            onChange(chosen);
            setPicking(false);
          }}
          onClose={() => setPicking(false)}
        />
      )}
    </div>
  );
}

// The picker (docs/PLAN_V8.md, decision 10): the library's thumbnails to choose from, and Upload
// new, which turns it into the upload form and chooses what it uploads. A modal <dialog>, mounted
// while open. Portalled to the body: it opens from inside the recipe's form, and its upload is a
// form of its own, which may not stand inside another.
function PicturePicker({
  chosenId,
  onChoose,
  onClose,
}: {
  chosenId?: string;
  onChoose: (picture: PictureData) => void;
  onClose: () => void;
}) {
  const dialog = useRef<HTMLDialogElement>(null);
  const titleId = useId();
  const [library, setLibrary] = useState<PictureData[]>();
  const [loadError, setLoadError] = useState<unknown>();
  const [uploading, setUploading] = useState(false);
  const [draft, setDraft] = useState<PictureDraft>({ name: "", altText: "" });
  const [saving, setSaving] = useState(false);
  const [uploadError, setUploadError] = useState<unknown>();
  const errors = fieldErrors(uploadError);

  useEffect(() => {
    dialog.current?.showModal();
    pictures.list().then(setLibrary, setLoadError);
  }, []);

  async function upload(event: FormEvent) {
    event.preventDefault();
    // React carries a submit up its own tree, portal or not: it must not reach the recipe's form.
    event.stopPropagation();
    setSaving(true);
    try {
      onChoose(await uploadDraft(draft));
    } catch (caught) {
      setUploadError(caught);
      setSaving(false);
    }
  }

  return createPortal(
    // Escape closes a modal dialog by itself; onClose follows it.
    <dialog
      ref={dialog}
      aria-labelledby={titleId}
      onClose={onClose}
      className="m-auto max-h-[calc(100dvh-2rem)] w-[min(40rem,calc(100%-2rem))] rounded-md bg-cream p-6 text-olive shadow-[0_10px_30px] shadow-olive/25 backdrop:bg-olive/40"
    >
      {uploading ? (
        <form onSubmit={upload} noValidate>
          <h2 id={titleId} className="text-xl">
            Upload a picture
          </h2>
          <div className="mt-4">
            <PictureFields draft={draft} onChange={setDraft} errors={errors} saving={saving} idPrefix="picker-" />
          </div>
          {uploadError !== undefined && Object.keys(errors).length === 0 && <ErrorMessage error={uploadError} />}
          <div className="mt-6 flex gap-3">
            <button type="submit" disabled={saving} className={primaryButton}>
              Upload
            </button>
            <button type="button" onClick={() => setUploading(false)} disabled={saving} className={secondaryButton}>
              Back
            </button>
          </div>
        </form>
      ) : (
        <>
          <h2 id={titleId} className="text-xl">
            Choose a picture
          </h2>
          <div className="mt-4 flex flex-wrap items-center gap-3">
            <button type="button" onClick={() => setUploading(true)} className={secondaryButton}>
              Upload new
            </button>
            <button type="button" onClick={() => dialog.current?.close()} className={secondaryButton}>
              Cancel
            </button>
            {library?.length === 0 && <p>No pictures yet.</p>}
          </div>
          {loadError ? (
            <ErrorMessage error={loadError} />
          ) : !library ? (
            <Loading />
          ) : (
            library.length > 0 && (
              // Scrolls inside the dialog; the padding keeps the chosen one's ring in view.
              <ul className="mt-4 grid max-h-[55dvh] grid-cols-3 gap-3 overflow-y-auto p-1 md:grid-cols-4">
                {library
                  .toSorted((a, b) => danish(a.name, b.name))
                  .map((picture) => {
                    const chosen = picture.id === chosenId;
                    return (
                      <li key={picture.id}>
                        {/* Named by the picture's name; the thumbnail adds nothing to it. */}
                        <button
                          type="button"
                          aria-pressed={chosen}
                          onClick={() => onChoose(picture)}
                          className="relative flex w-full flex-col text-left"
                        >
                          <span className={`block bg-tan p-1.5 ${chosen ? "outline-3 outline-olive" : ""}`}>
                            <Picture
                              picture={{ id: picture.id, altText: "" }}
                              place="picker"
                              lazy
                              className="aspect-square w-full object-cover"
                            />
                          </span>
                          {chosen && (
                            <span className="absolute top-1 right-1 rounded-full bg-olive p-0.5 text-cream">
                              <TickIcon className="size-5" />
                            </span>
                          )}
                          <span className="mt-1 text-sm font-semibold break-words">{picture.name}</span>
                        </button>
                      </li>
                    );
                  })}
              </ul>
            )
          )}
        </>
      )}
    </dialog>,
    document.body,
  );
}
