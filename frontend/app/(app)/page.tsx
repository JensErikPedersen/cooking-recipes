import Link from "next/link";

const SECTIONS = [
  { href: "/recipes", label: "Recipes", text: "Each with its category, tags, ingredients and instructions." },
  { href: "/categories", label: "Categories", text: "What kind of dish a recipe is, such as bread or dessert." },
  { href: "/ingredients", label: "Ingredients", text: "What recipes are made of." },
  { href: "/units", label: "Units", text: "What ingredients are measured in, such as grams or decilitres." },
  { href: "/tags", label: "Tags", text: "Labels to find recipes by, such as spicy or child-friendly." },
];

// The Welcome page: what the application is, and the way into each part of it.
export default function WelcomePage() {
  return (
    <>
      <h1 className="text-3xl font-bold">Cooking Recipes</h1>
      <p className="mt-4 max-w-2xl">
        Keep your recipes in one place. A recipe has a category, any number of tags, and a list of
        ingredients, each with an amount and a unit. Start with a recipe, or set up the categories,
        ingredients, units and tags they draw on.
      </p>
      <ul className="mt-8 flex flex-col gap-4">
        {SECTIONS.map(({ href, label, text }) => (
          <li key={href}>
            <Link href={href} className="text-lg font-semibold underline hover:no-underline">
              {label}
            </Link>
            <p className="text-gray-700">{text}</p>
          </li>
        ))}
      </ul>
    </>
  );
}
