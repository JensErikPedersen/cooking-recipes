# Implementation plan, version 7

Read `CLAUDE.md` first for the requirements, technical decisions and work process. `docs/PLAN.md`
is the MVP, `docs/PLAN_V2.md` to `docs/PLAN_V6.md` the rounds of enhancements before this one, kept
as the record; this file plans the sixth round.

Nothing proceeds from one part to the next without review and acceptance. Check off each box as it
completes, and record what was actually done underneath the part when it differs from the plan.

**Status: in progress.** Drafted and started 2026-11-07; the decisions below were settled that
day. Parts 1 and 2 are done.

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
   | Tan | `#D8C9A8` | the band of a label, rules between rows; at half strength or less, a tint behind text |

   Measured contrast (WCAG): olive/cream 6.41, olive/tan 4.49, cream/ochre 2.76, olive/ochre 2.32,
   ochre/tan 1.93, cream/tan 1.43. So:
   - Text is olive on cream or cream on olive. On tan, only large text (24 px, or 19 px bold),
     since 4.49 is just under the 4.5 body text needs.
   - Ochre never carries text and is never the only edge of a control - controls need 3:1. Input
     borders and the focus ring are olive.
   - A palette colour with transparency is allowed for shadows, the dialog backdrop and tints. Tan
     at 50% over cream takes olive text at 5.40, at 25% at 5.91. Text itself is never lightened
     below 85% olive (4.54): "None" and placeholders are set in italics instead.
   - Errors are olive text on a tan tint with a warning icon, `role="alert"` as now. The Delete
     dialog's confirming button is the primary look; its wording - "This cannot be undone." -
     carries the warning. `dangerButton` goes.

   *Amended in Part 2.* Tan was "surfaces: cards, labels, table heads" and errors had "an ochre
   bar": small text on full tan fails (4.49), and a coloured bar down the side of a box is a stock
   look the frontend-design skill warns against.
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

## Design language

Written in Part 2 with the frontend-design skill. Parts 3 to 6 build to it; a change to it is a
change to this section first.

**Subject.** A household's own recipes, in Danish - Fuldkorns hvedebrød, Stegt flæsk med
persillesovs, measured in gr, dl and tsk. **Audience**: the people of that household, signed in,
as often on a phone or a tablet propped up on the kitchen counter as at a desk. **Primary job**:
find a recipe and read it at arm's length while cooking; now and then add one or fix one.

**Concept: the pantry.** The entity types are jars on a shelf, and anything a user opens wears a
jar's label: a cream field inside a tan band, with an ochre hairline inside the field, as a jam
label is printed. The label is kept for what is opened or read as one thing - a recipe card, a
recipe's ingredients, the sign-in panel. Lists, forms and tables stay plain on the cream page.

**The one bold element: the shelf** on the Welcome page. Five drawn jars stand on an ochre shelf,
one per entity type, each with its name and one sentence, each a link to its list. The jars are
set down on the shelf one after another when the page opens - the one entrance of decision 7.
Everything else is quiet: no other drawing moves, and no other page has a hero.

**Typefaces** (decision 8):
- **Young Serif** for the app's name, page headings and recipe names. One heavy weight with the
  ink of letterpress on a label; it carries the personality, so it is used for names only, never
  for running text.
- **Fira Sans** for everything else. A humanist sans drawn for small screens, open and easy to
  read from across a counter. Its zero is a plain narrow oval, never mistaken for Ø, and its `l`
  has a tail, so "1 dl" and "Il" read apart. Weights 400 and 600, and 400 italic for "None" and
  placeholders.

Fallbacks are Georgia and the system sans. Both faces have Æ, Ø and Å, checked in the preview.
*Changed after the preview's first look:* the body face was Atkinson Hyperlegible Next, chosen
for legibility - but its zero carries a slash, so "10 gr Gær" read "1Ø gr Gær", and no stylistic
set of the font removes it. Of the alternatives rendered side by side, Lexend, Readex Pro and
Figtree draw `I` and `l` alike, and Andika's single-storey `a` sits badly beside Young Serif.

**Type scale**, from the classic scale in *The Elements of Typographic Style*, in px:

| Size | Use |
|---|---|
| 14 | small print: the audit fields, a field's error |
| 16 | body, inputs, buttons, table cells |
| 18 | a recipe's ingredient lines and instructions |
| 21 | recipe names on cards, `h2` |
| 24 | `h1` on a phone |
| 36 | `h1` from `md`; the Welcome page's title on a phone |
| 48 | the Welcome page's title from `md` |

Line height 1.5 for the sans, 1.65 for instructions, 1.15 for headings. Running text at most
65 characters wide. Headings `text-wrap: balance`. Sentence case everywhere; no capitals as labels.

**Spacing and width.** Tailwind's 4 px steps. Side gutter 16 px on a phone, 32 px from `md`.
Content at most 64 rem wide (`max-w-5xl`), wider than today's `max-w-4xl` for three columns of cards.

**Shapes, by hierarchy.**
- The label: the jar-label outline, its four corners cut in by a quarter circle of 12 px. The
  notch is drawn with a CSS mask on a background layer (`::before`), not on the element: a mask
  on the element would cut off its focus ring too.
- Buttons and inputs: 6 px corners.
- The category of a recipe: a pill tinted tan at 50%. Its tags: pills with an olive outline on
  cream. The two differ by fill, so a recipe's category is never read as one of its tags.
- The header band and tables: square.

**Shadows.** None, but under what floats above the page - the open menu panel and the dialog: an
olive shadow at 25%, never grey.

**Colour, by element** (decision 1 holds throughout):
- Header band olive; the app's name and the entries cream; the current entry has an ochre bar
  under it, 3 px; an entry under the pointer is underlined in cream.
- Primary button olive with cream text, an ochre ring inside it under the pointer. Secondary
  button cream with an olive outline, tinted tan at 50% under the pointer. Disabled: at half
  opacity, which WCAG exempts.
- Inputs: a well tinted tan at 25%, an olive outline 1.5 px, 2 px when invalid. Focus ring olive,
  2 px, 2 px away from the control; cream on the olive band.
- Links in running text: olive, underlined in ochre 2 px below the baseline, olive under the pointer.
- Tables: the head olive and bold on cream with an ochre rule beneath it, rows divided by tan
  rules, a row under the pointer tinted tan at 25%. Amounts use tabular figures, right aligned.
- Errors: olive text on tan at 50%, after an outlined warning triangle. A field's error is the
  same at 14 px, under its input.

**Drawings** (decision 4): flat shapes in the four colours, an olive outline 2.5 px. Five jars,
each telling what it holds - Recipes a tall jar with a clamp lid, full of preserves; Categories a
wide jar of three layers; Ingredients a round jar of grains under a cloth tied with twine; Units a
measuring jug with marks; Tags a jar with a tag on a string. A dill sprig and an ear of wheat for
the sign-in pages. The jars stand empty for an empty list and a page not found. They are
decoration, `aria-hidden`: the text beside them says what they show.

**Motion** (decision 7), all of it off with `prefers-reduced-motion`:
1. The menu panel opens down in 160 ms; closing is instant.
2. The Delete dialog rises 8 px into place in 160 ms, its backdrop olive at 40%.
3. A button pressed moves 1 px down.
4. A recipe card under the pointer or in focus: its ochre hairline goes from 1 to 2 px. No lift.
5. The Welcome page's jars set down on the shelf, 70 ms apart, 320 ms each. They start in place
   and visible - the page is complete without the animation.

**Checked against the skill's generic defaults.**
- *Cream with a high-contrast serif and a terracotta accent*: the cream is the brief's own. The
  serif is Young Serif, heavy and low in contrast - lettering, not a fashion display - and there
  is no terracotta. Changed: the first choice was Fraunces, the serif such pages default to.
- *Broadsheet hairlines and zero radius*: the frames are bands; hairlines only inside labels.
- *The SaaS card kit*: labels only where a thing is opened, a silhouette rather than `rounded-lg`,
  no shadows under them, no gradients. Changed: the error box first had an ochre bar down its side.
- *Template chrome*: no capitals as eyebrows, no middle dots, no arrows after links, no monospace.
  Changed: a card first had its category as a small-capitals line above the name - now a pill
  under it, and category and tags no longer read "Brød · Godt til kaffen".
- Changed for contrast: table heads were to be tan, and the ingredients panel a full-tan surface;
  small text fails there, so both are cream, the panel inside a tan band.

### Wireframes

**Header, from `md`.** One row; between `md` and `lg` the entries may wrap to a second.

```
+------------------------------------------------------------------------------------+
| Cooking Recipes  Recipes  Categories  Ingredients  Units  Tags  Users  jens [Log out] |
|                  =======                                                           |
+------------------------------------------------------------------------------------+
                   ochre bar under the current entry
```

**Header on a phone**, closed and open. The button's name stays "Menu"; its icon turns from three
bars into a cross. The panel pushes the page down rather than covering it.

```
+-----------------------------------+    +-----------------------------------+
| Cooking Recipes         [≡ Menu]  |    | Cooking Recipes         [x Menu]  |
+-----------------------------------+    +-----------------------------------+
  Recipes                                 | Recipes                   48 px   |
  ...                                     | Categories                        |
                                          | Ingredients                       |
                                          | Units                             |
                                          | Tags                              |
                                          | Users            (admins only)    |
                                          | --------------- ochre rule ------ |
                                          | jens                   [Log out]  |
                                          +-----------------------------------+
```

**Recipe cards.** One column on a phone, two from `md`, three from `lg`. The whole card is the
link; the name is its text.

```
 .--------------------------.  .--------------------------.  .--------------------------.
(  Fuldkorns hvedebrød       )(  Hvedebrød med Rugmel      )(  Kylling med bacon og     )
(                            )(                            )(  lækker kålsalat           )
(  (Brød)                    )(  (Brød)                    )(                            )
(  [Godt til kaffen]         )(  [Godt til kaffen]         )(  (Hovedret)                )
 '--------------------------'  '--------------------------'  '--------------------------'
   (..) category, tan pill       [..] tag, outlined pill
```

**A recipe on a phone.** Ingredients before the instructions.

```
Fuldkorns hvedebrød              h1, 24 px
[Edit] [Delete]
Category     (Brød)
Tags         [Godt til kaffen]
Description  Et dejligt Fuldkorns brød som ...
 .-----------------------------.
(  Ingredients                  )
(    170 gr  Fuldkorns hvedemel )   18 px, amounts right aligned
(     10 gr  Gær                )
(    550 gr  Hvedemel           )
 '-----------------------------'
Instructions                     h2
1. Hæld vand og gær i en ...     18 px, line height 1.65
```

**A recipe from `md`.** The ingredients beside the instructions, and sticky, so the amounts stay
in view while the steps are read.

```
Fuldkorns hvedebrød                                       [Edit] [Delete]
Category (Brød)    Tags [Godt til kaffen]
Description  Et dejligt Fuldkorns brød som smager godt og mætter dejligt
 .------------------------.   Instructions
(  Ingredients             )  1. Hæld vand og gær i en røremaskine ...
(   170 gr Fuldkorns ...   )  2. ...
(    10 gr Gær             )
(   550 gr Hvedemel        )     at most 65 characters wide
 '------------------------'
   sticky while scrolling
```

**The Welcome page.** From `md` one shelf of five; on a phone each jar has a shelf of its own, with
its name and sentence beside it.

```
Cooking Recipes                                   48 px
Keep your recipes in one place. ...               at most 65 characters

   _____      _______      .---.        __         _____
  |     |    |=======|    (     )     _/  |       |     |_
  |#####|    |-------|    |:::::|    |-- -|       |#####| \ tag
  |#####|    |#######|    |:::::|    |-- -|       |#####|
==================================================================  ochre shelf
  Recipes    Categories   Ingredients  Units       Tags
  Each with  What kind    What         What        Labels to
  its ...    of dish ...  recipes ...  ...         find ...
```

**Sign in** (and register, forgot and reset password): one label, centred on the cream page, the
dill sprig above it.

```
              (dill sprig)
     .-------------------------------.
    (  Cooking Recipes                )   Young Serif, 24 px
    (  Sign in                        )
    (  Username                       )
    (  [___________________________]  )
    (  Password                       )
    (  [___________________________]  )
    (  [          Sign in          ]  )
    (  Forgot your password?          )
    (  No account yet?                )
    (  Create an account              )
     '-------------------------------'
```

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

- [x] A "Design language" section in this file: subject, audience and primary job (a home cook's
      own Danish recipes, read on a phone or tablet propped up while cooking); the colours' roles
      from decision 1; the two typefaces and a type scale; spacing, corner radii by hierarchy,
      shadows; the drawings to make; the motion list from decision 7
- [x] ASCII wireframes: the header at desktop and on a phone, closed and open; the recipe cards;
      a recipe's read view on a phone and a tablet; the Welcome page; the sign-in page
- [x] The one bold element named, and everything else quiet
- [x] The design checked against the skill's list of generic defaults, with what was changed
- [x] A one-page preview of the palette, type scale and a recipe card with real names
      ("Fuldkorns hvedebrød", "Ølandshvedebrød"), published as a private Artifact for review and
      not added to the repository

**Verify it yourself.** Read the section; open the preview on a phone and a desktop.

**Done 2026-11-07.** The section is "Design language", above the parts. The preview,
"Pantry Labels Preview", is https://claude.ai/artifact/VpqQ45zKky75C7YR2rX7y8 - private to its
owner. It goes further than asked: the header and its phone menu work, as do the Delete dialog
and the jars' entrance, so decisions 5 and 7 can be tried before Part 3. Its names, amounts and
messages come from the seed data and the backend; steps 2 to 4 of the instructions are example
text. Two things changed on the way, both recorded where they belong:
- Decision 1 was amended: small text on full tan fails, so tan became a band and a tint, and the
  error box lost the ochre bar the skill lists as a stock look.
- The body face became Fira Sans: Atkinson Hyperlegible Next, the first choice, has a slashed
  zero, which in Danish reads as Ø.

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
