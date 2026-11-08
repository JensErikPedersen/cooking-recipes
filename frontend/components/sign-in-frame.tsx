import type { ReactNode } from "react";
import { WheatAndDill } from "@/components/drawings";
import { Label } from "@/components/label";

// The pages outside the signed-in frame - sign in, register, forgot and reset password, and an
// unknown address: one label centred on the page, the app's name at its head and a drawing above
// it (docs/PLAN_V7.md, "Sign in"). The heading, when given, is the page's own h1.
export function SignInFrame({
  title,
  drawing = <WheatAndDill className="mx-auto h-auto w-36" />,
  children,
}: {
  title?: string;
  drawing?: ReactNode;
  children: ReactNode;
}) {
  return (
    <main className="mx-auto w-full max-w-md px-4 pt-8 pb-16 md:px-8 md:pt-16">
      {drawing}
      <Label className="mt-4" fieldClassName="px-6 pt-6 pb-7 md:px-8">
        {/* The label's name, ruled off from what the label says, as on a jar. */}
        <p className="border-b border-ochre pb-3 font-display text-2xl">Cooking Recipes</p>
        {title && <h1 className="mt-5 text-xl">{title}</h1>}
        {children}
      </Label>
    </main>
  );
}
