# Implementation plan, version 7

Read `CLAUDE.md` first for the requirements, technical decisions and work process. `docs/PLAN.md`
is the MVP, `docs/PLAN_V2.md` to `docs/PLAN_V6.md` the rounds of enhancements before this one, kept
as the record; this file plans the sixth round.

Nothing proceeds from one part to the next without review and acceptance. Check off each box as it
completes, and record what was actually done underneath the part when it differs from the plan.

**Status: in progress.** Drafted and started 2026-11-07; the decisions below were settled that
day. Part 1 is done.

## Shape of the work

The frontend only: the same pages, the same behaviour, a design of their own in the palette below,
and fit for phones and tablets. No change to the API, the schema or the Docker setup.

| Part | Delivers | Layers |
|---|---|---|
| 1 | Baseline and scope | docs |
| 2 | Design language | docs |
| 3 | Foundation: colours, type, controls | frontend, e2e |
| 4 | Header and burger menu | frontend, e2e |
| 5 | Recipes: cards, read view, form | frontend, e2e |
| 6 | Welcome and sign-in pages | frontend, e2e, docs |

**Order.** The design is written down and agreed before any code (Part 2), so the code parts carry
out a decision instead of making one. The foundation next: it replaces every grey and red at once,
so no page is left half old. Then the header, the frame of every page and the phone menu asked
for. Then Recipes, the most used pages. The Welcome and sign-in pages last: they hold the one bold
moment, designed once the language has been proven on working pages.

**Out of scope.** Ratings, as in every version so far. Recipe photos (decision 4), a dark scheme
(decision 10), new features or data. No component, icon or animation library: Tailwind and inline
SVG, as the technical decisions say.

**Numbering.** Parts restart at 1. Commits say `V7 Part 2: ...`.

**Branch.** `spice_up_frontend`, already branched from `master` at the merge of version 6
(`b54de6e`).

## Decisions

Settled 2026-11-07. The parts refer to them by number.

1. **Four colours, and no others.** No white, black, grey or red anywhere.

   | Colour | Hex | Role |
   |---|---|---|
   | Cream | `#F5EFE3` | page background; text on olive |
   | Olive | `#4F5B2A` | text, primary buttons, the header band, input borders, focus ring |
   | Ochre | `#B8892D` | accents only: frames, rules, drawings, the error bar, the current menu entry's mark |
   | Tan | `#D8C9A8` | surfaces: cards, labels, table heads, dividers |

   Measured contrast (WCAG): olive/cream 6.41, olive/tan 4.49, cream/ochre 2.76, olive/ochre 2.32,
   ochre/tan 1.93, cream/tan 1.43. So:
   - Text is olive on cream or cream on olive. On tan, only large text (24 px, or 19 px bold),
     since 4.49 is just under the 4.5 body text needs.
   - Ochre never carries text and is never the only edge of a control - controls need 3:1. Input
     borders and the focus ring are olive.
   - A palette colour with transparency is allowed for shadows, the dialog backdrop and hover
     tints, never to lighten text.
   - Errors are olive text with an ochre bar and a warning icon, `role="alert"` as now. The Delete
     dialog's confirming button is the primary look; its wording - "This cannot be undone." -
     carries the warning. `dangerButton` goes.
2. **Direction: pantry labels** - the labels on preserving jars: an olive header band, tan labels
   in thin ochre frames, an expressive serif for headings. Part 2 turns it into tokens and
   wireframes.
3. **Recipes as cards**: one column on a phone, two on a tablet, three on a desktop. Each card has
   the name as its link, the category and the tags as labels. Still sorted the Danish way, and
   search works as before. Categories, Ingredients, Units, Tags and Users stay tables, restyled:
   short reference lists read best as rows.
4. **No photos.** A small set of inline SVG drawings - herbs, wheat, jars - as React components, so
   there is still no `public/` directory and the `Dockerfile` is unchanged. Photo upload, which
   would need a column, storage and an upload endpoint, goes to `future_enhancements.md`.
5. **The burger menu below `md` (768 px)**: phones get it, tablets keep the menu in the header,
   which wraps to a second row between `md` and `lg` as now. One `<nav aria-label="Main">`, hidden
   below `md` until a "Menu" button opens it - the disclosure pattern, `aria-expanded` and
   `aria-controls`, not a `<dialog>`. The panel holds the menu, the username and Log out. It
   closes when an entry is chosen, on Escape (focus back on the button) and on the button again.
6. **Sizes**: phone 375 px, tablet portrait 768, tablet landscape 1024, desktop 1280. Below `lg`,
   every control is at least 44 px tall; inputs use 16 px text, or iOS Safari zooms in on focus.
7. **Motion answers what the user does**: the menu opening, the dialog, a button pressed, a card
   in focus. One entrance on the Welcome page, nothing else moves by itself. CSS transitions only,
   and none with `prefers-reduced-motion`.
8. **At most two typefaces**, from `next/font/google`, chosen in Part 2 with Danish letters
   rendered. They replace Geist, and `next/font` serves them from this origin, as it does Geist.
9. **e2e asserts behaviour, not looks.** No screenshot comparison: fonts render differently from
   machine to machine and such tests break on every change of design. The structure the specs
   read stays - `dl`/`dt`/`dd` in read views, table rows in the five reference lists, roles and
   names. Looks are reviewed by eye: every code part ends with screenshots of its pages at the
   four sizes, taken with Playwright into a scratch folder that is not committed.
10. **Light only, still**: the palette is a light one.

---

## Part 1: Baseline and scope

Docs.

- [x] Work on `spice_up_frontend`, branched from `master` at version 6's merge
- [x] Root `CLAUDE.md` and `README.md` point at `docs/PLAN_V7.md` as the current plan, with
      `PLAN_V6.md` joining the record; ratings out of scope "in versions 2 to 7"
- [x] Decisions recorded as settled
- [x] `backend/docs/future_enhancements.md`: recipe photos, and what they would take
- [x] Baseline green: `./mvnw clean verify` and the whole Playwright suite at version 6's final
      numbers: 275 unit, 211 integration, 152 Playwright

**Verify it yourself.** Read the diff, then:

```powershell
scripts\start.ps1
cd backend; .\mvnw clean verify
cd ..\e2e; npx playwright test
```

**Done 2026-11-07.** On `spice_up_frontend`, at `master`'s merge of version 6 (`b54de6e`), plus
the plan's own commit. Baseline on a freshly built stack: 275 unit, 211 integration, 152
Playwright tests, all green - version 6's final numbers. The references to `PLAN_V6.md` left in
the docs and the code cite its decisions, and stay.

---

## Part 2: Design language

Docs only, with the frontend-design skill.

- [ ] A "Design language" section in this file: subject, audience and primary job (a home cook's
      own Danish recipes, read on a phone or tablet propped up while cooking); the colours' roles
      from decision 1; the two typefaces and a type scale; spacing, corner radii by hierarchy,
      shadows; the drawings to make; the motion list from decision 7
- [ ] ASCII wireframes: the header at desktop and on a phone, closed and open; the recipe cards;
      a recipe's read view on a phone and a tablet; the Welcome page; the sign-in page
- [ ] The one bold element named, and everything else quiet
- [ ] The design checked against the skill's list of generic defaults, with what was changed
- [ ] A one-page preview of the palette, type scale and a recipe card with real names
      ("Fuldkorns hvedebrød", "Ølandshvedebrød"), published as a private Artifact for review and
      not added to the repository

**Verify it yourself.** Read the section; open the preview on a phone and a desktop.

---

## Part 3: Foundation - colours, type, controls

Frontend, e2e. The header keeps today's layout, recoloured; Part 4 rebuilds it.

- [ ] `app/globals.css`: the four colours as Tailwind theme colours (`bg-cream`, `text-olive`, ...)
      and the fonts; body cream with olive text
- [ ] `app/layout.tsx`: the Part 2 fonts instead of Geist; a `viewport` export with `themeColor`
      olive, which colours a phone browser's bar
- [ ] `components/styles.ts`: primary and secondary looks in the palette, `dangerButton` gone,
      and one input look - olive border, 16 px, 44 px tall below `lg` - used by `TextField`,
      `SelectField`, `IngredientLines`, the search field and the four pages outside the frame
- [ ] Restyled: `ErrorMessage` and field errors, `Loading`, `NotFound`, `PageNotFound`,
      `DetailList`, `PageHeader`, the `DeleteButton` dialog, the `EntityList` table, the Users table
- [ ] A visible focus ring on every control
- [ ] No grey, red, white or black class is left: a grep for them finds nothing
- [ ] `phone.spec.ts` becomes `responsive.spec.ts`: no sideways scrolling on every page it lists,
      at 375, 768 and 1024 px
- [ ] `frontend/CLAUDE.md`: the palette instead of "fixed Tailwind greys", the fonts, `styles.ts`

**Verify it yourself.** Rebuild with `scripts\start.ps1`, run the whole Playwright suite, and look
through the screenshots; open a few pages at `http://localhost:3000` in the browser's device mode.

---

## Part 4: Header and burger menu

Frontend, e2e.

- [ ] `AppShell`: the olive band, the menu inline from `md`, the Menu button below it
- [ ] The panel as decision 5 says; the current page marked with `aria-current` in both layouts
- [ ] `openFromMenu` in `e2e/support/pages.ts` opens the menu first when the Menu button is shown
- [ ] `menu.spec.ts`: at 375 px the menu is hidden until opened and `aria-expanded` follows; every
      entry, the username and Log out are in the panel; choosing an entry closes it; Escape closes
      it with focus on the button; an admin sees Users; at 768 and 1280 px there is no Menu button
      and the entries are visible
- [ ] `frontend/CLAUDE.md`: the `AppShell` and phone-width lines

**Verify it yourself.** Run `npx playwright test menu.spec.ts responsive.spec.ts`; on a phone,
open the app via the PC's network address, or use the browser's device mode.

---

## Part 5: Recipes - cards, read view, form

Frontend, e2e.

- [ ] The recipe list as cards (decision 3): `EntityList` takes a card for Recipes; the other
      lists keep their columns. The empty and no-match messages are as before
- [ ] The read view: category and tags as labels; ingredients on a tan label beside the
      instructions from `md`, above them on a phone; instructions in a size readable at arm's
      length; `dl`/`dt`/`dd` kept for Category, Tags and Description
- [ ] The recipe form: ingredient lines on a phone (two rows, as now) and a tablet
- [ ] e2e: one helper reads a list's names from cards or rows; the empty-list checks look for no
      cards on Recipes; `recipes.spec.ts` reads a card instead of a row
- [ ] `frontend/CLAUDE.md`: the `EntityList` line

**Verify it yourself.** Run `npx playwright test recipes.spec.ts search.spec.ts states.spec.ts`;
open Recipes and "Fuldkorns hvedebrød" at all four sizes.

---

## Part 6: Welcome and sign-in pages

Frontend, e2e, docs.

- [ ] The Welcome page: the bold element from Part 2 with its drawing, the five sections as
      labels, the one entrance (decision 7)
- [ ] Sign in, register, forgot-password and reset-password in one framed panel; the
      `useHydrated` fieldset is unchanged
- [ ] `app/not-found.tsx` in the same design
- [ ] The record: final numbers, every doc swept (root, backend and frontend `CLAUDE.md`,
      `README.md`, `backend/README.md`, `future_enhancements.md`), status done

**Verify it yourself.** The whole suite green; sign out and walk through sign-in, registration and
a reset at phone size.
