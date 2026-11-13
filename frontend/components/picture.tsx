import { PICTURE_SIZES, pictureUrl, type Picture as PictureData } from "@/lib/api";

// Each place a picture appears, with the width it is shown at as `sizes` (docs/PLAN_V7.md,
// "Pictures (version 8)"): the browser multiplies it by the screen's density and fetches the
// smallest of the three files that is wide enough.
export const PICTURE_PLACES = {
  grid: "(min-width: 1024px) 213px, (min-width: 768px) calc(33vw - 47px), calc(50vw - 34px)",
  readView: "(min-width: 1024px) 960px, (min-width: 768px) calc(100vw - 64px), calc(100vw - 32px)",
  thumbnail: "120px",
} as const;

const srcSet = (id: string) =>
  Object.entries(PICTURE_SIZES)
    .map(([size, width]) => `${pictureUrl(id, size as keyof typeof PICTURE_SIZES)} ${width}w`)
    .join(", ");

// A picture from the library: a plain <img> with srcset and sizes, not next/image, whose optimizer
// would fetch the file on the server, without the session cookie (docs/PLAN_V8.md, decision 8).
// The box - aspect ratio, object-fit - is the caller's, by className. srcset states the widths the
// backend aims for; an upload narrower than 1600 px has smaller files than that, which the box
// scales up like any picture.
export function Picture({
  picture,
  place,
  className = "",
  lazy = false,
}: {
  picture: Pick<PictureData, "id" | "altText">;
  place: keyof typeof PICTURE_PLACES;
  className?: string;
  /** Thumbnails and cards load as they come into view. */
  lazy?: boolean;
}) {
  return (
    // eslint-disable-next-line @next/next/no-img-element -- see above
    <img
      src={pictureUrl(picture.id, "medium")}
      srcSet={srcSet(picture.id)}
      sizes={PICTURE_PLACES[place]}
      alt={picture.altText}
      loading={lazy ? "lazy" : undefined}
      className={className}
    />
  );
}
