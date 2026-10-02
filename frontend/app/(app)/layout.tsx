import { AppShell } from "@/components/app-shell";

// Every page in this route group requires a signed-in user; /login sits outside it.
export default function SignedInLayout({ children }: LayoutProps<"/">) {
  return <AppShell>{children}</AppShell>;
}
