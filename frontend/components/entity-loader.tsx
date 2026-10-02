"use client";

import { useEffect, useState, type ReactNode } from "react";
import { ApiError } from "@/lib/api";
import { ErrorMessage } from "@/components/error-message";
import { NotFound } from "@/components/not-found";

// Loads one entity for a read view or an edit form, and renders it - or "not found" for a 404, the
// error for any other failure, and nothing while it loads.
export function EntityLoader<T>({
  id,
  get,
  notFoundTitle,
  listHref,
  listLabel,
  children,
}: {
  id: string;
  get: (id: string) => Promise<T>;
  notFoundTitle: string;
  listHref: string;
  listLabel: string;
  children: (entity: T) => ReactNode;
}) {
  const [entity, setEntity] = useState<T>();
  const [error, setError] = useState<unknown>();

  useEffect(() => {
    get(id).then(setEntity, setError);
  }, [get, id]);

  if (error instanceof ApiError && error.status === 404) {
    return <NotFound title={notFoundTitle} listHref={listHref} listLabel={listLabel} />;
  }
  if (error) {
    return <ErrorMessage error={error} />;
  }
  return entity === undefined ? null : children(entity);
}
