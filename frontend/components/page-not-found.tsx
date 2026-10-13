import Link from "next/link";

// What an address with no page for this visitor shows: app/not-found.tsx for any unknown URL, and a
// signed-in page the visitor may not see, such as the users page for anyone but an admin.
export function PageNotFound() {
  return (
    <>
      <h1 className="text-2xl font-bold">Page not found</h1>
      <p className="mt-4">
        There is no page at this address.{" "}
        <Link href="/" className="underline hover:no-underline">
          Go to the start page
        </Link>
      </p>
    </>
  );
}
