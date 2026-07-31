import React, { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { HiOutlineBars3, HiOutlineXMark, HiOutlineLink } from "react-icons/hi2";
import { useStoreContext } from "../contextApi/ContextApi";
import Button from "./ui/Button";
import ThemeToggle from "./ui/ThemeToggle";

const Navbar = () => {
  const navigate = useNavigate();
  const { token, setToken } = useStoreContext();
  const path = useLocation().pathname;
  const [navbarOpen, setNavbarOpen] = useState(false);

  const onLogOutHandler = () => {
    setToken(null);
    localStorage.removeItem("JWT_TOKEN");
    navigate("/login");
  };

  const linkClass = (isActive) =>
    `font-medium transition-colors ${
      isActive
        ? "text-brand-600 dark:text-brand-400"
        : "text-slate-600 hover:text-slate-900 dark:text-neutral-300 dark:hover:text-white"
    }`;

  return (
    <div className="sticky top-0 z-50">
      {/* --- Top Navbar Row --- */}
      <div className="h-16 bg-white dark:bg-neutral-900 border-b border-slate-200 dark:border-neutral-800 flex items-center">
        <div className="lg:px-14 sm:px-8 px-4 w-full flex justify-between items-center">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-2">
            <HiOutlineLink className="text-2xl text-brand-600 dark:text-brand-400" />
            <h1 className="font-bold text-2xl text-slate-900 dark:text-white">
              SnapURL
            </h1>
          </Link>

          {/* Desktop Menu */}
          <ul className="hidden sm:flex gap-8 items-center">
            <li>
              <Link className={linkClass(path === "/")} to="/">
                Home
              </Link>
            </li>
            <li>
              <Link className={linkClass(path === "/about")} to="/about">
                About
              </Link>
            </li>
            {token && (
              <li>
                <Link className={linkClass(path === "/dashboard")} to="/dashboard">
                  Dashboard
                </Link>
              </li>
            )}
            <li>
              <ThemeToggle />
            </li>
            {!token && (
              <li>
                <Link to="/register">
                  <Button variant="primary">Sign Up</Button>
                </Link>
              </li>
            )}
            {token && (
              <li>
                <Button variant="secondary" onClick={onLogOutHandler}>
                  Log Out
                </Button>
              </li>
            )}
          </ul>

          {/* Mobile Menu Toggle */}
          <div className="sm:hidden flex items-center gap-2">
            <ThemeToggle />
            <button
              onClick={() => setNavbarOpen(!navbarOpen)}
              className="flex items-center text-slate-700 dark:text-neutral-200"
            >
              {navbarOpen ? (
                <HiOutlineXMark className="text-3xl" />
              ) : (
                <HiOutlineBars3 className="text-3xl" />
              )}
            </button>
          </div>
        </div>
      </div>

      {/* --- Mobile Dropdown --- */}
      <div
        className={`sm:hidden transition-all duration-300 overflow-hidden ${
          navbarOpen ? "max-h-96" : "max-h-0"
        }`}
      >
        <ul className="flex flex-col gap-4 px-4 py-4 bg-white dark:bg-neutral-900 border-b border-slate-200 dark:border-neutral-800 shadow-md">
          <li>
            <Link className={linkClass(path === "/")} to="/">
              Home
            </Link>
          </li>
          <li>
            <Link className={linkClass(path === "/about")} to="/about">
              About
            </Link>
          </li>
          {token && (
            <li>
              <Link className={linkClass(path === "/dashboard")} to="/dashboard">
                Dashboard
              </Link>
            </li>
          )}
          {!token && (
            <Link to="/register">
              <Button variant="primary" className="w-full">
                Sign Up
              </Button>
            </Link>
          )}
          {token && (
            <Button variant="secondary" className="w-full" onClick={onLogOutHandler}>
              Log Out
            </Button>
          )}
        </ul>
      </div>
    </div>
  );
};

export default Navbar;
