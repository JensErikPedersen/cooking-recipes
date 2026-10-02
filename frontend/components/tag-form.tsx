"use client";

import { useState } from "react";
import { tags, type Tag } from "@/lib/api";
import { EntityForm } from "@/components/entity-form";
import { TextField } from "@/components/text-field";

// Create and edit alike; see CategoryForm.
export function TagForm({ tag }: { tag?: Tag }) {
  const [name, setName] = useState(tag?.name ?? "");

  function save() {
    return tag ? tags.update(tag.id, { name }) : tags.create({ name });
  }

  return (
    <EntityForm entity={tag} path="/tags" save={save}>
      {(errors) => <TextField label="Name" name="name" value={name} onChange={setName} error={errors.name} />}
    </EntityForm>
  );
}
