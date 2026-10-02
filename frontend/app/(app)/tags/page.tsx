"use client";

import { tags } from "@/lib/api";
import { EntityList } from "@/components/entity-list";

export default function TagsPage() {
  return <EntityList title="Tags" path="/tags" newLabel="New tag" load={tags.list} />;
}
