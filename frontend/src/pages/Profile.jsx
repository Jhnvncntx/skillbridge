import { useEffect, useState } from "react";
import { api } from "../api";
import ProgramPicker from "../components/ProgramPicker";
import TagInput from "../components/TagInput";

/** Canva page 8: view and edit the profile. Changing program, role or skills changes your matches. */
export default function Profile({ user, onSaved, onLogout, notify }) {
  const [f, setF] = useState({ ...user, age: String(user.age) });
  const [tree, setTree] = useState(null);
  const [roles, setRoles] = useState([]);
  const [ideas, setIdeas] = useState([]);
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  useEffect(() => { api.tree().then(setTree).catch(() => {}); }, []);
  useEffect(() => {
    if (!f.programCode) return;
    api.roles(f.programCode).then(setRoles).catch(() => {});
    api.programSkills(f.programCode).then(setIdeas).catch(() => {});
  }, [f.programCode]);

  const save = async () => {
    setBusy(true);
    try {
      const updated = await api.updateMe({ ...f, age: Number(f.age) });
      onSaved(updated); notify("Profile saved. Your matches have been updated.", true);
    } catch (e) { notify(e.message, false); }
    finally { setBusy(false); }
  };

  const Row = ({ label, children }) => <div><label className="mb-1 block text-sm font-semibold">{label}</label>{children}</div>;

  return (
    <div className="mx-auto max-w-2xl rounded-3xl bg-white p-6 shadow">
      <h2 className="mb-4 text-2xl font-extrabold text-maroon-900">My Profile</h2>
      <div className="space-y-4">
        <Row label="Email (cannot be changed)"><input className="field bg-slate-100" value={user.email} disabled /></Row>
        <Row label="Full name"><input className="field" value={f.fullName} onChange={set("fullName")} /></Row>
        <div className="grid grid-cols-2 gap-3">
          <Row label="Age"><input className="field" type="number" value={f.age} onChange={set("age")} /></Row>
          <Row label="Gender (optional)">
            <select className="field" value={f.gender} onChange={set("gender")}>
              <option value="UNDISCLOSED">Prefer not to say</option><option value="MALE">Male</option>
              <option value="FEMALE">Female</option><option value="OTHER">Other</option>
            </select>
          </Row>
        </div>
        <Row label="Contact number"><input className="field" value={f.contact} onChange={set("contact")} /></Row>
        <Row label="Home address"><input className="field" value={f.address} onChange={set("address")} /></Row>
        <Row label="Specific degree program">
          <ProgramPicker tree={tree} value={f.programCode} onChange={(code) => code && setF((p) => ({ ...p, programCode: code }))} />
        </Row>
        <div className="grid grid-cols-2 gap-3">
          <Row label="Year of graduation / expected"><input className="field" value={f.gradYear} onChange={set("gradYear")} /></Row>
          <Row label="Target job role">
            <input className="field" list="profile-roles" value={f.targetRole} onChange={set("targetRole")} />
            <datalist id="profile-roles">{roles.map((r) => <option key={r} value={r} />)}</datalist>
          </Row>
        </div>
        <TagInput label="Hard / technical skills" suggest quick={ideas} values={f.hardSkills} onChange={(v) => setF({ ...f, hardSkills: v })} />
        <TagInput label="Soft / personal skills" values={f.softSkills} max={20} onChange={(v) => setF({ ...f, softSkills: v })} />
        <TagInput label="Languages spoken" values={f.languages} max={10} onChange={(v) => setF({ ...f, languages: v })} />
      </div>
      <div className="mt-6 flex justify-between">
        <button onClick={onLogout} className="btn-ghost border border-slate-300">Log out</button>
        <button onClick={save} disabled={busy} className="btn-primary">{busy ? "Saving…" : "Save profile"}</button>
      </div>
    </div>
  );
}
