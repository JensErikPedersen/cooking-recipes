import { EmptyJar } from "@/components/drawings";
import { NoPageHere } from "@/components/page-not-found";
import { SignInFrame } from "@/components/sign-in-frame";

// Any URL no page answers. It renders in the root layout, outside the signed-in frame, since it
// cannot know whether the visitor is signed in - so it is framed as the sign-in pages are, with
// the jar standing empty above it.
export default function NotFound() {
  return (
    <SignInFrame title="Page not found" drawing={<EmptyJar className="mx-auto h-auto w-20" />}>
      <NoPageHere />
    </SignInFrame>
  );
}
