# Implementation plan, version 8

Read `CLAUDE.md` first for the requirements, technical decisions and work process. `docs/PLAN.md`
is the MVP, `docs/PLAN_V2.md` to `docs/PLAN_V7.md` the rounds of enhancements before this one, kept
as the record; this file plans the seventh round.

Nothing proceeds from one part to the next without review and acceptance. Check off each box as it
completes, and record what was actually done underneath the part when it differs from the plan.

**Status: accepted.** Drafted and accepted 2026-11-08, the decisions below settled that day.
Part 2 done, for review.

## Shape of the work

**The brief, as written.** This increment should be introducing a picture feature. This will give
the website a more appealing look and it should be possible to upload pictures to the homepage,
recipes and ingredients pages. This extension will require an extension to the database schema
with eg. a new table for pictures and maybe a relations table mapping to recipes and ingredients
so a picture can be shown on multiple pages. The backend will also be extended with CRUD
operations and relevant models and DTO's should be adjusted accordingly. Frontend change involve
features to upload pictures, add from saved pictures in database and deleting an image from the
page. A new menu item Images will open a view listing images in the database and actions to add
or delete images. When an image is uploaded to a page, preferably the picture's size should be
adjusted to the grid that the web page. On a desktop the image should have a larger format, on a
tablet another and viewed on a phone a third format or size fitting the canvas size. For the
Images view, thumbnails with mouse over effects of enlarging the image would be cool.

**In short.** A library of pictures, kept on a Docker volume, each stored in three sizes. A recipe,
an ingredient and the Welcome page can each show one picture from the library, and one picture can
be shown on any number of them. A new Pictures menu entry lists the library as thumbnails.

| Part | Delivers | Layers |
|---|---|---|
| 1 | Baseline and scope | docs |
| 2 | Design: pictures in the pantry | docs |
| 3 | Pictures in the API: upload, sizes, serving | backend, docker, e2e |
| 4 | Pictures in the API: edit, delete, and where they are used | backend, e2e |
| 5 | Pictures pages | frontend, e2e |
| 6 | Pictures on recipes and ingredients | frontend, e2e |
| 7 | A picture on the Welcome page, and the record | frontend, e2e, docs |

**Order.** The design first (Part 2), because V7 designed every page without photos and a picture
on a card changes it. Then the API in two parts: Part 3 proves the hard part - a file through the
Next.js proxy into the backend, resized, onto a volume that survives a restart - before anything
links to it. Part 4 adds the links and the rule that refuses deleting a picture in use. Then the
frontend: the Pictures pages first, since the picker on the forms reuses their thumbnails and their
upload; then the forms and read views; the Welcome page last, as in V7.

**Out of scope.** Ratings, as in every version so far. More than one picture per recipe or
ingredient (decision 2). Cropping or rotating in the browser. Pictures in a recipe's ingredient
lines. Seeded pictures (decision 11). Ownership: any account edits and deletes any picture, as with
everything else (`docs/PLAN_V5.md` decision 8). The desktop polish in `future_enhancements.md`
stays there.

**Numbering.** Parts restart at 1. Commits say `V8 Part 3: ...`.

**Branch.** `add_pictures_feature`, branched from `master` at the merge of version 7 (`9aadb7c`).

## Decisions

Settled 2026-11-08. The parts refer to them by number.

1. **Files on a Docker volume, metadata in MySQL.** A `picture` table holds the name, alt text and
   the audit columns; the files go to a named volume `pictures-data`, mounted at `/data/pictures`
   in the backend, one file per size: `{id}-small.jpg`, `{id}-medium.jpg`, `{id}-large.jpg`. Not in
   the database: pictures would bloat every backup and every dump. `docker compose down -v` deletes
   both volumes together, so a reseed never leaves orphaned files or rows. The directory is the
   property `app.pictures.dir`; the backend's `Dockerfile` creates `/data/pictures` owned by its
   `recipes` user, because a named volume takes the ownership of the image's directory and would
   otherwise be root's and unwritable.
2. **One picture each, by column.** Nullable `recipe.picture_id` and `ingredient.picture_id`, each
   a foreign key to `picture`. Many recipes and ingredients may point at the same picture, which is
   what "shown on multiple pages" needs; a relations table would pay only with several pictures
   per recipe. The DTOs gain a nested `picture` reference, as they hold `category`. The Welcome page
   is not an entity, so its picture is a one-row table `welcome_page` with a nullable `picture_id`,
   created with its row by the changelog. *A flag column on `picture` was the alternative; it needs
   a rule that at most one row has it set.*
3. **A picture's file never changes.** Editing a picture changes its name and alt text only; for
   another photo, upload a new one. So each size's address serves the same bytes forever, and the
   backend sends it with `Cache-Control: private, max-age=31536000, immutable`: the browser fetches
   each size once.
4. **Three sizes, made on upload**: 400, 800 and 1600 px wide, JPEG, the height following the
   original's proportions. A picture is never enlarged: a 600 px upload gives small at 400 and
   medium and large at 600. The original is not kept. Resizing uses **Thumbnailator**, one small
   library, because Java's own ImageIO ignores a phone photo's EXIF orientation and the picture
   would come out sideways. A transparent PNG is flattened onto cream (`#F5EFE3`).
5. **JPEG and PNG, up to 10 MB.** Checked by decoding the file, not by its name or declared type: a
   file that does not decode is refused with 400. ImageIO reads neither WebP nor HEIC; whether an
   iPhone's photo arrives as JPEG through the file input is checked on a real phone in Part 6.
   Spring's multipart limit (1 MB by default) is raised to match, and Part 3 proves the limit holds
   through the Next.js proxy too.
6. **Deleting a picture in use is refused**: 409 `PICTURE_IN_USE`, naming how many recipes and
   ingredients and whether the Welcome page use it, as `CATEGORY_IN_USE` does. Removing a picture
   from a recipe, an ingredient or the Welcome page only unlinks it. Deleting a recipe or an
   ingredient leaves its picture in the library.
7. **A picture is an entity like the others.** The list is a grid of thumbnails; opening one gives the
   read view - the picture large, its name and alt text, where it is used, created and updated by
   and when - with Edit and Delete. New is the upload form: a file, a name and an alt text. Edit is
   the same form without the file. A name and an alt text are both required, at most 255
   characters: the alt text is what a screen reader says instead of the picture.
8. **The browser picks the size.** Pages use a plain `<img>` with `srcset` and `sizes`, so a phone
   downloads the small or medium file and a desktop the large. The grid fixes the box with CSS
   `aspect-ratio` and `object-fit: cover`; Part 2 sets the ratios. Thumbnails and cards load
   lazily. Not `next/image`: the backend already makes the sizes, and its optimizer fetches on the
   server, without the browser's session cookie.
9. **Hover enlarges only where there is hover.** In the Pictures grid a thumbnail grows on hover under
   `@media (hover: hover)`, and only with motion allowed - motion that answers the user, as V7's
   decision 7 says. On a phone or a tablet a tap opens the read view.
10. **One picker for every page.** A dialog shows the library's thumbnails to choose from, and an
    "Upload new" that goes through the same upload as the Pictures form and chooses the result. The
    recipe form, the ingredient form and the Welcome page all use it, with a Remove beside a chosen
    picture.
11. **No seeded pictures.** The stack starts with an empty library. e2e uploads small fixture
    pictures committed under `e2e/fixtures/`, and builds the too-large file in the test rather than
    committing 10 MB.
12. **Behind sign-in, like all of `/api`.** A picture's files are served only to a signed-in
    account; a signed-out request gets 401.
13. **"Pictures", one word everywhere**: the menu entry, the pages and routes (`/pictures`), the
    table (`picture`), the API (`/api/v1/pictures`), the code and the specs. The brief's "Images"
    menu item is named Pictures. "Image" stays only where a name outside this application has it:
    the Docker image, `ImageIO`, `next/image`, `image/jpeg`.

---

## Part 1: Baseline and scope

Docs.

- [x] Work on `add_pictures_feature`, branched from `master` at version 7's merge
- [x] Root `CLAUDE.md` and `README.md` point at `docs/PLAN_V8.md` as the current plan, with
      `PLAN_V7.md` joining the record; ratings out of scope "in versions 2 to 8"
- [x] Decisions recorded as settled
- [x] `backend/docs/future_enhancements.md`: "Recipe photos: none, by decision" replaced by what
      version 8 leaves out - several pictures per recipe, cropping in the browser, WebP and HEIC
- [x] Baseline green: `./mvnw clean verify` and the whole Playwright suite at version 7's final
      numbers: 275 unit, 211 integration, 190 Playwright

**Verify it yourself.** Read the diff, then:

```powershell
scripts\start.ps1
cd backend; .\mvnw clean verify
cd ..\e2e; npx playwright test
```

**Done 2026-11-08.** On `add_pictures_feature`, at `master`'s merge of version 7 (`9aadb7c`), plus
the plan's own commit. Baseline: 275 unit, 211 integration, 190 Playwright tests, all green -
version 7's numbers - but only on a reseeded stack. The first run failed 10 recipe tests: the
seeded "Brunkage" had been renamed by hand that morning, so the test expecting "already exists" for
that name created a real "Brunkage" instead, and left it behind. Reseeded with
`docker compose down -v`, the suite passed. The references to `PLAN_V7.md` left in the docs cite
its design language and its desktop review, and stay. An OpenAPI specification generated at build
time joined `future_enhancements.md` before this part, at the user's request.

---

## Part 2: Design: pictures in the pantry

Docs only, with the frontend-design skill. Amends the "Design language" of `docs/PLAN_V7.md`, which
stays the design's home.

- [x] Where a picture sits in a recipe card, and the card without one unchanged; its aspect ratio
- [x] A recipe's and an ingredient's read view with a picture, on a phone and a desktop
- [x] The Pictures grid: columns per size, the thumbnail's frame, the hover from decision 9
- [x] The picker dialog and the upload form, with the file field's states: none chosen, chosen,
      uploading, refused
- [x] The Welcome page's picture beside the shelf, V7's one bold element: whether it joins the
      shelf, stands above it, or replaces the drawn jar of Recipes - decided here, with wireframes
- [x] The `sizes` value for each place a picture appears, from the grid at the four sizes of V7's
      decision 6
- [x] A preview published as a private Artifact, with stand-in pictures, not added to the
      repository

**Verify it yourself.** Read the amended section; open the preview on a phone and a desktop.

**Done 2026-11-12, for review.** The design is "Pictures (version 8)" in `docs/PLAN_V7.md`, under
its "Design language", with wireframes of its own. The preview, "Pantry Pictures Preview", is
https://claude.ai/artifact/NSbW4GZ6ZsGbEPCsxyFY3j - private to its owner; its photos are painted by
the page, since an Artifact cannot load images from elsewhere. Its Welcome page switches between
the recommended layout, the alternative on the shelf, and none. The frontend-design skill named
above is not available in this environment; the design was checked against V7's list of generic
defaults instead.

In short: 3:2 at the top of a card, inside the label; 4:3 heading the ingredients' column of a
read view from `md`, under the heading on a phone; square tiles in a tan band, 2, 3 and 4 columns;
the Welcome picture beside the introduction, the shelf unchanged. A photo keeps its own colours -
decision 1's palette rules the interface - and never carries text.

Three findings change later parts, if accepted:
- **Part 5, verify.** "The phone width fetches `small` or `medium`, never `large`" holds only at a
  screen density of 1 or 2. A phone at density 3 needs `large` for a read view (343 px x 3), and
  should fetch it. The check belongs at Playwright's density 1.
- **Part 5, menu.** No sixth jar on the Welcome shelf for Pictures: it is a store of photos, not
  something a recipe is made of. The menu entry is in the header and the burger menu only.
- **Part 6, the form.** The form has Choose and Remove; Upload new lives in the picker, as
  decision 10 already has it, rather than as a third button on the form.

---

## Part 3: Pictures in the API: upload, sizes, serving

Backend, docker, e2e. No link to recipes yet.

- [ ] Changelog `1.7`: table `picture` (`id`, `name`, `alt_text`, the audit columns)
- [ ] Thumbnailator in `pom.xml`, at its latest stable version
- [ ] `PictureStorage`: writes the three sizes of decision 4, reads one, deletes all three; unit
      tests on a `@TempDir` with fixture pictures - a landscape and a portrait JPEG, one with an EXIF
      rotation, a transparent PNG, one smaller than 400 px
- [ ] `POST /api/v1/pictures`, multipart: the file, a name and an alt text; 201 with the picture.
      400 for a file that does not decode or a missing name or alt text; 413 above 10 MB
- [ ] `GET /api/v1/pictures` and `GET /api/v1/pictures/{id}`: the metadata, as for other entities
- [ ] `GET /api/v1/pictures/{id}/{size}` for `small`, `medium`, `large`: `image/jpeg`, the cache
      header of decision 3; 404 for an unknown id or size
- [ ] Volume `pictures-data` in `docker-compose.yml`; `/data/pictures` in the backend's
      `Dockerfile`, owned by `recipes`
- [ ] e2e `pictures-api.spec.ts`, through `localhost:3000`: upload, list, fetch each size, a
      file that is not a picture refused, a file just under 10 MB accepted and one over refused - the proof that the
      Next.js proxy passes a large body - and 401 signed out; each test deletes what it uploads
- [ ] `backend/CLAUDE.md` and `backend/README.md`: the endpoints, the storage, the volume

**Verify it yourself.** `./mvnw clean verify` and `npx playwright test pictures-api.spec.ts`. Then,
signed in at `localhost:3000`, upload a phone photo from the browser's console with a few lines of
`fetch` given in the part's record, and open its three sizes in the browser; restart the stack (`scripts\stop.ps1`, `scripts\start.ps1`) and open them
again - they survive. List the files with
`docker compose exec backend ls -l /data/pictures`.

---

## Part 4: Pictures in the API: edit, delete, and where they are used

Backend, e2e.

- [ ] Changelog `1.8`: `recipe.picture_id`, `ingredient.picture_id`, table `welcome_page` with its
      one row; foreign keys to `picture`
- [ ] `RecipeDTO` and `IngredientDTO` carry a `picture` reference, written and read as `category`
      is; an unknown picture id is a 400
- [ ] `GET` and `PUT /api/v1/welcome-page`: the Welcome page's picture, or none
- [ ] `PUT /api/v1/pictures/{id}`: name and alt text only (decision 3)
- [ ] The picture DTO says where it is used: the recipes and ingredients, by id and name, and
      whether the Welcome page uses it
- [ ] `DELETE /api/v1/pictures/{id}`: 409 `PICTURE_IN_USE` while in use (decision 6), else the row
      and the three files
- [ ] Unit and integration tests for each; e2e `pictures-api.spec.ts` extended with linking,
      unlinking and the refused delete
- [ ] `backend/CLAUDE.md`, `backend/README.md`, and the error code in their list

**Verify it yourself.** `./mvnw clean verify` and the e2e spec. Link a picture to a recipe through
the API, try to delete the picture (409 naming the recipe), unlink it and delete it (204); its
files are gone from `/data/pictures`.

---

## Part 5: Pictures pages

Frontend, e2e.

- [ ] Menu entry "Pictures", in the header, the burger menu and on the Welcome page's list of
      sections if Part 2 says so
- [ ] `/pictures`: the thumbnail grid with the hover of decision 9; "No pictures yet." when empty
- [ ] `/pictures/[id]`: the read view of decision 7, with where the picture is used, each a link
- [ ] `/pictures/new`: the upload form; `/pictures/[id]/edit`: name and alt text
- [ ] Delete, with the 409 shown as the error it is
- [ ] A `Picture` component: `<img>` with `srcset` and `sizes` (decision 8)
- [ ] e2e `pictures.spec.ts`: upload, read, edit, delete, the refused delete, a file that is not a
      picture refused in the form, the list at three widths in `responsive.spec.ts`
- [ ] `frontend/CLAUDE.md`: the `Picture` component and the routes

**Verify it yourself.** `npm run lint`, `npm run build`, the whole Playwright suite. At phone and
desktop size: upload a photo, see the thumbnail, hover it on the desktop, open it, edit its alt
text, delete it. In the browser's network tab, the phone width fetches `small` or `medium`, never
`large`.

---

## Part 6: Pictures on recipes and ingredients

Frontend, e2e.

- [ ] The picker of decision 10, using the Pictures upload
- [ ] The recipe form and the ingredient form: a picture field with Choose, Upload new and Remove
- [ ] The recipe card and the recipe's read view with its picture, as Part 2 drew them
- [ ] The ingredient's read view with its picture
- [ ] e2e: choose from the library, upload new from the form, remove, the card shows it, a recipe
      without one unchanged; a picture in use cannot be deleted from the Pictures page
- [ ] An upload from an iPhone, if one is at hand, to settle the HEIC question of decision 5

**Verify it yourself.** The whole suite. At phone size: give a recipe a new photo from the form,
see it on the card and the read view; give an ingredient the same picture from the library; try
to delete it on the Pictures page and read where it is used.

---

## Part 7: A picture on the Welcome page, and the record

Frontend, e2e, docs.

- [ ] The Welcome page's picture as Part 2 placed it, with Choose and Remove through the picker
- [ ] e2e: set, show, remove; the picture in use there cannot be deleted
- [ ] The record: final numbers, every doc swept (root, backend and frontend `CLAUDE.md`,
      `README.md`, `backend/README.md`, `future_enhancements.md`), status done

**Verify it yourself.** The whole suite green; walk the Welcome page at the four sizes with and
without a picture.
