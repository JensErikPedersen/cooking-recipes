"use client";

import { use } from "react";
import Link from "next/link";
import { units } from "@/lib/api";
import { DeleteButton } from "@/components/delete-button";
import { DetailList } from "@/components/detail-list";
import { EntityLoader } from "@/components/entity-loader";
import { PageHeader } from "@/components/page-header";
import { secondaryButton } from "@/components/styles";

export default function UnitPage({ params }: PageProps<"/units/[id]">) {
  const { id } = use(params);

  return (
    <EntityLoader id={id} get={units.get} notFoundTitle="Unit not found" listHref="/units" listLabel="Back to units">
      {(unit) => (
        <>
          <PageHeader title={unit.name}>
            <Link href={`/units/${unit.id}/edit`} className={secondaryButton}>
              Edit
            </Link>
            <DeleteButton what="unit" name={unit.name} remove={() => units.remove(unit.id)} listHref="/units" />
          </PageHeader>
          <DetailList
            rows={[
              ["Name", unit.name],
              ["Label", unit.label],
            ]}
          />
        </>
      )}
    </EntityLoader>
  );
}
