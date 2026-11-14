"use client";

import { useEffect, useState, type FormEvent, type ReactNode } from "react";
import Link from "next/link";
import { EmptyJar } from "@/components/drawings";
import { ErrorMessage } from "@/components/error-message";
import { Loading } from "@/components/loading";
import { PageHeader } from "@/components/page-header";
import { ButtonLabel } from "@/components/icons";
import { Label } from "@/components/label";
import { iconButton, input, primaryButton, secondaryButton, textLink } from "@/components/styles";
import { danish } from "@/lib/sort";

// The list page of an entity type: a New button, then a table that starts with the name, linked to
// the read view and sorted by it, the Danish way. The API returns rows in storage order. Given a
// card, the list is cards instead - Recipes (docs/PLAN_V7.md, decision 3) - each a label with the
// name as its heading, whose link covers the whole card, and what the card adds under it. Given a
// tile, it is a grid of square tiles - Pictures - each the tile in a tan band, the name under it.
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
  card,
  cardPicture,
  tile,
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
  /** What a card shows under the name; given, the list is cards rather than a table. */
  card?: (entity: T) => ReactNode;
  /** What a card shows above the name - a recipe's picture - if anything. */
  cardPicture?: (entity: T) => ReactNode;
  /** What fills a tile's square band; given, the list is a grid of tiles. */
  tile?: (entity: T) => ReactNode;
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
        <Link href={`${path}/new`} className={`${primaryButton} ${iconButton}`}>
          <ButtonLabel icon="new">{newLabel}</ButtonLabel>
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
            className={`${input} min-w-0 flex-1`}
          />
          <button type="submit" className={`${secondaryButton} ${iconButton} flex-none`}>
            <ButtonLabel icon="search">Search</ButtonLabel>
          </button>
        </form>
      )}
      {error ? (
        <ErrorMessage error={error} />
      ) : !list ? (
        <Loading />
      ) : list.length === 0 ? (
        // An empty jar beside the words, as on a page not found.
        <div className="mt-6 flex items-end gap-4">
          <EmptyJar className="h-auto w-12 flex-none" />
          <p>
            {searched === undefined
              ? `No ${title.toLowerCase()} yet.`
              : `No ${title.toLowerCase()} match "${searched}".`}
          </p>
        </div>
      ) : tile ? (
        <ul className="mt-6 grid grid-cols-2 gap-3 md:grid-cols-3 md:gap-5 lg:grid-cols-4">
          {list
            .toSorted((a, b) => danish(a.name, b.name))
            .map((entity) => (
              // As on a card, the name's link covers the tile. Where there is hover (Tailwind's hover
              // variant asks for it) and motion is allowed, the band grows and floats above its
              // neighbours (docs/PLAN_V8.md, decision 9); in focus it gets the ring and stays put.
              <li key={entity.id} className="group relative hover:z-10">
                <div
                  className={
                    "bg-tan p-1.5 transition duration-160 ease-out motion-reduce:transition-none " +
                    "motion-safe:group-hover:scale-112 motion-safe:group-hover:shadow-[0_10px_30px] motion-safe:group-hover:shadow-olive/25 " +
                    "group-has-[a:focus-visible]:outline-2 group-has-[a:focus-visible]:outline-offset-2 group-has-[a:focus-visible]:outline-olive"
                  }
                >
                  {tile(entity)}
                </div>
                <h2 className="mt-2 font-sans text-base font-semibold">
                  <Link href={`${path}/${entity.id}`} className="after:absolute after:inset-0 focus-visible:outline-none">
                    {entity.name}
                  </Link>
                </h2>
              </li>
            ))}
        </ul>
      ) : card ? (
        <ul className="mt-6 grid gap-5 md:grid-cols-2 lg:grid-cols-3">
          {list
            .toSorted((a, b) => danish(a.name, b.name))
            .map((entity) => (
              // The link's ::after spans the card, so the card is the target; its focus ring is the
              // card's, and under the pointer or in focus the hairline thickens (motion 4).
              <Label
                key={entity.id}
                as="li"
                className="group has-[a:focus-visible]:outline-2 has-[a:focus-visible]:outline-offset-3 has-[a:focus-visible]:outline-olive"
                fieldClassName="flex flex-col gap-3.5 group-hover:outline-2 group-has-[a:focus-visible]:outline-2"
              >
                {cardPicture?.(entity)}
                <h2 className="text-xl">
                  <Link href={`${path}/${entity.id}`} className="after:absolute after:inset-0 focus-visible:outline-none">
                    {entity.name}
                  </Link>
                </h2>
                {card(entity)}
              </Label>
            ))}
        </ul>
      ) : (
        <table className="mt-6 w-full text-left">
          <thead className="border-b-2 border-ochre">
            <tr>
              <th className="py-2 pr-8 font-semibold">Name</th>
              {columns.map(([header]) => (
                <th key={header} className="py-2 pr-8 font-semibold">
                  {header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {list
              .toSorted((a, b) => danish(a.name, b.name))
              .map((entity) => (
                <tr key={entity.id} className="border-b border-tan hover:bg-tan/25">
                  <td className="py-2.5 pr-8">
                    <Link href={`${path}/${entity.id}`} className={textLink}>
                      {entity.name}
                    </Link>
                  </td>
                  {columns.map(([header, value]) => (
                    <td key={header} className="py-2.5 pr-8">
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
