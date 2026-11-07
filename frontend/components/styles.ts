// Looks shared by several components, in the palette of docs/PLAN_V7.md.

// Buttons, for links and buttons alike: one primary action per screen, the rest secondary. At
// least 44 px tall below lg, for a finger; a press moves it down 1 px.
const button =
  "inline-flex min-h-11 items-center justify-center gap-2 rounded-md px-4 font-semibold transition duration-100 ease-out " +
  "active:not-disabled:translate-y-px disabled:cursor-not-allowed disabled:opacity-50 " +
  "motion-reduce:transition-none motion-reduce:active:translate-y-0 lg:min-h-10";
export const primaryButton = `${button} bg-olive text-cream hover:not-disabled:inset-ring-2 hover:not-disabled:inset-ring-ochre`;
export const secondaryButton = `${button} border-[1.5px] border-olive bg-cream text-olive hover:not-disabled:bg-tan/50`;

// Added to a button that shows an icon instead of its word narrower than md (ButtonLabel): a square.
export const iconButton = "max-md:w-11 max-md:px-0";

// Every input, select and textarea: 16 px text, or iOS Safari zooms in on focus.
export const input =
  "min-h-11 w-full rounded-md border-[1.5px] border-olive bg-tan/25 px-3 py-2 text-base font-normal text-olive " +
  "placeholder:text-olive placeholder:italic aria-invalid:border-2 lg:min-h-10";

// A link in running text or a table: underlined in ochre, olive under the pointer.
export const textLink = "underline decoration-ochre decoration-2 underline-offset-4 hover:decoration-olive";
