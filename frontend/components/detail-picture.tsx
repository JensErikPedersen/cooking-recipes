import type { ReactNode } from "react";
import type { Picture as PictureData } from "@/lib/api";
import { Picture } from "@/components/picture";

// The two columns of a recipe's read view, shared by what stands above them: from md a 19 rem
// column on the left - the ingredients' - and the rest.
export const readViewColumns = "md:grid-cols-[minmax(15rem,19rem)_minmax(0,1fr)]";

// A recipe's or an ingredient's details with its picture (docs/PLAN_V7.md, "A recipe's read view"):
// on a phone the picture under the heading, the page's width, the details under it; from md the
// picture heads the left column and the details stand beside it. Without a picture, the details
// alone, as before.
export function WithPicture({ picture, children }: { picture?: PictureData; children: ReactNode }) {
  if (!picture) {
    return children;
  }
  return (
    <div className={`mt-6 grid items-start gap-x-8 ${readViewColumns} md:[&>dl]:mt-0`}>
      <div className="bg-tan p-1.5">
        <Picture picture={picture} place="detail" className="aspect-[4/3] w-full object-cover" />
      </div>
      {children}
    </div>
  );
}
