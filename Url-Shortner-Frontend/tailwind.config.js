
/** @type {import('tailwindcss').Config} */
export default {
  darkMode: "class",
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      backgroundImage: {
        "custom-gradient": "linear-gradient(to right, #140b6cff, #77a3efff)",
        "custom-gradient-footer": "linear-gradient(to right, #140b6cff, #77a3efff, #140b6cff)",
        "custom-gradient-navbar": "linear-gradient(to right, #ffffffff, #ffffffff)",
        "custom-gradient-2": "linear-gradient(to left, #3b82f6, #f43f5e)",
        "card-gradient": "linear-gradient(to right, #38b2ac, #4299e1)",
      },
      colors: {
        navbarColor: "#ffffff",
        btnColor: "#3364F7",
        linkColor: "#2a5bd7",
        // Primary accent scale - used for buttons, links, and highlights in both themes.
        brand: {
          50: "#eef1ff",
          100: "#e0e4ff",
          200: "#c3caff",
          300: "#9ba7ff",
          400: "#7a82fa",
          500: "#5b5ff2",
          600: "#4548d6",
          700: "#3638ab",
          800: "#2c2e87",
          900: "#242566",
        },
      },
      boxShadow: {
        custom: "0 0 15px rgba(0, 0, 0, 0.3)",
        right: "10px 0px 10px -5px rgba(0, 0, 0, 0.3)",
      },
      fontFamily: {
        roboto: ["Roboto", "sans-serif"],
        montserrat: ["Montserrat"],
      },
    },
  },

  variants: {
    extend: {
      backgroundImage: ["responsive"],
    },
  },

  plugins: [],
};
