import { useState } from "react";
import { api } from "../api";

/** A list of tags (skills, languages…). `suggest` turns on skill autocomplete; `quick` shows one-click suggestions. */
export default function TagInput({ label, values, onChange, suggest = false, quick = [], placeholder, max = 30 }) {
  const [text, setText] = useState("");
  const [options, setOptions] = useState([]);

  const add = (raw) => {
    const t = raw.trim().toLowerCase().replace(/\s+/g, " ");
    if (t && !values.includes(t) && values.length < max) onChange([...values, t]);
    setText(""); setOptions([]);
  };
  const type = (v) => {
    setText(v);
    if (suggest && v.trim()) api.skillSuggest(v.trim()).then(setOptions).catch(() => {});
  };
  const key = (e) => { if (e.key === "Enter" || e.key === ",") { e.preventDefault(); add(text); } };
  const listId = `list-${label.replace(/\W/g, "")}`;

  return (
    <div>
      <label className="mb-1 block text-sm font-semibold">{label}</label>
      <div className="flex gap-2">
        <input className="field" list={listId} value={text} placeholder={placeholder}
          onChange={(e) => type(e.target.value)} onKeyDown={key} />
        <datalist id={listId}>{options.map((o) => <option key={o} value={o} />)}</datalist>
        <button type="button" onClick={() => add(text)} className="btn-ghost border border-slate-300">Add</button>
      </div>
      <div className="mt-2 flex flex-wrap gap-2">
        {values.map((v) => (
          <span key={v} className="flex items-center gap-1 rounded-full bg-maroon-800 px-3 py-1 text-sm text-white">
            {v}
            <button type="button" aria-label={`Remove ${v}`} onClick={() => onChange(values.filter((x) => x !== v))}>×</button>
          </span>
        ))}
      </div>
      {quick.filter((q) => !values.includes(q.toLowerCase())).length > 0 && (
        <div className="mt-2 text-xs text-slate-500">
          Suggested:{" "}
          {quick.filter((q) => !values.includes(q.toLowerCase())).map((q) => (
            <button key={q} type="button" onClick={() => add(q)}
              className="mr-1 mt-1 rounded-full border border-slate-300 px-2 py-0.5 hover:bg-slate-100">+ {q}</button>
          ))}
        </div>
      )}
    </div>
  );
}
