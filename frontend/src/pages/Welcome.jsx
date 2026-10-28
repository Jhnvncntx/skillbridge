/** Canva page 5. Smaller headings on phones. */
export default function Welcome({ user, onContinue }) {
  const points = ["Program-Specific Precision", "Gender-Inclusive Matching", "100% Digital & Paperless", "Direct Job-Skills Alignment"];
  return (
    <div className="mx-auto grid max-w-5xl items-center gap-6 px-4 py-8 md:grid-cols-2 md:gap-8 md:py-12">
      <div>
        <h2 className="break-words font-serif text-2xl font-bold text-white sm:text-3xl">Welcome, {user.fullName.split(" ")[0]}!</h2>
        <h1 className="mt-2 text-4xl font-extrabold text-white sm:text-6xl">SkillBridge</h1>
        <p className="mt-3 text-3xl font-extrabold leading-tight text-gold sm:text-4xl">Your skills. Your program. Your shot.</p>
        <button onClick={onContinue} className="btn-primary mt-6 w-full py-3 text-lg sm:mt-8 sm:w-auto sm:px-10">CONTINUE</button>
      </div>
      <div className="rounded-3xl bg-white/10 p-5 text-white sm:p-6">
        <p className="mb-3 text-center text-lg font-bold text-gold">Why choose us?</p>
        <ul className="space-y-2">
          {points.map((p) => <li key={p} className="rounded-xl bg-white px-4 py-2 font-semibold text-maroon-900">✔ {p}</li>)}
        </ul>
      </div>
      <p className="text-sm font-bold text-white md:col-span-2">CONNECTING BPSU GRADUATES TO FAIR, SUSTAINABLE, AND SKILL-ALIGNED CAREERS.</p>
    </div>
  );
}
