import React from "react";

const VARIANT_CLASSES = {
  primary:
    "bg-gradient-to-r from-brand-600 to-brand-500 text-white hover:from-brand-500 hover:to-brand-400 shadow-sm shadow-brand-900/30",
  secondary:
    "bg-slate-100 text-slate-700 hover:bg-slate-200 dark:bg-neutral-800 dark:text-neutral-200 dark:hover:bg-neutral-700",
  danger: "bg-red-600 text-white hover:bg-red-500",
  ghost:
    "bg-transparent text-slate-600 hover:bg-slate-100 dark:text-neutral-300 dark:hover:bg-neutral-800",
};

const Button = ({
  variant = "primary",
  icon: Icon,
  iconPosition = "left",
  className = "",
  children,
  ...props
}) => {
  return (
    <button
      className={`inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-semibold transition-colors duration-150 disabled:opacity-50 disabled:cursor-not-allowed ${VARIANT_CLASSES[variant]} ${className}`}
      {...props}
    >
      {Icon && iconPosition === "left" && <Icon className="text-base" />}
      {children}
      {Icon && iconPosition === "right" && <Icon className="text-base" />}
    </button>
  );
};

export default Button;
