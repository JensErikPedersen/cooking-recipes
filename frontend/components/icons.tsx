import type { ReactNode } from "react";

// Line icons for buttons: 2 px strokes in the button's text colour. Controls, not decoration, so
// unlike the drawings they have no fill.
const ICONS = {
  edit: "M4 20 L5 15 L15.5 4.5 a2.1 2.1 0 0 1 3 0 l1 1 a2.1 2.1 0 0 1 0 3 L9 19 Z M13.5 6.5 l4 4",
  delete: "M4 7h16 M9.5 7V4.5h5V7 M6.5 7l1 13h9l1-13 M10.5 11v5.5 M13.5 11v5.5",
  save: "M5 12.5l4.5 4.5L19 7.5",
  cancel: "M6.5 6.5l11 11 M17.5 6.5l-11 11",
  search: "M16.5 10.5a6 6 0 1 1-12 0a6 6 0 0 1 12 0Z M15 15l5 5",
  new: "M12 5v14 M5 12h14",
  menu: "M4 6.5h16 M4 12h16 M4 17.5h16",
};

export type IconName = Exclude<keyof typeof ICONS, "menu">;

function Icon({ path, className }: { path: string; className: string }) {
  return (
    <svg
      viewBox="0 0 24 24"
      aria-hidden="true"
      className={className}
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d={path} />
    </svg>
  );
}

// A button's content: narrower than md its icon, from md its word. The word stays in the button,
// visually hidden, as its name for a screen reader and for the tests. The button itself takes
// iconButton from styles.ts, which makes it a square.
export function ButtonLabel({ icon, children }: { icon: IconName; children: ReactNode }) {
  return (
    <>
      <Icon path={ICONS[icon]} className="size-[22px] md:hidden" />
      <span className="max-md:sr-only">{children}</span>
    </>
  );
}

// The picker's mark on the picture already chosen: the tick of Save.
export function TickIcon({ className }: { className: string }) {
  return <Icon path={ICONS.save} className={className} />;
}

// The Menu button's: three bars, a cross while the menu is open.
export function MenuIcon({ open }: { open: boolean }) {
  return <Icon path={open ? ICONS.cancel : ICONS.menu} className="size-5" />;
}

// Before an error: an ochre triangle outlined in olive, so the warning does not rest on colour.
export function WarningIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true" className="mt-0.5 size-[18px] flex-none">
      <path d="M10 2.5 L18.5 17 H1.5 Z" fill="#b8892d" stroke="#4f5b2a" strokeWidth={1.6} strokeLinejoin="round" />
      <path d="M10 8v4.2" stroke="#4f5b2a" strokeWidth={1.8} strokeLinecap="round" />
      <circle cx="10" cy="14.6" r="1.1" fill="#4f5b2a" />
    </svg>
  );
}
