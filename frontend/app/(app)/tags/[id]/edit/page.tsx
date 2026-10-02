"use client";

import { use } from "react";
import { tags } from "@/lib/api";
import { EntityLoader } from "@/components/entity-loader";
import { PageHeader } from "@/components/page-header";
import { TagForm } from "@/components/tag-form";

export default function EditTagPage({ params }: PageProps<"/tags/[id]/edit">) {
  const { id } = use(params);

  return (
    <EntityLoader id={id} get={tags.get} notFoundTitle="Tag not found" listHref="/tags" listLabel="Back to tags">
      {(tag) => (
        <>
          <PageHeader title="Edit tag" />
          <TagForm tag={tag} />
        </>
      )}
    </EntityLoader>
  );
}
