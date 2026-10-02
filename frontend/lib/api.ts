// The one place the frontend calls the backend. Every path is relative (/api/...), so the browser
// stays on this origin and next.config.ts forwards the call - the session and CSRF cookies are
// first-party and there is no CORS.

/** The backend's error envelope, as every non-2xx response carries it. */
export interface ErrorEnvelope {
  errorCode: number;
  message: string;
  description?: string;
  validationExceptions?: { objectName: string; message: string }[];
}

/** Thrown for any non-2xx response. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly envelope: ErrorEnvelope,
  ) {
    super(envelope.message);
  }
}

export interface AuthenticatedUser {
  username: string;
  roles: string[];
}

function readCookie(name: string): string | undefined {
  return document.cookie
    .split("; ")
    .find((cookie) => cookie.startsWith(`${name}=`))
    ?.slice(name.length + 1);
}

// Writes need the CSRF token from the XSRF-TOKEN cookie, sent back as a header. The backend issues
// that cookie on every response, but logout clears it - so if it is missing, any GET fetches a new
// one, even a 401.
async function csrfToken(): Promise<string> {
  if (!readCookie("XSRF-TOKEN")) {
    await fetch("/api/v1/auth/me");
  }
  return decodeURIComponent(readCookie("XSRF-TOKEN") ?? "");
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {};
  if (method !== "GET") {
    headers["X-XSRF-TOKEN"] = await csrfToken();
  }
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }
  const response = await fetch(`/api/v1${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await response.text();
  // A session that ended while a page was open - the backend restarted, or it timed out - answers
  // 401 to whatever the page asks next. Go to sign in rather than show the error. Not for the login
  // call itself, whose 401 is a wrong password for the login page to show.
  if (response.status === 401 && path !== "/auth/login") {
    window.location.replace("/login");
  }
  if (!response.ok) {
    throw new ApiError(response.status, JSON.parse(text));
  }
  return (text ? JSON.parse(text) : undefined) as T;
}

/** The field errors of a failed call by field name: a validation 400 and a 409 on a field alike. */
export function fieldErrors(error: unknown): Record<string, string> {
  if (!(error instanceof ApiError)) {
    return {};
  }
  return Object.fromEntries((error.envelope.validationExceptions ?? []).map((v) => [v.objectName, v.message]));
}

/**
 * The audit fields every entity carries. The backend writes dates as "yyyy-MM-dd HH:mm" in its
 * JVM's zone, which the stack pins to UTC, with no offset - and cannot read them back. So they are
 * display-only and never sent.
 */
export interface Audited {
  id: string;
  created: string;
  createdBy: string;
  updated?: string;
  updatedBy?: string;
}

export interface Category extends Audited {
  name: string;
  description?: string;
}

/** What a form sends: the editable fields only. */
export type CategoryInput = Pick<Category, "name" | "description">;

export const categories = {
  list: () => request<Category[]>("GET", "/categories"),
  get: (id: string) => request<Category>("GET", `/categories/${encodeURIComponent(id)}`),
  create: (input: CategoryInput) => request<Category>("POST", "/categories", input),
  update: (id: string, input: CategoryInput) =>
    request<Category>("PUT", `/categories/${encodeURIComponent(id)}`, input),
  remove: (id: string) => request<void>("DELETE", `/categories/${encodeURIComponent(id)}`),
};

/** One ingredient line of a recipe, with the names the read view shows already resolved. */
export interface RecipeIngredientLine {
  ingredientId: string;
  ingredientName: string;
  amount: number;
  unitId: string;
  unitLabel: string;
}

export interface Recipe extends Audited {
  name: string;
  description?: string;
  instructions?: string;
  category: Category;
  tags?: Tag[];
  recipeIngredients?: RecipeIngredientLine[];
}

/**
 * What the recipe form sends: its own fields, the category and tags by id, and every ingredient
 * line - the API replaces the recipe's lines with these. Never recipeRatings: the API rejects them.
 */
export interface RecipeInput {
  name: string;
  description?: string;
  instructions?: string;
  category?: { id: string };
  tags: { id: string }[];
  recipeIngredients: { ingredientId?: string; amount?: number; unitId?: string }[];
}

export const recipes = {
  list: () => request<Recipe[]>("GET", "/recipes"),
  get: (id: string) => request<Recipe>("GET", `/recipes/${encodeURIComponent(id)}`),
  create: (input: RecipeInput) => request<Recipe>("POST", "/recipes", input),
  update: (id: string, input: RecipeInput) => request<Recipe>("PUT", `/recipes/${encodeURIComponent(id)}`, input),
  remove: (id: string) => request<void>("DELETE", `/recipes/${encodeURIComponent(id)}`),
};

export interface Ingredient extends Audited {
  name: string;
  description?: string;
}

export type IngredientInput = Pick<Ingredient, "name" | "description">;

export const ingredients = {
  list: () => request<Ingredient[]>("GET", "/ingredients"),
  get: (id: string) => request<Ingredient>("GET", `/ingredients/${encodeURIComponent(id)}`),
  create: (input: IngredientInput) => request<Ingredient>("POST", "/ingredients", input),
  update: (id: string, input: IngredientInput) =>
    request<Ingredient>("PUT", `/ingredients/${encodeURIComponent(id)}`, input),
  remove: (id: string) => request<void>("DELETE", `/ingredients/${encodeURIComponent(id)}`),
};

export interface Tag extends Audited {
  name: string;
}

export type TagInput = Pick<Tag, "name">;

export const tags = {
  list: () => request<Tag[]>("GET", "/tags"),
  get: (id: string) => request<Tag>("GET", `/tags/${encodeURIComponent(id)}`),
  create: (input: TagInput) => request<Tag>("POST", "/tags", input),
  update: (id: string, input: TagInput) => request<Tag>("PUT", `/tags/${encodeURIComponent(id)}`, input),
  remove: (id: string) => request<void>("DELETE", `/tags/${encodeURIComponent(id)}`),
};

export interface Unit extends Audited {
  name: string;
  label: string;
}

export type UnitInput = Pick<Unit, "name" | "label">;

export const units = {
  list: () => request<Unit[]>("GET", "/units"),
  get: (id: string) => request<Unit>("GET", `/units/${encodeURIComponent(id)}`),
  create: (input: UnitInput) => request<Unit>("POST", "/units", input),
  update: (id: string, input: UnitInput) => request<Unit>("PUT", `/units/${encodeURIComponent(id)}`, input),
  remove: (id: string) => request<void>("DELETE", `/units/${encodeURIComponent(id)}`),
};

export const auth = {
  me: () => request<AuthenticatedUser>("GET", "/auth/me"),
  login: (username: string, password: string) =>
    request<AuthenticatedUser>("POST", "/auth/login", { username, password }),
  logout: () => request<void>("POST", "/auth/logout"),
};
