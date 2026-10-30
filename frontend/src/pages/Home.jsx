import { useCallback, useEffect, useState } from "react";
import { api, applyAndOpen } from "../api";
import JobCard from "../components/JobCard";

function Section({ title, items, empty, go, apply }) {
  return (
    <section className="mb-8">
      <h2 className="mb-3 text-lg font-extrabold uppercase tracking-wide text-white">
        {title}
      </h2>
      {items.length === 0 ? (
        <div className="rounded-2xl bg-white/10 p-4 text-sm text-pink-100">
          {empty}
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {items.map((m) => (
            <JobCard
              key={m.job.id}
              job={m.job}
              score={m.score}
              onOpen={() => go("jobs", { jobId: m.job.id })}
              onApply={() => apply(m.job)}
            />
          ))}
        </div>
      )}
    </section>
  );
}

export default function Home({ user, go, notify }) {
  const [data, setData] = useState(null);
  const [error, setError] = useState("");

  const load = useCallback(() => {
    api
      .home(6)
      .then(setData)
      .catch((e) => setError(e.message));
  }, []);
  useEffect(load, [load, user]);

  const apply = async (job) => {
    const r = await applyAndOpen(job);
    notify(r.message, r.ok);
    load();
  };

  return (
    <div>
      <p className="mb-5 break-words text-sm font-bold text-white">
        YOUR DEGREE PROGRAM:{" "}
        <span className="text-gold">{user.programLabel.toUpperCase()}</span>
        {user.targetRole && (
          <>
            {" "}
            &nbsp;·&nbsp; TARGET ROLE:{" "}
            <span className="text-gold">{user.targetRole.toUpperCase()}</span>
          </>
        )}
      </p>
      {error && (
        <p className="mb-3 rounded bg-red-100 p-2 text-sm text-red-700">
          {error}
        </p>
      )}
      {!data && !error && <p className="text-white">Finding your matches…</p>}
      {data && (
        <>
          <Section
            go={go}
            apply={apply}
            title="Recommended for you:"
            items={data.recommended}
            empty={
              <>
                We don't have postings for your program or target role yet.{" "}
                <button
                  className="font-bold text-gold underline"
                  onClick={() => go("jobs")}
                >
                  Browse all jobs
                </button>{" "}
                or update your target role in your profile.
              </>
            }
          />
          <Section
            go={go}
            apply={apply}
            title="You might also qualify to:"
            items={data.alsoQualify}
            empty="Add more hard skills to your profile to see jobs from other programs that fit your skills."
          />
        </>
      )}
    </div>
  );
}
