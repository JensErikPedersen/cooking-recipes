"use client";

import { use } from "react";
import Link from "next/link";
import { tags } from "@/lib/api";
import { DeleteButton } from "@/components/delete-button";
import { DetailList } from "@/components/detail-list";
import { EntityLoader } from "@/components/entity-loader";
import { PageHeader } from "@/components/page-header";
import { secondaryButton } from "@/components/styles";

export default function TagPage({ params }: PageProps<"/tags/[id]">) {
  const { id } = use(params);

  return (
    <EntityLoader id={id} get={tags.get} notFoundTitle="Tag not found" listHref="/tags" listLabel="Back to tags">
      {(tag) => (
        <>
          <PageHeader title={tag.name}>
            <Link href={`/tags/${tag.id}/edit`} className={secondaryButton}>
              Edit
            </Link>
            <DeleteButton what="tag" name={tag.name} remove={() => tags.remove(tag.id)} listHref="/tags" />
          </PageHeader>
          <DetailList rows={[["Name", tag.name]]} />
        </>
      )}
    </EntityLoader>
  );
}
