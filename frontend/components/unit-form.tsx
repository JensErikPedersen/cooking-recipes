"use client";

import { useState } from "react";
import { units, type Unit } from "@/lib/api";
import { EntityForm } from "@/components/entity-form";
import { TextField } from "@/components/text-field";

// Create and edit alike; see CategoryForm. Both fields are required, which the server reports.
export function UnitForm({ unit }: { unit?: Unit }) {
  const [name, setName] = useState(unit?.name ?? "");
  const [label, setLabel] = useState(unit?.label ?? "");

  function save() {
    const input = { name, label };
    return unit ? units.update(unit.id, input) : units.create(input);
  }

  return (
    <EntityForm entity={unit} path="/units" save={save}>
      {(errors) => (
        <>
          <TextField label="Name" name="name" value={name} onChange={setName} error={errors.name} />
          <TextField label="Label" name="label" value={label} onChange={setLabel} error={errors.label} />
        </>
      )}
    </EntityForm>
  );
}
