import Link from "next/link";
import { PageHeader } from "@/components/page-header";
import { textLink } from "@/components/styles";

// What an address with no page for this visitor shows: app/not-found.tsx for any unknown URL, and a
// signed-in page the visitor may not see, such as the users page for anyone but an admin.
export function PageNotFound() {
  return (
    <>
      <PageHeader title="Page not found" />
      <p className="mt-4">
        There is no page at this address.{" "}
        <Link href="/" className={textLink}>
          Go to the start page
        </Link>
      </p>
    </>
  );
}
