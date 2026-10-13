"use client";

import { useEffect, useState } from "react";
import { isAdmin, users, type User, type UserAccess } from "@/lib/api";
import { useCurrentUser } from "@/components/app-shell";
import { ErrorMessage } from "@/components/error-message";
import { Loading } from "@/components/loading";
import { PageNotFound } from "@/components/page-not-found";
import { PageHeader } from "@/components/page-header";
import { secondaryButton } from "@/components/styles";
import { formatDateTime } from "@/lib/date";
import { danish } from "@/lib/sort";

// Every account, for an admin: disable or enable another one, and make it an admin or not. The
// admin's own row offers nothing - the backend refuses it, so there is always an enabled admin.
// Anyone else sees "Page not found", as for an address that does not exist, and the list is never
// asked for; the backend would answer 403 regardless. A change answers nothing, so the list is asked for again after it.
export default function UsersPage() {
  const me = useCurrentUser();
  const [list, setList] = useState<User[]>();
  const [error, setError] = useState<unknown>();
  const [saving, setSaving] = useState(false);
  const admin = isAdmin(me);

  useEffect(() => {
    if (admin) {
      users.list().then(setList, setError);
    }
  }, [admin]);

  if (!admin) {
    return <PageNotFound />;
  }

  async function change(user: User, access: UserAccess) {
    setSaving(true);
    try {
      await users.update(user.id, access);
      setList(await users.list());
      setError(undefined);
    } catch (caught) {
      setError(caught);
    }
    setSaving(false);
  }

  return (
    <>
      <PageHeader title="Users" />
      {error !== undefined && <ErrorMessage error={error} />}
      {!list ? (
        error === undefined && <Loading />
      ) : (
        <table className="mt-6 w-full text-left">
          <thead className="border-b">
            <tr>
              <th className="py-2 pr-4">Username</th>
              <th className="py-2 pr-4">Role</th>
              <th className="py-2 pr-4">Status</th>
              <th className="hidden py-2 pr-4 sm:table-cell">Created</th>
              <th className="py-2">
                <span className="sr-only">Actions</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {list
              .toSorted((a, b) => danish(a.username, b.username))
              .map((user) => {
                const userIsAdmin = isAdmin(user);
                return (
                  <tr key={user.id} className="border-b">
                    <td className="py-2 pr-4 break-all">{user.username}</td>
                    <td className="py-2 pr-4">{userIsAdmin ? "Admin" : "User"}</td>
                    <td className="py-2 pr-4">{user.enabled ? "Enabled" : "Disabled"}</td>
                    <td className="hidden py-2 pr-4 sm:table-cell">{formatDateTime(user.created)}</td>
                    <td className="py-2">
                      {user.username !== me.username && (
                        <div className="flex flex-wrap justify-end gap-2">
                          <button
                            type="button"
                            disabled={saving}
                            aria-label={`${user.enabled ? "Disable" : "Enable"} ${user.username}`}
                            onClick={() => change(user, { enabled: !user.enabled, admin: userIsAdmin })}
                            className={secondaryButton}
                          >
                            {user.enabled ? "Disable" : "Enable"}
                          </button>
                          <button
                            type="button"
                            disabled={saving}
                            aria-label={`${userIsAdmin ? "Remove admin" : "Make admin"} ${user.username}`}
                            onClick={() => change(user, { enabled: user.enabled, admin: !userIsAdmin })}
                            className={secondaryButton}
                          >
                            {userIsAdmin ? "Remove admin" : "Make admin"}
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                );
              })}
          </tbody>
        </table>
      )}
    </>
  );
}
