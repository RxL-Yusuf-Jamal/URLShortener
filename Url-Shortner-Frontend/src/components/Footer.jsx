import React from "react";
import { FaLinkedin, FaGithub } from "react-icons/fa";

const Footer = () => {
  return (
    <footer className="bg-slate-100 dark:bg-neutral-950 border-t border-slate-200 dark:border-neutral-800 text-slate-600 dark:text-neutral-400 py-8 z-40 relative transition-colors duration-200">
      <div className="container mx-auto px-6 lg:px-14 flex flex-col lg:flex-row lg:justify-between items-center gap-4">
        <div className="text-center lg:text-left">
          <h2 className="text-3xl font-bold mb-2 text-slate-900 dark:text-white">
            SnapURL
          </h2>
          <p>Simplifying URL shortening for efficient sharing</p>
          <p className="text-sm">&copy; 2026 SnapURL. All rights reserved.</p>
        </div>

        <div className="flex space-x-6 mt-4 lg:mt-0">
          <a
            href="https://github.com"
            className="hover:text-brand-600 dark:hover:text-brand-400 transition-colors"
          >
            <FaGithub size={24} />
          </a>
          <a
            href=""
            className="hover:text-brand-600 dark:hover:text-brand-400 transition-colors"
          >
            <FaLinkedin size={24} />
          </a>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
