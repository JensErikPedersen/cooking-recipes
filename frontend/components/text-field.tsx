// A labelled input with the server's error for it underneath, tied to it by aria-describedby so the
// error belongs to the field for assistive technology and for Playwright alike.
export function TextField({
  label,
  name,
  value,
  onChange,
  error,
  multiline = false,
  type = "text",
  autoComplete,
}: {
  label: string;
  name: string;
  value: string;
  onChange: (value: string) => void;
  error?: string;
  multiline?: boolean;
  type?: "text" | "password" | "email";
  autoComplete?: string;
}) {
  const errorId = `${name}-error`;
  const props = {
    id: name,
    name,
    value,
    "aria-invalid": error ? true : undefined,
    "aria-describedby": error ? errorId : undefined,
    className: "rounded border px-3 py-2",
  };

  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={name} className="font-semibold">
        {label}
      </label>
      {multiline ? (
        <textarea rows={3} {...props} onChange={(event) => onChange(event.target.value)} />
      ) : (
        <input {...props} type={type} autoComplete={autoComplete} onChange={(event) => onChange(event.target.value)} />
      )}
      {error && (
        <p id={errorId} className="text-sm text-red-700">
          {error}
        </p>
      )}
    </div>
  );
}
