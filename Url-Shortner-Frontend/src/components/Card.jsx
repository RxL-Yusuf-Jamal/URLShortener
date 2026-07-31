
import React from "react";
import { motion } from "framer-motion";
const Card = ({ title, desc }) => {
  return (
    <motion.div
      initial={{ opacity: 0, y: 120 }}
      whileInView={{
        opacity: 1,
        y: 0,
      }}
      viewport={{ once: true }}
      transition={{ duration: 0.5 }}
      className="shadow-md shadow-slate-200 dark:shadow-none border border-slate-200 dark:border-neutral-800 bg-white dark:bg-neutral-900 flex flex-col px-4 py-8 gap-3 rounded-lg transition-colors duration-200"
    >
      <h1 className="text-slate-900 dark:text-white text-xl font-bold ">{title}</h1>
      <p className="text-slate-700 dark:text-neutral-400 text-sm"> {desc}</p>
    </motion.div>
  );
};

export default Card;
