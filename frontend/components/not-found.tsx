import Link from "next/link";

// What a read view or edit form shows for an id the backend answers 404 to.
export function NotFound({ title, listHref, listLabel }: { title: string; listHref: string; listLabel: string }) {
  return (
    <>
      <h1 className="text-2xl font-bold">{title}</h1>
      <p className="mt-4">
        It may have been deleted.{" "}
        <Link href={listHref} className="underline hover:no-underline">
          {listLabel}
        </Link>
      </p>
    </>
  );
}
