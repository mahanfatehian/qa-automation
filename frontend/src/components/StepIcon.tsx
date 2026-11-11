const paths: Record<string, string> = {
  START: "M6 4h8v12H6zM8 7h4M8 10h4",
  NAVIGATE: "M4 8h12M12 4l4 4-4 4",
  SEARCH_ELEMENT: "M8 8a4 4 0 1 1 0 0M11 11l4 4",
  CLICK: "M8 3v6l2 2 1 5 2-1-1-4 3 1V8z",
  TYPE_TEXT: "M4 5h12M10 5v10M7 15h6",
  EXTRACT_CONTENT: "M4 4h8l4 4v8H4zM12 4v4h4",
  IF_CONDITION: "M10 3l6 7-6 7H6l6-7-6-7z",
  LOOP_ELEMENTS: "M6 6a5 5 0 0 1 8 2M14 14a5 5 0 0 1-8-2M13 5l2 3-3 1M7 15l-2-3 3-1",
  WAIT: "M10 4a6 6 0 1 1 0 12 6 6 0 0 1 0-12M10 7v4l3 2",
  ASSERT: "M4 10l4 4 8-8",
  SCROLL: "M10 3v14M6 7l4-4 4 4M6 13l4 4 4-4",
  HOVER: "M4 14c2-6 10-6 12 0M7 9a3 3 0 0 1 6 0",
  SELECT_OPTION: "M4 6h12v8H4zM4 9h12M12 11l2 2-2 2",
  PRESS_KEY: "M4 7h12v7H4zM7 10h2M11 10h2",
  ENTER_FRAME: "M4 4h12v12H4zM8 8h6v6H8z",
  LEAVE_FRAME: "M8 8h6v6H8zM4 4h12v12",
  SWITCH_TAB: "M4 6h8v8H4zM8 4h8v8",
  ALERT: "M10 3l7 12H3zM10 8v3M10 13v1",
  UPLOAD: "M10 14V6M6 9l4-4 4 4M4 16h12",
  VERIFY: "M4 10a6 6 0 1 1 12 0 6 6 0 0 1-12 0M8 10l2 2 3-3",
  SET_CHECKBOX: "M4 4h8v8H4zM6 8l2 2 3-3",
  DOUBLE_CLICK: "M6 5v8l2 2M11 5v8l2 2",
  RIGHT_CLICK: "M12 4v8H6V8zM12 8h4v6h-4",
  DRAG_DROP: "M4 8h6v6H4zM10 6h6v6",
  REFRESH: "M6 8a5 5 0 0 1 8-2l2 2M14 12a5 5 0 0 1-8 2l-2-2",
  COOKIE: "M10 4a6 6 0 1 1 0 12 6 6 0 0 1 0-12M8 9h1M12 8h1M11 12h1",
  SCREENSHOT: "M4 7h3l1-2h4l1 2h3v8H4zM10 9a3 3 0 1 1 0 6 3 3 0 0 1 0-6",
  GO_FORWARD: "M8 6l6 4-6 4z",
  GO_BACK: "M12 6L6 10l6 4z",
  OPEN_TAB: "M4 6h8v8H4zM10 4h6v6",
  CLOSE_TAB: "M5 5l10 10M15 5L5 15",
  FOCUS: "M10 4v4M10 12v4M4 10h4M12 10h4",
  CLEAR_FIELD: "M4 7h12v7H4zM7 10h6",
  READ_URL: "M4 8h12M6 12h8",
  READ_TITLE: "M4 5h12M6 9h8M6 13h5",
  SET_WINDOW: "M4 4h12v12H4zM4 8h12",
  MAXIMIZE: "M5 5h10v10H5z",
  COUNT_ELEMENTS: "M4 6h4v4H4zM12 6h4v4h-4zM8 12h4v4H8z",
  WAIT_GONE: "M10 4a6 6 0 1 1 0 12M7 7l6 6",
  PAGE_STORAGE: "M5 4h10v4H5zM5 9h10v4H5z",
  SUBMIT: "M4 10h10M10 6l4 4-4 4",
};

export function StepIcon({ type }: { type: string }) {
  const d = paths[type] || "M4 4h12v12H4z";
  return (
    <svg viewBox="0 0 20 20" width="16" height="16" aria-hidden="true" className="shrink-0">
      <path d={d} fill="none" stroke="currentColor" strokeWidth="1.4" strokeLinejoin="round" strokeLinecap="round" />
    </svg>
  );
}
