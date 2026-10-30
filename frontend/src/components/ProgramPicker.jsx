import { useEffect, useState } from "react";

function findPath(node, code, path = []) {
  for (let i = 0; i < node.children.length; i++) {
    const c = node.children[i];
    if (c.code === code) return [...path, i];
    const deeper = findPath(c, code, [...path, i]);
    if (deeper) return deeper;
  }
  return null;
}

export default function ProgramPicker({ tree, value, onChange }) {
  const [sel, setSel] = useState([]);

  const nodeAt = (path) => path.reduce((n, i) => n.children[i], tree);

  useEffect(() => {
    if (!tree) return;
    const current = sel.length ? nodeAt(sel).code : null;
    if (value && current !== value) setSel(findPath(tree, value) || []);
  }, [tree, value]); // eslint-disable-line react-hooks/exhaustive-deps

  if (!tree) return <p className="text-sm text-slate-500">Loading programs…</p>;

  const choose = (level, v) => {
    const next = sel.slice(0, level);
    if (v !== "") next.push(Number(v));
    setSel(next);
    onChange(nextCode(next));
  };
  const nextCode = (path) => nodeAt(path).code || "";

  const group = sel.length > 0 ? nodeAt(sel.slice(0, 1)) : null;
  const program = sel.length > 1 ? nodeAt(sel.slice(0, 2)) : null;
  const hasMajors = program && program.children.length > 0;

  return (
    <div className="space-y-2">
      <select
        className="field"
        value={sel[0] ?? ""}
        onChange={(e) => choose(0, e.target.value)}
      >
        <option value="">Select college / group…</option>
        {tree.children.map((g, i) => (
          <option key={g.name} value={i}>
            {g.name}
          </option>
        ))}
      </select>
      {group && (
        <select
          className="field"
          value={sel[1] ?? ""}
          onChange={(e) => choose(1, e.target.value)}
        >
          <option value="">Select program…</option>
          {group.children.map((p, i) => (
            <option key={p.name} value={i}>
              {p.name}
            </option>
          ))}
        </select>
      )}
      {hasMajors && (
        <select
          className="field"
          value={sel[2] ?? ""}
          onChange={(e) => choose(2, e.target.value)}
        >
          <option value="">
            {program.code ? "No specific major" : "Select major…"}
          </option>
          {program.children.map((m, i) => (
            <option key={m.name} value={i}>
              {m.name}
            </option>
          ))}
        </select>
      )}
    </div>
  );
}
