"use client";

import { pictures } from "@/lib/api";
import { EntityList } from "@/components/entity-list";
import { Picture } from "@/components/picture";

export default function PicturesPage() {
  return (
    <EntityList
      title="Pictures"
      path="/pictures"
      newLabel="New picture"
      load={pictures.list}
      tile={(picture) => <Picture picture={picture} place="grid" lazy className="aspect-square w-full object-cover" />}
    />
  );
}
