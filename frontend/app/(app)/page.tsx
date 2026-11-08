import Link from "next/link";
import type { ComponentType, CSSProperties } from "react";
import { CategoriesJar, IngredientsJar, RecipesJar, TagsJar, UnitsJug } from "@/components/drawings";

const SECTIONS: { href: string; label: string; text: string; Jar: ComponentType<{ className?: string; style?: CSSProperties }> }[] = [
  { href: "/recipes", label: "Recipes", text: "Each with its category, tags, ingredients and instructions.", Jar: RecipesJar },
  { href: "/categories", label: "Categories", text: "What kind of dish a recipe is, such as bread or dessert.", Jar: CategoriesJar },
  { href: "/ingredients", label: "Ingredients", text: "What recipes are made of.", Jar: IngredientsJar },
  { href: "/units", label: "Units", text: "What ingredients are measured in, such as grams or decilitres.", Jar: UnitsJug },
  { href: "/tags", label: "Tags", text: "Labels to find recipes by, such as spicy or child-friendly.", Jar: TagsJar },
];

// The Welcome page: what the application is, and the way into each part of it - the shelf, the
// design's one bold element (docs/PLAN_V7.md, "Design language"). From md five jars stand on one
// ochre shelf, each over its name and sentence; on a phone each jar has a shelf of its own, the
// words beside it. The jars are set down one after another as the page opens, the one entrance of
// decision 7; they start in place, so the page is complete without it.
export default function WelcomePage() {
  return (
    <>
      <h1 className="text-4xl md:text-5xl">Cooking Recipes</h1>
      <p className="mt-4 max-w-[65ch]">
        Keep your recipes in one place. A recipe has a category, any number of tags, and a list of
        ingredients, each with an amount and a unit. Start with a recipe, or set up the categories,
        ingredients, units and tags they draw on.
      </p>
      <ul className="relative mt-10 grid gap-7 md:grid-cols-5 md:gap-x-4.5 md:before:absolute md:before:-inset-x-2.5 md:before:top-33 md:before:h-2.5 md:before:rounded-xs md:before:bg-ochre md:before:shadow-[0_3px_0] md:before:shadow-olive">
        {SECTIONS.map(({ href, label, text, Jar }, index) => (
          // The name's link covers the whole jar and its words, as a recipe card's does, and the
          // focus ring is drawn around them all.
          <li
            key={href}
            className="group relative grid grid-cols-[72px_minmax(0,1fr)] items-end gap-x-4.5 rounded-md has-[a:focus-visible]:outline-2 has-[a:focus-visible]:outline-offset-4 has-[a:focus-visible]:outline-olive md:grid-cols-1 md:grid-rows-[8.25rem_auto] md:items-stretch md:gap-y-6"
          >
            <span className="relative flex items-end justify-center max-md:pb-2.5 max-md:after:absolute max-md:after:-inset-x-1.5 max-md:after:bottom-0 max-md:after:h-2 max-md:after:rounded-xs max-md:after:bg-ochre max-md:after:shadow-[0_3px_0] max-md:after:shadow-olive">
              <Jar
                className="block h-auto w-full max-w-[104px] motion-safe:animate-set-down"
                style={{ animationDelay: `${index * 70}ms` }}
              />
            </span>
            <div>
              <Link
                href={href}
                className="font-display text-xl underline decoration-ochre decoration-2 underline-offset-5 group-hover:decoration-olive after:absolute after:inset-0 focus-visible:outline-none"
              >
                {label}
              </Link>
              <p className="mt-1">{text}</p>
            </div>
          </li>
        ))}
      </ul>
    </>
  );
}
