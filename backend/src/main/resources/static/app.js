const participants = [
  { name: "Payer", x: 70 },
  { name: "UPI App", x: 210 },
  { name: "PSP", x: 360 },
  { name: "Switch", x: 510 },
  { name: "Issuer", x: 660 },
  { name: "Acquirer", x: 810 },
  { name: "PSP(Payee)", x: 960 },
  { name: "Payee App", x: 1100 }
];

const svg = document.getElementById("sequenceSvg");
const eventLogEl = document.getElementById("eventLog");
const statusPill = document.getElementById("statusPill");
let txSocket = null;
let rowY = 110;
let seen = new Set();

initDiagram();

document.getElementById("startBtn").addEventListener("click", startPayment);
document.getElementById("offlineBtn").addEventListener("click", startOfflinePayment);
document.getElementById("refreshBtn").addEventListener("click", refreshCurrentTransaction);

async function startPayment() {
  const payload = {
    clientRequestId: document.getElementById("clientRequestId").value,
    payerVpa: document.getElementById("payerVpa").value,
    payeeVpa: document.getElementById("payeeVpa").value,
    amount: parseFloat(document.getElementById("amount").value),
    mpin: document.getElementById("mpin").value
  };
  const res = await fetch("/api/payments/push", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  const data = await res.json();
  if (!res.ok) {
    appendLog(`ERROR ${JSON.stringify(data)}`);
    return;
  }
  document.getElementById("txId").value = data.transactionId;
  updatePill(data.state);
  resetDiagramRows();
  connectTransactionSocket(data.transactionId);
  await loadEvents(data.transactionId);
}

async function startOfflinePayment() {
  const payload = {
    clientRequestId: document.getElementById("clientRequestId").value + "-offline",
    payerVpa: document.getElementById("payerVpa").value,
    payeeVpa: document.getElementById("payeeVpa").value,
    amount: parseFloat(document.getElementById("amount").value),
    mpin: document.getElementById("mpin").value
  };
  const keyBase64 = "MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=";
  const keyBytes = Uint8Array.from(atob(keyBase64), (c) => c.charCodeAt(0));
  const key = await crypto.subtle.importKey("raw", keyBytes, { name: "AES-GCM" }, false, ["encrypt"]);
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const messageId = `sms-${crypto.randomUUID()}`;
  const encoded = new TextEncoder().encode(JSON.stringify(payload));
  const cipher = await crypto.subtle.encrypt({ name: "AES-GCM", iv, additionalData: new TextEncoder().encode(messageId) }, key, encoded);
  const req = {
    messageId,
    ivBase64: btoa(String.fromCharCode(...iv)),
    cipherTextBase64: btoa(String.fromCharCode(...new Uint8Array(cipher))),
    timestampEpochSeconds: Math.floor(Date.now() / 1000)
  };
  const res = await fetch("/api/offline/sms/submit", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(req)
  });
  const data = await res.json();
  if (!res.ok || !data.accepted) {
    appendLog(`OFFLINE ERROR ${JSON.stringify(data)}`);
    return;
  }
  document.getElementById("txId").value = data.transactionId;
  resetDiagramRows();
  connectTransactionSocket(data.transactionId);
  await loadEvents(data.transactionId);
}

async function refreshCurrentTransaction() {
  const txId = document.getElementById("txId").value.trim();
  if (!txId) return;
  await loadEvents(txId);
  const res = await fetch(`/api/payments/${txId}`);
  if (res.ok) {
    const tx = await res.json();
    updatePill(tx.state);
  }
}

function connectTransactionSocket(txId) {
  if (txSocket) txSocket.close();
  const protocol = location.protocol === "https:" ? "wss" : "ws";
  txSocket = new WebSocket(`${protocol}://${location.host}/ws/transactions/${txId}`);
  txSocket.onmessage = (evt) => {
    const msg = JSON.parse(evt.data);
    renderEvent(msg);
  };
  txSocket.onopen = () => appendLog(`WS connected for tx ${txId}`);
  txSocket.onclose = () => appendLog(`WS disconnected`);
}

async function loadEvents(txId) {
  const res = await fetch(`/api/payments/${txId}/events`);
  if (!res.ok) return;
  const events = await res.json();
  for (const e of events) {
    renderEvent({
      transactionId: txId,
      fromState: e.fromState,
      toState: e.toState,
      actor: e.actor,
      reason: e.reason,
      createdAt: e.createdAt
    });
  }
}

function renderEvent(msg) {
  const k = `${msg.createdAt}|${msg.toState}|${msg.reason}`;
  if (seen.has(k)) return;
  seen.add(k);
  const edge = edgeForState(msg.toState);
  if (edge) drawEdge(edge.from, edge.to, `${msg.toState} - ${msg.reason}`, edge.color);
  appendLog(`${msg.createdAt}  ${msg.toState}  [${msg.actor}] ${msg.reason}`);
  updatePill(msg.toState);
}

function edgeForState(state) {
  switch (state) {
    case "CREATED": return { from: 0, to: 1, color: "#0f766e" };
    case "OFFLINE_QUEUED": return { from: 0, to: 1, color: "#0f766e" };
    case "OFFLINE_RECEIVED": return { from: 1, to: 2, color: "#0f766e" };
    case "OFFLINE_DECRYPTED": return { from: 1, to: 2, color: "#0f766e" };
    case "VALIDATION_PASSED": return { from: 1, to: 2, color: "#0f766e" };
    case "ROUTED_TO_SWITCH": return { from: 2, to: 3, color: "#0f766e" };
    case "DEBIT_REQUESTED": return { from: 3, to: 4, color: "#0f766e" };
    case "DEBIT_FAILED": return { from: 4, to: 2, color: "#b91c1c" };
    case "FAILED_PRE_DEBIT": return { from: 2, to: 0, color: "#b91c1c" };
    case "DEBIT_SUCCESS": return { from: 4, to: 3, color: "#15803d" };
    case "CREDIT_REQUESTED": return { from: 3, to: 5, color: "#15803d" };
    case "CREDIT_FAILED": return { from: 5, to: 3, color: "#b91c1c" };
    case "REVERSAL_REQUESTED": return { from: 3, to: 4, color: "#92400e" };
    case "REVERSED": return { from: 4, to: 0, color: "#92400e" };
    case "COMPLETED": return { from: 5, to: 7, color: "#15803d" };
    default: return null;
  }
}

function initDiagram() {
  svg.innerHTML = `
    <defs>
      <marker id="arrow" markerWidth="8" markerHeight="8" refX="8" refY="4" orient="auto">
        <path d="M0,0 L8,4 L0,8 z" fill="#334155"></path>
      </marker>
    </defs>
  `;
  for (const p of participants) {
    const t = document.createElementNS("http://www.w3.org/2000/svg", "text");
    t.setAttribute("x", p.x - 35);
    t.setAttribute("y", "28");
    t.setAttribute("font-size", "12");
    t.setAttribute("font-weight", "700");
    t.textContent = p.name;
    svg.appendChild(t);

    const l = document.createElementNS("http://www.w3.org/2000/svg", "line");
    l.setAttribute("x1", p.x);
    l.setAttribute("x2", p.x);
    l.setAttribute("y1", "40");
    l.setAttribute("y2", "500");
    l.setAttribute("stroke", "#cbd5e1");
    l.setAttribute("stroke-dasharray", "6 4");
    svg.appendChild(l);
  }
}

function drawEdge(fromIndex, toIndex, label, color) {
  if (rowY > 490) {
    rowY = 110;
    clearDynamicEdges();
  }
  const x1 = participants[fromIndex].x;
  const x2 = participants[toIndex].x;

  const line = document.createElementNS("http://www.w3.org/2000/svg", "line");
  line.setAttribute("x1", String(x1));
  line.setAttribute("x2", String(x2));
  line.setAttribute("y1", String(rowY));
  line.setAttribute("y2", String(rowY));
  line.setAttribute("stroke", color);
  line.setAttribute("stroke-width", "2");
  line.setAttribute("marker-end", "url(#arrow)");
  line.dataset.dynamic = "1";
  line.style.strokeDasharray = "1200";
  line.style.strokeDashoffset = "1200";
  line.style.transition = "stroke-dashoffset 600ms ease";
  svg.appendChild(line);
  requestAnimationFrame(() => { line.style.strokeDashoffset = "0"; });

  const txt = document.createElementNS("http://www.w3.org/2000/svg", "text");
  txt.setAttribute("x", String(Math.min(x1, x2) + 10));
  txt.setAttribute("y", String(rowY - 6));
  txt.setAttribute("font-size", "11");
  txt.setAttribute("fill", "#1e293b");
  txt.dataset.dynamic = "1";
  txt.textContent = label;
  svg.appendChild(txt);

  rowY += 26;
}

function clearDynamicEdges() {
  svg.querySelectorAll("[data-dynamic='1']").forEach((el) => el.remove());
}

function resetDiagramRows() {
  rowY = 110;
  seen = new Set();
  eventLogEl.innerHTML = "";
  clearDynamicEdges();
}

function appendLog(text) {
  const p = document.createElement("p");
  p.textContent = text;
  eventLogEl.prepend(p);
}

function updatePill(state) {
  statusPill.textContent = state;
  statusPill.className = "status-pill";
  if (state === "COMPLETED") statusPill.classList.add("status-ok");
  if (state === "FAILED_PRE_DEBIT") statusPill.classList.add("status-fail");
  if (state === "REVERSED") statusPill.classList.add("status-rev");
}
