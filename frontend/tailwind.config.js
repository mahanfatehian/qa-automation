export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      fontFamily: {
        sans: ["Schibsted Grotesk", "Segoe UI", "sans-serif"],
        mono: ["IBM Plex Mono", "ui-monospace", "monospace"],
      },
      colors: {
        ink: "#081018",
        panel: "#0d1722",
        line: "#1d568c",
      },
    },
  },
  plugins: [],
};
