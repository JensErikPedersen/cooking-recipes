"use client";

import { use } from "react";
import { pictures } from "@/lib/api";
import { EntityLoader } from "@/components/entity-loader";
import { PageHeader } from "@/components/page-header";
import { PictureForm } from "@/components/picture-form";

export default function EditPicturePage({ params }: PageProps<"/pictures/[id]/edit">) {
  const { id } = use(params);

  return (
    <EntityLoader id={id} get={pictures.get} notFoundTitle="Picture not found" listHref="/pictures" listLabel="Back to pictures">
      {(picture) => (
        <>
          <PageHeader title="Edit picture" />
          <PictureForm picture={picture} />
        </>
      )}
    </EntityLoader>
  );
}
