import { ApiError } from "@/lib/api";

// A failed load or write. The backend's message is written for the user; anything else - the
// network, a proxy error page - gets a generic one.
export function ErrorMessage({ error }: { error: unknown }) {
  return (
    <p role="alert" className="mt-6 text-red-700">
      {error instanceof ApiError ? error.message : "Something went wrong. Please try again."}
    </p>
  );
}
