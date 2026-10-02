// The names are Danish, so lists sort the Danish way - Æ, Ø, Å after Z - whatever language the
// viewer's browser runs in. Without a locale, localeCompare follows the browser, and the same list
// came out in a different order for an English browser than for a Danish one.
export const danish = (a: string, b: string) => a.localeCompare(b, "da");
