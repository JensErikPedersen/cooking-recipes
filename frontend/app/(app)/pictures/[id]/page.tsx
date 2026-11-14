"use client";

import { use } from "react";
import Link from "next/link";
import { pictures } from "@/lib/api";
import { formatDateTime } from "@/lib/date";
import { danish } from "@/lib/sort";
import { DeleteButton } from "@/components/delete-button";
import { DetailList } from "@/components/detail-list";
import { EntityLoader } from "@/components/entity-loader";
import { PageHeader } from "@/components/page-header";
import { Picture } from "@/components/picture";
import { ButtonLabel } from "@/components/icons";
import { iconButton, secondaryButton, textLink } from "@/components/styles";

// Each a link to where the picture is shown; undefined for none, which DetailList reads as "None".
function uses(path: string, list: { id: string; name: string }[]) {
  if (list.length === 0) {
    return undefined;
  }
  return (
    <ul className="flex flex-wrap gap-x-4 gap-y-1">
      {list.toSorted((a, b) => danish(a.name, b.name)).map((use) => (
        <li key={use.id}>
          <Link href={`${path}/${use.id}`} className={textLink}>
            {use.name}
          </Link>
        </li>
      ))}
    </ul>
  );
}

export default function PicturePage({ params }: PageProps<"/pictures/[id]">) {
  const { id } = use(params);

  return (
    <EntityLoader id={id} get={pictures.get} notFoundTitle="Picture not found" listHref="/pictures" listLabel="Back to pictures">
      {(picture) => (
        <>
          <PageHeader title={picture.name}>
            <Link href={`/pictures/${picture.id}/edit`} className={`${secondaryButton} ${iconButton}`}>
              <ButtonLabel icon="edit">Edit</ButtonLabel>
            </Link>
            <DeleteButton what="picture" name={picture.name} remove={() => pictures.remove(picture.id)} listHref="/pictures" />
          </PageHeader>
          {/* Whole, in its band, at most 70% of the window's height. */}
          <div className="mt-6 w-fit max-w-full bg-tan p-1.5">
            <Picture picture={picture} place="readView" className="block max-h-[70vh] max-w-full" />
          </div>
          <DetailList
            rows={[
              ["Name", picture.name],
              ["Alt text", picture.altText],
              ["Recipes", uses("/recipes", picture.recipes)],
              ["Ingredients", uses("/ingredients", picture.ingredients)],
              ["Welcome page", picture.welcomePage && "Shown there"],
              ["Created", formatDateTime(picture.created)],
              ["Created by", picture.createdBy],
              ["Updated", picture.updated && formatDateTime(picture.updated)],
              ["Updated by", picture.updatedBy],
            ]}
          />
        </>
      )}
    </EntityLoader>
  );
}
