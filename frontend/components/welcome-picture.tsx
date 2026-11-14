"use client";

import { useEffect, useState, type ReactNode } from "react";
import { welcomePage, type Picture as PictureData } from "@/lib/api";
import { ErrorMessage } from "@/components/error-message";
import { ButtonLabel } from "@/components/icons";
import { Picture } from "@/components/picture";
import { PicturePicker } from "@/components/picture-field";
import { iconButton, secondaryButton } from "@/components/styles";

// The Welcome page's introduction with its picture (docs/PLAN_V7.md, "The Welcome page"): from md
// the text on the left and the picture 18 rem wide on the right, in its band; under the text on a
// phone. Choose and Remove under it save at once - the page has no Save. Without a picture the
// introduction stands as before, with Choose under it.
export function WelcomeIntro({ children }: { children: ReactNode }) {
  // undefined until loaded, null for none: the page is complete without it, so nothing shows meanwhile.
  const [picture, setPicture] = useState<PictureData | null>();
  const [picking, setPicking] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<unknown>();

  useEffect(() => {
    welcomePage.get().then((page) => setPicture(page.picture ?? null), setError);
  }, []);

  async function save(chosen?: PictureData) {
    setPicking(false);
    setSaving(true);
    setError(undefined);
    try {
      setPicture((await welcomePage.update(chosen)).picture ?? null);
    } catch (caught) {
      setError(caught);
    }
    setSaving(false);
  }

  return (
    <div className={picture ? "md:grid md:grid-cols-[minmax(0,1fr)_18rem] md:items-start md:gap-x-8" : undefined}>
      <div>{children}</div>
      {picture !== undefined && (
        <div role="group" aria-label="Welcome picture" className={`flex flex-col gap-3 ${picture ? "mt-6 md:mt-0" : "mt-4"}`}>
          {picture && (
            <div className="bg-tan p-1.5">
              <Picture picture={picture} place="welcome" className="aspect-[4/3] w-full object-cover" />
            </div>
          )}
          <div className="flex gap-2 md:gap-3">
            <button type="button" onClick={() => setPicking(true)} disabled={saving} className={secondaryButton}>
              {picture ? "Choose" : "Choose a picture"}
            </button>
            {picture && (
              <button type="button" onClick={() => save()} disabled={saving} className={`${secondaryButton} ${iconButton}`}>
                <ButtonLabel icon="delete">Remove</ButtonLabel>
              </button>
            )}
          </div>
        </div>
      )}
      {error !== undefined && <ErrorMessage error={error} />}
      {picking && <PicturePicker chosenId={picture?.id} onChoose={save} onClose={() => setPicking(false)} />}
    </div>
  );
}
