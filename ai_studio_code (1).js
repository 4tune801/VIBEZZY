/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        vibezzy: {
          black: "#000000",
          surface: "#0D0D0F",
          card: "#1A1A1D",
          elevated: "#242429",
          border: "#2E2E35",
          neon: "#00FF88",
          neonDim: "#00CC6D",
        },
      },
    },
  },
  plugins: [],
};