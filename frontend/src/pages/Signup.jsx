import { useEffect, useState } from "react";
import { api, setToken } from "../api";
import ProgramPicker from "../components/ProgramPicker";
import TagInput from "../components/TagInput";

const EMPTY = {
  fullName: "", age: "", email: "", password: "", contact: "", address: "", gender: "",
  programCode: "", gradYear: "", targetRole: "", hardSkills: [], softSkills: [], languages: [],
};

// NOTE: these helpers must live OUTSIDE Signup. Defined inside, React sees a brand-new component on every
// keystroke, rebuilds the inputs and the cursor focus is lost.
function Card({ step, title, error, children }) {
  return (
    <div className="mx-auto max-w-xl rounded-3xl bg-white p-6 shadow-xl">
      <p className="text-xs font-bold uppercase text-maroon-700">Step {step} of 3</p>
      <h2 className="mb-4 text-2xl font-extrabold text-maroon-900">{title}</h2>
      {children}
      {error && <p className="mt-3 text-sm text-red-600">{error}</p>}
    </div>
  );
}

function Row({ label, children }) {
  return <div><label className="mb-1 block text-sm font-semibold">{label}</label>{children}</div>;
}

/** Canva pages 2-4: personal info -> program and skills -> confirm + privacy notice. */
export default function Signup({ onDone, onBack }) {
  const [step, setStep] = useState(1);
  const [f, setF] = useState(EMPTY);
  const [tree, setTree] = useState(null);
  const [roles, setRoles] = useState([]);
  const [skillIdeas, setSkillIdeas] = useState([]);
  const [consent, setConsent] = useState(false);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  useEffect(() => { api.tree().then(setTree).catch((e) => setError(e.message)); }, []);
  useEffect(() => {
    if (!f.programCode) { setRoles([]); setSkillIdeas([]); return; }
    api.roles(f.programCode).then(setRoles).catch(() => {});
    api.programSkills(f.programCode).then(setSkillIdeas).catch(() => {});
  }, [f.programCode]);

  const next1 = () => {
    if (f.fullName.trim().length < 2) return setError("Please enter your full name.");
    if (!f.age || f.age < 15 || f.age > 100) return setError("Please enter a valid age.");
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(f.email)) return setError("Please enter a valid email address.");
    if (f.password.length < 8) return setError("Password must be at least 8 characters.");
    setError(""); setStep(2);
  };
  const next2 = () => {
    if (!f.programCode) return setError("Please choose your specific degree program (and major, if any).");
    setError(""); setStep(3);
  };

  const submit = async () => {
    setError(""); setBusy(true);
    try {
      const res = await api.signup({ ...f, age: Number(f.age) });
      setToken(res.token);
      onDone(res.user, true);
    } catch (err) { setError(err.message); setBusy(false); }
  };

  return (
    <div className="px-4 py-8">
      {step === 1 && (
        <Card step={step} error={error} title="Let me know you!">
          <div className="space-y-3">
            <Row label="Full name"><input className="field" value={f.fullName} onChange={set("fullName")} /></Row>
            <div className="grid grid-cols-2 gap-3">
              <Row label="Age"><input className="field" type="number" min="15" max="100" value={f.age} onChange={set("age")} /></Row>
              <Row label="Gender (optional)">
                <select className="field" value={f.gender} onChange={set("gender")}>
                  <option value="">Prefer not to say</option>
                  <option value="MALE">Male</option><option value="FEMALE">Female</option><option value="OTHER">Other</option>
                </select>
              </Row>
            </div>
            <Row label="Email address"><input className="field" type="email" value={f.email} onChange={set("email")} /></Row>
            <Row label="Password (8+ characters)"><input className="field" type="password" value={f.password} onChange={set("password")} /></Row>
            <Row label="Contact number"><input className="field" value={f.contact} onChange={set("contact")} /></Row>
            <Row label="Home address"><input className="field" value={f.address} onChange={set("address")} /></Row>
            <p className="text-xs text-slate-500">Gender, age and address are never used to rank jobs.</p>
          </div>
          <div className="mt-5 flex justify-between">
            <button onClick={onBack} className="btn-ghost border border-slate-300">Back to login</button>
            <button onClick={next1} className="btn-primary">NEXT</button>
          </div>
        </Card>
      )}

      {step === 2 && (
        <Card step={step} error={error} title="Let me know you!">
          <div className="space-y-4">
            <Row label="Specific degree program">
              <ProgramPicker tree={tree} value={f.programCode} onChange={(code) => setF((p) => ({ ...p, programCode: code, targetRole: "" }))} />
            </Row>
            <div className="grid grid-cols-2 gap-3">
              <Row label="Year of graduation / expected">
                <input className="field" placeholder="2026" value={f.gradYear} onChange={set("gradYear")} />
              </Row>
              <Row label="Target job role">
                <input className="field" list="role-list" value={f.targetRole} onChange={set("targetRole")}
                  placeholder={f.programCode ? "Choose or type…" : "Pick a program first"} />
                <datalist id="role-list">{roles.map((r) => <option key={r} value={r} />)}</datalist>
              </Row>
            </div>
            <TagInput label="Hard / technical skills" suggest quick={skillIdeas} values={f.hardSkills}
              onChange={(v) => setF({ ...f, hardSkills: v })} placeholder="e.g. java, autocad" />
            <TagInput label="Soft / personal skills" values={f.softSkills} max={20}
              onChange={(v) => setF({ ...f, softSkills: v })} placeholder="e.g. teamwork" />
            <TagInput label="Languages spoken" values={f.languages} max={10}
              onChange={(v) => setF({ ...f, languages: v })} placeholder="e.g. english, filipino" />
          </div>
          <div className="mt-5 flex justify-between">
            <button onClick={() => { setError(""); setStep(1); }} className="btn-ghost border border-slate-300">Back</button>
            <button onClick={next2} className="btn-primary">NEXT</button>
          </div>
        </Card>
      )}

      {step === 3 && (
        <Card step={step} error={error} title="Are you sure that all of the details are correct?">
          <dl className="grid grid-cols-3 gap-x-3 gap-y-1 text-sm">
            {[["Name", f.fullName], ["Age", f.age], ["Email", f.email], ["Contact", f.contact || "—"], ["Address", f.address || "—"],
              ["Gender", f.gender ? f.gender.toLowerCase() : "not stated"], ["Program code", f.programCode], ["Graduation", f.gradYear || "—"],
              ["Target role", f.targetRole || "—"], ["Hard skills", f.hardSkills.join(", ") || "—"],
              ["Soft skills", f.softSkills.join(", ") || "—"], ["Languages", f.languages.join(", ") || "—"]].map(([k, v]) => (
              <div key={k} className="contents"><dt className="font-semibold">{k}</dt><dd className="col-span-2">{v}</dd></div>
            ))}
          </dl>
          <p className="mt-4 rounded bg-slate-100 p-3 text-xs leading-relaxed text-slate-700">
            The personal information collected through this app will be used solely for the purpose stated in this app.
            It will be kept confidential and processed in accordance with the Data Privacy Act of 2012 (Republic Act No. 10173).
            It will not be shared with unauthorized third parties without your consent, unless required by law.
          </p>
          <label className="mt-3 flex items-start gap-2 text-sm">
            <input type="checkbox" className="mt-1" checked={consent} onChange={(e) => setConsent(e.target.checked)} />
            I have read and understood this privacy notice and consent to the collection and processing of my personal data.
          </label>
          <div className="mt-5 flex justify-between">
            <button onClick={() => { setError(""); setStep(2); }} className="btn-ghost border border-slate-300">Edit details</button>
            <button onClick={submit} disabled={!consent || busy} className="btn-primary">{busy ? "Creating…" : "CREATE ACCOUNT"}</button>
          </div>
        </Card>
      )}
    </div>
  );
}
