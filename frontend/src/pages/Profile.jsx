import { useEffect, useState } from "react";
import { api } from "../api";
import ProgramPicker from "../components/ProgramPicker";
import TagInput from "../components/TagInput";

// Defined outside Profile so typing does not rebuild the inputs (which would drop the focus).
function Row({ label, children }) {
  return <div><label className="mb-1 block text-sm font-semibold">{label}</label>{children}</div>;
}

// Defined outside Profile (same reason as Row): a component declared inside would be rebuilt on every keystroke.
function DeleteAccount({ onDeleted, notify }) {
  const [open, setOpen] = useState(false);
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);

  const confirmDelete = async () => {
    setBusy(true);
    try { await api.deleteAccount(password); onDeleted(); }
    catch (e) { notify(e.message, false); setBusy(false); }
  };

  return (
    <div className="mt-8 border-t pt-4">
      {!open ? (
        <button onClick={() => setOpen(true)} className="text-sm font-semibold text-red-700 underline">Delete my account</button>
      ) : (
        <div className="rounded-xl border border-red-300 bg-red-50 p-4">
          <p className="text-sm font-bold text-red-800">Delete your account?</p>
          <p className="mt-1 text-sm text-red-700">
            This permanently deletes your profile, saved jobs and applications. It cannot be undone.
          </p>
          <input className="field mt-3" type="password" placeholder="Enter your password to confirm" value={password}
            onChange={(e) => setPassword(e.target.value)} />
          <div className="mt-3 flex gap-2">
            <button onClick={() => { setOpen(false); setPassword(""); }} className="btn-ghost border border-slate-300">Cancel</button>
            <button onClick={confirmDelete} disabled={!password || busy}
              className="rounded-lg bg-red-700 px-4 py-2 font-bold text-white disabled:opacity-50">
              {busy ? "Deleting…" : "Yes, delete everything"}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

/** Canva page 8: view and edit the profile. Changing program, role or skills changes your matches. */
export default function Profile({ user, onSaved, onLogout, onDeleted, notify }) {
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
      <DeleteAccount onDeleted={onDeleted} notify={notify} />
    </div>
  );
}
