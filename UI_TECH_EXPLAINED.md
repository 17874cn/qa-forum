# QA Forum UI — Technology Deep Dive (Interview Guide)

> This document explains every technology, concept, and pattern used in
> `src/main/resources/static/index.html` — the single-page frontend of the QA Forum.
> Written to help you answer interview questions confidently.

---

## 1. What Language Is It Written In?

The file uses **three web-standard languages** — no frameworks, no build tools.

| Layer | Language | Purpose |
|-------|----------|---------|
| Structure | **HTML5** | Page elements, forms, buttons |
| Styling | **CSS3** | Layout, colors, animations |
| Logic | **Vanilla JavaScript (ES2020+)** | API calls, DOM updates, routing |

It is served as a **static file** directly by Spring Boot from
`src/main/resources/static/` — no Node.js, no React, no bundler needed.

---

## 2. HTML5 Concepts Used

### `<!DOCTYPE html>`
Tells the browser to use modern HTML5 parsing (not legacy quirks mode).

### Semantic Tags
```html
<nav>   — top navigation bar
<div>   — layout containers
<form>  — not used directly; forms are plain divs with inputs for SPA control
```

### `<meta viewport>`
```html
<meta name="viewport" content="width=device-width, initial-scale=1.0" />
```
Makes the layout **mobile-responsive** by preventing the browser from zooming out on small screens.

### `data-*` Attributes
Not used here, but a related concept — IDs are used instead (`id="page-auth"`) for direct DOM lookup.

### Input Types
```html
<input type="text" />   — plain text
<input type="email" />  — triggers email keyboard on mobile + basic browser validation
<textarea>              — multi-line text input
```

---

## 3. CSS3 Concepts Used

### 3.1 CSS Custom Properties (Variables)
```css
:root {
  --primary: #0a7bd6;
  --bg: #f6f6f6;
}
/* Usage */
color: var(--primary);
```
**Why?** Define colours once at the top. Change one variable to retheme the whole app.
**Interview Q:** *"What is the difference between CSS variables and Sass variables?"*
→ CSS variables are live (change at runtime with JS), Sass variables compile away.

---

### 3.2 Flexbox Layout
Used everywhere for alignment.
```css
.nav-inner {
  display: flex;
  align-items: center;  /* vertical centre */
  gap: 12px;
}
.nav-spacer { flex: 1; } /* pushes right-side items to the far right */
```
**Key Flexbox properties used:**
| Property | Effect |
|----------|--------|
| `display: flex` | Enables flexbox |
| `align-items: center` | Cross-axis (vertical) alignment |
| `justify-content: center` | Main-axis (horizontal) alignment |
| `flex-direction: column` | Stacks children vertically |
| `flex: 1` | Makes element grow to fill remaining space |
| `flex-wrap: wrap` | Wraps children to next line if needed |
| `gap` | Space between flex children (no margin hacks) |

---

### 3.3 CSS Pseudo-Classes
```css
.question-card:first-child  { border-radius: 8px 8px 0 0; }
.question-card:last-child   { border-radius: 0 0 8px 8px; }
.question-card:only-child   { border-radius: 8px; }
.btn:hover:not(:disabled)   { background: var(--primary-dark); }
input:focus                 { box-shadow: 0 0 0 3px rgba(...); }
```
**Interview Q:** *"What does `:not()` do in CSS?"*
→ Selects elements that do NOT match the given selector. Here it prevents hover styles on disabled buttons.

---

### 3.4 CSS Transitions & Animations
```css
/* Smooth hover effect */
.btn { transition: all 0.15s; }

/* Spinning loader */
@keyframes spin {
  to { transform: rotate(360deg); }
}
.spinner { animation: spin 0.7s linear infinite; }

/* Toast slide-up */
#toast {
  transform: translateY(80px);
  opacity: 0;
  transition: all 0.25s;
}
#toast.show { transform: translateY(0); opacity: 1; }
```
**Interview Q:** *"What is the difference between `transition` and `animation`?"*
→ `transition` reacts to state changes (hover, class add). `animation` runs on its own with keyframes.

---

### 3.5 CSS `calc()`
```css
.auth-wrap {
  min-height: calc(100vh - 52px);
}
```
Subtracts the navbar height (52px) from the full viewport height so the auth card is always vertically centred.

---

### 3.6 `position: sticky`
```css
nav {
  position: sticky;
  top: 0;
  z-index: 100;
}
```
The navbar sticks to the top of the viewport when scrolling, without JavaScript.

---

### 3.7 Multi-line Text Clamp
```css
.q-body {
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
```
Limits the question preview to 2 lines with a `…` ellipsis — pure CSS, no JS needed.

---

### 3.8 Responsive Design (Media Query)
```css
@media (max-width: 600px) {
  .question-detail-body { flex-direction: column; }
  .vote-col { flex-direction: row; }
}
```
On mobile screens, the vote column shifts from vertical to horizontal layout.

---

## 4. JavaScript Concepts Used

### 4.1 Single-Page Application (SPA) Pattern
There is **no page reload**. All 4 screens (auth, questions list, ask, detail) exist in the DOM simultaneously. Only one is visible at a time via CSS:
```css
.page { display: none; }
.page.active { display: block; }
```
```js
function showPage(id) {
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  document.getElementById('page-' + id).classList.add('active');
}
```
**Interview Q:** *"How does a SPA differ from a traditional multi-page app?"*
→ SPA loads one HTML file; navigation happens via JS without server round-trips. Faster perceived performance, but requires manual state management.

---

### 4.2 IIFE — Immediately Invoked Function Expression
```js
(function init() {
  const saved = sessionStorage.getItem('qa_user');
  if (saved) {
    currentUser = JSON.parse(saved);
    updateNav();
    showQuestionsPage();
  }
})();
```
Runs automatically when the script loads to restore login state. Parentheses around `function` make it an expression; `()` at the end calls it immediately.

---

### 4.3 `async` / `await` & the Fetch API
```js
async function doLogin() {
  const user = await apiFetch('/api/users/login', {
    method: 'POST',
    body: JSON.stringify({ username }),
  });
}
```
`fetch()` is the browser's built-in HTTP client. It returns a **Promise**. `await` pauses execution until the Promise resolves — making async code look synchronous.

**The `apiFetch` wrapper:**
```js
async function apiFetch(path, options = {}) {
  const res = await fetch(API + path, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  if (res.status === 204) return null;
  return res.json();
}
```
Centralises error handling, JSON parsing, and headers — so all API calls share the same logic.

---

### 4.4 `sessionStorage` for State Persistence
```js
// Save on login
sessionStorage.setItem('qa_user', JSON.stringify(user));

// Restore on page load
const saved = sessionStorage.getItem('qa_user');
currentUser = JSON.parse(saved);

// Clear on logout
sessionStorage.removeItem('qa_user');
```
**Interview Q:** *"What is the difference between `localStorage` and `sessionStorage`?"*
| | `localStorage` | `sessionStorage` |
|--|--|--|
| Lifetime | Persists forever | Cleared when tab closes |
| Scope | All tabs (same origin) | Current tab only |
| Use case | Remember me | Single-session login |

---

### 4.5 Template Literals for Dynamic HTML
```js
list.innerHTML = questions.map(q => `
  <div class="question-card" onclick="openQuestion('${q.id}')">
    <div class="q-title">${escHtml(q.title)}</div>
    <span class="q-meta">${relativeTime(q.createdAt)}</span>
  </div>
`).join('');
```
Backtick strings allow multi-line strings and embedded expressions (`${}`). `.map().join('')` converts an array of question objects into an HTML string.

---

### 4.6 XSS Prevention — `escHtml()`
```js
function escHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}
```
**Cross-Site Scripting (XSS):** If a user submits `<script>alert('hacked')</script>` as a question title and you inject it raw into innerHTML, the script executes.
`escHtml()` converts `<` to `&lt;` so the browser renders it as text, not code.

**Interview Q:** *"How do you prevent XSS in a frontend?"*
→ Always escape user-supplied content before inserting into innerHTML. Prefer `textContent` over `innerHTML` when no HTML is needed.

---

### 4.7 Array Methods
```js
// map — transform array items
questions.map(q => `<div>${q.title}</div>`)

// filter — keep matching items
allQuestions.filter(item => item.title.toLowerCase().includes(query))

// sort — reorder (accepted answers first, then by votes)
answers.sort((a, b) => {
  if (a.accepted && !b.accepted) return -1;
  return (b.votes || 0) - (a.votes || 0);
})

// find — get first match
allQuestions.find(q => q.id === id)

// findIndex — get index of match
allQuestions.findIndex(q => q.id === updated.id)

// unshift — prepend to array
allQuestions.unshift(newQuestion);
```

---

### 4.8 Spread Operator
```js
// Merge object + override votes field
apiFetch('/api/questions/' + id, {
  method: 'PUT',
  body: JSON.stringify({ ...currentQuestion, votes: currentQuestion.votes + delta }),
});
```
`...currentQuestion` copies all fields from the existing question object, then `votes:` overrides just that one field.

---

### 4.9 Optional Chaining (`?.`)
```js
authorId: currentUser?.id || currentUser?.username || 'anonymous'
```
If `currentUser` is `null`, `?.id` returns `undefined` instead of throwing `TypeError: Cannot read property 'id' of null`.

---

### 4.10 Regular Expression for Email Validation
```js
if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
  showAlert('register-error', 'Please enter a valid email address.');
}
```
| Part | Meaning |
|------|---------|
| `^` | Start of string |
| `[^\s@]+` | One or more chars that are not space or `@` |
| `@` | Literal `@` symbol |
| `[^\s@]+` | Domain name |
| `\.` | Literal dot |
| `[^\s@]+$` | TLD (`.com`, `.org`, etc.) |

---

### 4.11 DOM Manipulation
```js
// Read input value
document.getElementById('login-username').value.trim()

// Set text
document.getElementById('detail-title').textContent = q.title;

// Show/hide element
document.getElementById('navUser').style.display = 'flex';

// Add/remove CSS class
el.classList.add('show');
el.classList.remove('show');
el.classList.toggle('active', condition);

// Query all matching elements
document.querySelectorAll('.page').forEach(p => ...)
```

---

### 4.12 Event Listeners
```js
// Inline handlers in HTML
<button onclick="doLogin()">Sign In</button>

// addEventListener for keyboard shortcut
document.getElementById('login-username').addEventListener('keydown', e => {
  if (e.key === 'Enter') doLogin();
});

// oninput for live search
<input oninput="filterQuestions()" />
```

---

### 4.13 Client-Side Caching
```js
let allQuestions = [];  // cache of all questions from API

async function openQuestion(id) {
  const cached = allQuestions.find(q => q.id === id);
  if (cached) { showDetailPage(cached); return; }  // no API call needed
  const q = await apiFetch('/api/questions/' + id);
  showDetailPage(q);
}
```
Avoids redundant network calls if the question is already in memory.

---

### 4.14 Toast Notification System
```js
let toastTimer;
function toast(msg, type = '') {
  const el = document.getElementById('toast');
  el.textContent = msg;
  el.className = 'show' + (type ? ' ' + type : '');
  clearTimeout(toastTimer);       // cancel any existing dismiss timer
  toastTimer = setTimeout(() => { el.className = ''; }, 3000);
}
```
Uses CSS `transform` + `opacity` transitions (GPU-accelerated) for smooth slide-in/out. `clearTimeout` prevents multiple toasts stacking timers.

---

## 5. Architecture Pattern — How It All Connects

```
Browser loads index.html (HTML + CSS + JS in one file)
        │
        ▼
init() runs → checks sessionStorage
        │
   ┌────┴────┐
   │logged in│          ─── showQuestionsPage() → GET /api/questions
   └─────────┘
        │
   ┌────┴──────────────────┐
   │ Auth Screen           │  POST /api/users        (register)
   │ Questions List        │  GET  /api/questions    (list)
   │ Ask Question          │  POST /api/questions    (create)
   │ Question Detail       │  GET  /api/questions/id (detail)
   │   + Answers           │  GET  /api/questions/id/answers
   │   + Vote/Accept       │  PUT  /api/answers/id/vote
   └───────────────────────┘  PUT  /api/answers/id/accept
```

**State lives in three module-level variables:**
```js
let currentUser = null;       // logged-in user object
let allQuestions = [];        // cached question list
let currentQuestion = null;   // currently viewed question
```

---

## 6. Why No Framework?

| Concern | Vanilla JS approach used here |
|---------|------------------------------|
| Routing | `display:none/block` + `classList.add('active')` |
| State | Module-level `let` variables |
| Templates | Template literals + `innerHTML` |
| HTTP | `fetch()` API |
| Reactivity | Manual DOM updates after API calls |

**Interview Q:** *"When would you choose vanilla JS over React/Vue?"*
→ For small, self-contained tools where you want zero dependencies, fast load time, and no build step. For large apps with complex state and many components, a framework is better.

---

## 7. How Spring Boot Serves This File

Spring Boot's auto-configuration automatically serves everything in
`src/main/resources/static/` at the root URL:

```
src/main/resources/static/index.html  →  http://localhost:8081/
src/main/resources/static/index.html  →  http://localhost:8081/index.html
```

No controller, no configuration needed — Spring's `ResourceHandlerRegistry`
handles this out of the box for both Spring MVC and Spring WebFlux.

The frontend calls the backend on the **same origin** (`localhost:8081`), so
there are no CORS issues. The `API` constant is set to empty string `''`:
```js
const API = '';  // → fetch('/api/questions') goes to localhost:8081/api/questions
```

---

## 8. Quick-Reference: Interview Questions & Answers

| Question | Answer |
|----------|--------|
| What is a SPA? | A web app that loads one HTML page and swaps content via JS without full page reloads |
| What is the Fetch API? | Browser built-in for making HTTP requests, returns Promises |
| What is `async/await`? | Syntax sugar over Promises; `await` pauses the function until the Promise settles |
| What is XSS? | Cross-Site Scripting — injecting malicious scripts via user input into the DOM |
| How do you prevent XSS? | Escape user content before inserting into innerHTML; prefer `textContent` |
| What is `sessionStorage`? | Browser key-value store that clears when the tab closes |
| What is a CSS variable? | A reusable value defined with `--name` and used with `var(--name)` |
| What is Flexbox? | CSS layout model for distributing space among items in a container |
| What is optional chaining `?.`? | Safely accesses nested properties without throwing if the parent is null/undefined |
| What does `@keyframes` do? | Defines animation steps; used with `animation` property to run them |
| What is an IIFE? | A function that defines and calls itself immediately on script load |
