"use client";

import { units } from "@/lib/api";
import { EntityList } from "@/components/entity-list";

export default function UnitsPage() {
  return (
    <EntityList
      title="Units"
      path="/units"
      newLabel="New unit"
      load={units.list}
      columns={[["Label", (unit) => unit.label]]}
    />
  );
}
