const PATHS = {
  home: "M4 11.5 12 4l8 7.5M6 10v9a1 1 0 0 0 1 1h3v-6h4v6h3a1 1 0 0 0 1-1v-9",
  sparkle:
    "m12 3 1.5 4.2L18 9l-4.5 1.8L12 15l-1.5-4.2L6 9l4.5-1.8L12 3Zm7 12 .8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8L19 15Z",
  building: "M4 21V9l8-5 8 5v12M9 21v-6h6v6M4 21h16",
  chart: "M4 20V10M10 20V4M16 20v-7M4 20h16",
  check: "M20 6 9 17l-5-5",
  calendar:
    "M7 3v4M17 3v4M4 9h16M5 6h14a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1Z",
  people:
    "M17 20v-1a4 4 0 0 0-4-4H7a4 4 0 0 0-4 4v1M10 11a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7ZM21 20v-1a4 4 0 0 0-3-3.87M15.5 4.13a4 4 0 0 1 0 7.75",
  user: "M12 12a4.5 4.5 0 1 0 0-9 4.5 4.5 0 0 0 0 9ZM4 21a8 8 0 0 1 16 0",
  search: "M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16ZM21 21l-4.35-4.35",
  wallet:
    "M4 7h15a2 2 0 0 1 2 2v10H5a2 2 0 0 1-2-2V6a3 3 0 0 1 3-3h12M16 12h5v4h-5a2 2 0 0 1 0-4Z",
  receipt: "M6 3h12v18l-3-2-3 2-3-2-3 2V3ZM9 8h6M9 12h6",
  plus: "M12 5v14M5 12h14",
  info: "M12 11v5M12 8h.01M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z",
  repeat:
    "m17 2 4 4-4 4M3 11V9a3 3 0 0 1 3-3h15M7 22l-4-4 4-4M21 13v2a3 3 0 0 1-3 3H3",
  card: "M3 6h18v13H3V6ZM3 10h18M7 15h4",
  coffee:
    "M4 8h13v6a6 6 0 0 1-6 6h-1a6 6 0 0 1-6-6V8Zm13 2h2a3 3 0 0 1 0 6h-2M7 4v1m4-2v2m4-1v1",
  utensils: "M7 3v8M4 3v5a3 3 0 0 0 6 0V3M7 11v10M17 3v18m0-18c-3 2-4 7 0 10",
  car: "m5 11 2-5h10l2 5M3 12h18v6H3v-6Zm4 5h.01M17 17h.01",
  bag: "M5 8h14l1 13H4L5 8Zm4 0a3 3 0 0 1 6 0",
  target:
    "M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0ZM17 12a5 5 0 1 1-10 0 5 5 0 0 1 10 0ZM12 12h.01",
  "chevron-right": "m9 18 6-6-6-6",
  "chevron-left": "m15 18-6-6 6-6",
  x: "m6 6 12 12M18 6 6 18",
  bell: "M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 0 1-3.46 0",
  settings:
    "M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8ZM12 2v3M12 19v3M4.2 4.2l2.1 2.1M17.7 17.7l2.1 2.1M2 12h3M19 12h3M4.2 19.8l2.1-2.1M17.7 6.3l2.1-2.1",
  edit: "M12 20h9M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5Z",
  menu: "M4 6h16M4 12h16M4 18h16",
};

function DashboardIcon({ name, size = 18, className }) {
  const d = PATHS[name];
  if (!d) return null;
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
    >
      <path d={d} />
    </svg>
  );
}

export default DashboardIcon;
