import React from "react";

// Lightweight CSS-only replacement for MUI's Tooltip - no portal, no extra dependency.
const Tooltip = ({ title, children }) => {
  return (
    <span className="relative inline-flex group">
      {children}
      <span className="pointer-events-none absolute -top-9 left-1/2 -translate-x-1/2 whitespace-nowrap rounded-md bg-slate-900 px-2 py-1 text-xs text-white opacity-0 shadow-md transition-opacity duration-150 group-hover:opacity-100 dark:bg-neutral-700">
        {title}
      </span>
    </span>
  );
};

export default Tooltip;
