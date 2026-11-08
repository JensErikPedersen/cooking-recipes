import type { ReactNode } from "react";
import { ApiError } from "@/lib/api";
import { WarningIcon } from "@/components/icons";

// A failed load or write. The backend's message is written for the user; anything else - the
// network, a proxy error page - gets a generic one.
export function ErrorMessage({ error }: { error: unknown }) {
  return (
    <Alert className="mt-6">
      {error instanceof ApiError ? error.message : "Something went wrong. Please try again."}
    </Alert>
  );
}

// An error box: olive text on a tan tint after a warning sign - no fifth colour for errors.
export function Alert({ className = "", children }: { className?: string; children: ReactNode }) {
  return (
    <div role="alert" className={`flex items-start gap-2.5 bg-tan/50 px-3.5 py-3 ${className}`}>
      <WarningIcon />
      <p>{children}</p>
    </div>
  );
}

// The server's error for one field, under its input; the input names it with aria-describedby.
export function FieldError({ id, children }: { id: string; children: ReactNode }) {
  return (
    <p id={id} className="flex items-start gap-1.5 text-sm">
      <WarningIcon />
      {children}
    </p>
  );
}
