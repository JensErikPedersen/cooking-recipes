"use client";

import { useState, type ChangeEvent } from "react";
import { pictures, type Picture } from "@/lib/api";
import { EntityForm } from "@/components/entity-form";
import { FieldError } from "@/components/error-message";
import { TextField } from "@/components/text-field";
import { secondaryButton } from "@/components/styles";

const formatSize = (bytes: number) =>
  bytes >= 1024 * 1024 ? `${(bytes / 1024 / 1024).toFixed(1)} MB` : `${Math.ceil(bytes / 1024)} kB`;

// The file input, visually hidden behind its label drawn as a button: a native input keeps the
// browser's own picker, the phone's camera and photo library included. A chosen file is shown from
// the browser before it is sent; a refused one stays shown under the server's message, so another
// can be chosen (docs/PLAN_V7.md, "The upload form").
function PictureFileField({
  file,
  onChange,
  error,
}: {
  file?: File;
  onChange: (file: File) => void;
  error?: string;
}) {
  const [preview, setPreview] = useState<string>();
  // A file the browser cannot draw - one the server will refuse - is shown by its name alone.
  const [drawable, setDrawable] = useState(true);

  function choose(event: ChangeEvent<HTMLInputElement>) {
    const chosen = event.target.files?.[0];
    // Cancelling the browser's picker keeps the file already chosen.
    if (!chosen) {
      return;
    }
    if (preview) {
      URL.revokeObjectURL(preview);
    }
    setPreview(URL.createObjectURL(chosen));
    setDrawable(true);
    onChange(chosen);
  }

  return (
    <div className="flex flex-col gap-2">
      <span className="font-semibold">Picture</span>
      {file && preview && (
        <div className="flex items-center gap-3">
          {drawable && (
            <div className="w-[120px] flex-none bg-tan p-1.5">
              {/* eslint-disable-next-line @next/next/no-img-element -- a local file, not yet a picture of the library */}
              <img src={preview} alt="" onError={() => setDrawable(false)} className="aspect-[3/2] w-full object-cover" />
            </div>
          )}
          <p className="min-w-0 break-words">
            {file.name}
            <br />
            {formatSize(file.size)}
          </p>
        </div>
      )}
      <label
        className={`${secondaryButton} w-fit cursor-pointer has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-olive`}
      >
        {file ? "Choose another file" : "Choose a file"}
        <input
          id="file"
          name="file"
          type="file"
          accept="image/jpeg,image/png"
          onChange={choose}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? "file-hint file-error" : "file-hint"}
          className="sr-only"
        />
      </label>
      <p id="file-hint" className="text-sm">
        JPEG or PNG, up to 10 MB.
      </p>
      {error && <FieldError id="file-error">{error}</FieldError>}
    </div>
  );
}

// New is the upload - a file, a name and an alt text; edit changes the name and the alt text only,
// as a picture's file never changes (docs/PLAN_V8.md, decisions 3 and 7).
export function PictureForm({ picture }: { picture?: Picture }) {
  const [file, setFile] = useState<File>();
  const [name, setName] = useState(picture?.name ?? "");
  const [altText, setAltText] = useState(picture?.altText ?? "");

  function save() {
    const input = { name, altText };
    return picture ? pictures.update(picture.id, input) : pictures.upload(file, input);
  }

  return (
    <EntityForm entity={picture} path="/pictures" save={save}>
      {(errors, saving) => (
        <fieldset disabled={saving} className="flex min-w-0 flex-col gap-4">
          {!picture && <PictureFileField file={file} onChange={setFile} error={errors.file} />}
          <TextField label="Name" name="name" value={name} onChange={setName} error={errors.name} />
          <TextField
            label="Alt text"
            name="altText"
            value={altText}
            onChange={setAltText}
            error={errors.altText}
            hint="What a screen reader says instead of the picture."
          />
          {/* fetch reports no progress, so a line rather than a bar. */}
          {saving && file && <p role="status">Uploading {file.name}</p>}
        </fieldset>
      )}
    </EntityForm>
  );
}
