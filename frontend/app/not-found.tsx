import Link from "next/link";

// Any URL no page answers. It renders in the root layout, outside the signed-in frame, since it
// cannot know whether the visitor is signed in.
export default function NotFound() {
  return (
    <main className="mx-auto mt-24 w-full max-w-xl px-8">
      <h1 className="text-2xl font-bold">Page not found</h1>
      <p className="mt-4">
        There is no page at this address.{" "}
        <Link href="/" className="underline hover:no-underline">
          Go to the start page
        </Link>
      </p>
    </main>
  );
}
