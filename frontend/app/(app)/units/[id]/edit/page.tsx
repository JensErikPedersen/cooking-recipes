"use client";

import { use } from "react";
import { units } from "@/lib/api";
import { EntityLoader } from "@/components/entity-loader";
import { PageHeader } from "@/components/page-header";
import { UnitForm } from "@/components/unit-form";

export default function EditUnitPage({ params }: PageProps<"/units/[id]/edit">) {
  const { id } = use(params);

  return (
    <EntityLoader id={id} get={units.get} notFoundTitle="Unit not found" listHref="/units" listLabel="Back to units">
      {(unit) => (
        <>
          <PageHeader title="Edit unit" />
          <UnitForm unit={unit} />
        </>
      )}
    </EntityLoader>
  );
}
