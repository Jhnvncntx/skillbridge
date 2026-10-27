/** Canva page 5. */
export default function Welcome({ user, onContinue }) {
  const points = ["Program-Specific Precision", "Gender-Inclusive Matching", "100% Digital & Paperless", "Direct Job-Skills Alignment"];
  return (
    <div className="mx-auto grid max-w-5xl items-center gap-8 px-4 py-12 md:grid-cols-2">
      <div>
        <h2 className="font-serif text-3xl font-bold text-white">Welcome, {user.fullName.split(" ")[0]}!</h2>
        <h1 className="mt-2 text-6xl font-extrabold text-white">SkillBridge</h1>
        <p className="mt-3 text-4xl font-extrabold leading-tight text-gold">Your skills. Your program. Your shot.</p>
        <button onClick={onContinue} className="btn-primary mt-8 px-10 py-3 text-lg">CONTINUE</button>
      </div>
      <div className="rounded-3xl bg-white/10 p-6 text-white">
        <p className="mb-3 text-center text-lg font-bold text-gold">Why choose us?</p>
        <ul className="space-y-2">
          {points.map((p) => <li key={p} className="rounded-xl bg-white px-4 py-2 font-semibold text-maroon-900">✔ {p}</li>)}
        </ul>
      </div>
      <p className="text-sm font-bold text-white md:col-span-2">CONNECTING BPSU GRADUATES TO FAIR, SUSTAINABLE, AND SKILL-ALIGNED CAREERS.</p>
    </div>
  );
}
