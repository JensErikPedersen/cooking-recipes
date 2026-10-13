import { PageNotFound } from "@/components/page-not-found";

// Any URL no page answers. It renders in the root layout, outside the signed-in frame, since it
// cannot know whether the visitor is signed in.
export default function NotFound() {
  return (
    <main className="mx-auto mt-24 w-full max-w-xl px-8">
      <PageNotFound />
    </main>
  );
}
