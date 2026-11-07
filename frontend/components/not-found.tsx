import Link from "next/link";
import { PageHeader } from "@/components/page-header";
import { textLink } from "@/components/styles";

// What a read view or edit form shows for an id the backend answers 404 to.
export function NotFound({ title, listHref, listLabel }: { title: string; listHref: string; listLabel: string }) {
  return (
    <>
      <PageHeader title={title} />
      <p className="mt-4">
        It may have been deleted.{" "}
        <Link href={listHref} className={textLink}>
          {listLabel}
        </Link>
      </p>
    </>
  );
}
