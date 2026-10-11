"use client";

import { useEffect, useState, type FormEvent, type ReactNode } from "react";
import Link from "next/link";
import { ErrorMessage } from "@/components/error-message";
import { Loading } from "@/components/loading";
import { PageHeader } from "@/components/page-header";
import { primaryButton, secondaryButton } from "@/components/styles";
import { danish } from "@/lib/sort";

// The list page of an entity type: a New button, then a table that starts with the name, linked to
// the read view and sorted by it, the Danish way. The API returns rows in storage order.
//
// A searchable list has a field above the table. Searching asks the API with the term, which
// answers the names containing it in any case; an empty field asks for everything again. The page
// never filters what it already has: the API's filter is the one the user gets.
export function EntityList<T extends { id: string; name: string }>({
  title,
  path,
  newLabel,
  load,
  searchable = false,
  columns = [],
}: {
  title: string;
  /** The list route, such as "/units"; the read view is path/id. */
  path: string;
  newLabel: string;
  /** All of them, or with a name only those whose name contains it. */
  load: (name?: string) => Promise<T[]>;
  searchable?: boolean;
  /** The columns after Name. */
  columns?: [header: string, value: (entity: T) => ReactNode][];
}) {
  const [list, setList] = useState<T[]>();
  const [error, setError] = useState<unknown>();
  const [term, setTerm] = useState("");
  /** The term the list shows the result of; undefined for everything. */
  const [searched, setSearched] = useState<string>();

  // A response for a term no longer searched is dropped: the whole list, asked for on arrival, can
  // come back after a search made before it did.
  useEffect(() => {
    let current = true;
    load(searched).then(
      (result) => current && setList(result),
      (failure) => current && setError(failure),
    );
    return () => {
      current = false;
    };
  }, [load, searched]);

  function search(event: FormEvent) {
    event.preventDefault();
    const name = term === "" ? undefined : term;
    // The same term again changes nothing; the list already shows its result.
    if (name !== searched) {
      setList(undefined);
      setError(undefined);
      setSearched(name);
    }
  }

  return (
    <>
      <PageHeader title={title}>
        <Link href={`${path}/new`} className={primaryButton}>
          {newLabel}
        </Link>
      </PageHeader>
      {searchable && (
        <form role="search" onSubmit={search} className="mt-6 flex max-w-md gap-2">
          <label htmlFor="search" className="sr-only">
            Search by name
          </label>
          <input
            id="search"
            type="search"
            value={term}
            onChange={(event) => setTerm(event.target.value)}
            placeholder="Search by name"
            className="min-w-0 flex-1 rounded border px-3 py-2"
          />
          <button type="submit" className={secondaryButton}>
            Search
          </button>
        </form>
      )}
      {error ? (
        <ErrorMessage error={error} />
      ) : !list ? (
        <Loading />
      ) : list.length === 0 ? (
        <p className="mt-6">
          {searched === undefined
            ? `No ${title.toLowerCase()} yet.`
            : `No ${title.toLowerCase()} match "${searched}".`}
        </p>
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
