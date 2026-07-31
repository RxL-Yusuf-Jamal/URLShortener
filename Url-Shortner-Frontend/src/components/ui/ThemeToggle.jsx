import React from "react";
import { HiOutlineSun, HiOutlineMoon } from "react-icons/hi2";
import { useTheme } from "../../contextApi/ThemeContext";
import Tooltip from "./Tooltip";

const ThemeToggle = ({ className = "" }) => {
  const { theme, toggleTheme } = useTheme();
  const isDark = theme === "dark";

  return (
    <Tooltip title={isDark ? "Switch to light mode" : "Switch to dark mode"}>
      <button
        onClick={toggleTheme}
        aria-label="Toggle theme"
        className={`inline-flex items-center justify-center rounded-full p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-700 dark:text-neutral-300 dark:hover:bg-neutral-800 dark:hover:text-white transition-colors duration-150 ${className}`}
      >
        {isDark ? <HiOutlineSun className="text-xl" /> : <HiOutlineMoon className="text-xl" />}
      </button>
    </Tooltip>
  );
};

export default ThemeToggle;
