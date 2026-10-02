"use client";

import { useEffect, useState, type ReactNode } from "react";
import Link from "next/link";
import { ErrorMessage } from "@/components/error-message";
import { Loading } from "@/components/loading";
import { PageHeader } from "@/components/page-header";
import { primaryButton } from "@/components/styles";
import { danish } from "@/lib/sort";

// The list page of an entity type: a New button, then a table that starts with the name, linked to
// the read view and sorted by it, the Danish way. The API returns rows in storage order.
export function EntityList<T extends { id: string; name: string }>({
  title,
  path,
  newLabel,
  load,
  columns = [],
}: {
  title: string;
  /** The list route, such as "/units"; the read view is path/id. */
  path: string;
  newLabel: string;
  load: () => Promise<T[]>;
  /** The columns after Name. */
  columns?: [header: string, value: (entity: T) => ReactNode][];
}) {
  const [list, setList] = useState<T[]>();
  const [error, setError] = useState<unknown>();

  useEffect(() => {
    load().then(setList, setError);
  }, [load]);

  return (
    <>
      <PageHeader title={title}>
        <Link href={`${path}/new`} className={primaryButton}>
          {newLabel}
        </Link>
      </PageHeader>
      {error ? (
        <ErrorMessage error={error} />
      ) : !list ? (
        <Loading />
      ) : list.length === 0 ? (
        <p className="mt-6">No {title.toLowerCase()} yet.</p>
      ) : (
        <table className="mt-6 w-full text-left">
          <thead className="border-b">
            <tr>
              <th className="py-2 pr-8">Name</th>
              {columns.map(([header]) => (
                <th key={header} className="py-2 pr-8">
                  {header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {list
              .toSorted((a, b) => danish(a.name, b.name))
              .map((entity) => (
                <tr key={entity.id} className="border-b">
                  <td className="py-2 pr-8">
                    <Link href={`${path}/${entity.id}`} className="underline hover:no-underline">
                      {entity.name}
                    </Link>
                  </td>
                  {columns.map(([header, value]) => (
                    <td key={header} className="py-2 pr-8">
                      {value(entity)}
                    </td>
                  ))}
                </tr>
              ))}
          </tbody>
        </table>
      )}
    </>
  );
}
