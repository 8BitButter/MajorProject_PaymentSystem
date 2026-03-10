const nodes = [
  { id: "INITIATE", label: "Initiate", x: 70, y: 150 },
  { id: "VALIDATE", label: "Validate", x: 250, y: 150 },
  { id: "ROUTE", label: "Route", x: 430, y: 150 },
  { id: "DEBIT", label: "Debit", x: 610, y: 150 },
  { id: "CREDIT", label: "Credit", x: 790, y: 150 },
  { id: "REVERSAL", label: "Reversal", x: 970, y: 220 },
  { id: "TERMINAL", label: "Terminal", x: 1120, y: 150 }
];

const edgeMap = {
  INITIATE: ["INITIATE", "VALIDATE"],
  VALIDATE: ["VALIDATE", "ROUTE"],
  ROUTE: ["ROUTE", "DEBIT"],
  DEBIT: ["DEBIT", "CREDIT"],
  CREDIT: ["CREDIT", "TERMINAL"],
  REVERSAL: ["REVERSAL", "TERMINAL"]
};

const flowSvg = document.getElementById("flowSvg");
const timelineLog = document.getElementById("timelineLog");
const payeeLog = document.getElementById("payeeLog");
const txState = document.getElementById("txState");
const payeeLatest = document.getElementById("payeeLatest");
const payeeConn = document.getElementById("payeeConn");

let txSocket = null;
let payeeSocket = null;
let currentTxId = null;
let seenSteps = new Set();
let eventQueue = [];
let processingQueue = false;
let paused = false;
let speedFactor = 1;

initFlowSvg();
bindUi();
loadDelayProfile();

function bindUi() {
  document.getElementById("startBtn").addEventListener("click", startPush);
  document.getElementById("offlineBtn").addEventListener("click", startOfflineSms);
  document.getElementById("refreshTxBtn").addEventListener("click", refreshCurrentTx);
  document.getElementById("pauseBtn").addEventListener("click", () => { paused = true; });
  document.getElementById("resumeBtn").addEventListener("click", () => {
    paused = false;
    processQueue();
  });
  document.getElementById("speedFactor").addEventListener("change", (e) => {
    speedFactor = parseFloat(e.target.value);
  });
  document.getElementById("queryAccountsBtn").addEventListener("click", queryAccounts);
  document.getElementById("saveDelayBtn").addEventListener("click", saveDelayProfile);

  document.querySelectorAll(".failureToggle").forEach((checkbox) => {
    checkbox.addEventListener("change", async () => {
      const scenario = checkbox.dataset.scenario;
      const mode = checkbox.checked ? "enable" : "disable";
      await fetch(`/api/v1/admin/failures/${scenario}/${mode}`, { method: "POST" });
    });
  });
}

async function startPush() {
  resetFlowView();
  const payload = {
    clientRequestId: document.getElementById("clientRequestId").value.trim(),
    payerVpa: document.getElementById("payerVpa").value.trim(),
    payeeVpa: document.getElementById("payeeVpa").value.trim(),
    amount: parseFloat(document.getElementById("amount").value),
    mpin: document.getElementById("mpin").value
  };

  const res = await fetch("/api/v1/payer/push", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  const data = await res.json();
  if (!res.ok) {
    appendTimeline(`ERROR: ${JSON.stringify(data)}`);
    txState.textContent = "ERROR";
    txState.className = "pill fail";
    return;
  }

  currentTxId = data.transactionId;
  document.getElementById("txId").value = currentTxId;
  txState.textContent = data.state;
  txState.className = "pill neutral";

  connectTxSocket(currentTxId);
  connectPayeeSocket(payload.payeeVpa);
  await loadTimeline(currentTxId);
}

async function startOfflineSms() {
  resetFlowView();
  const pushPayload = {
    clientRequestId: `${document.getElementById("clientRequestId").value.trim()}-offline`,
    payerVpa: document.getElementById("payerVpa").value.trim(),
    payeeVpa: document.getElementById("payeeVpa").value.trim(),
    amount: parseFloat(document.getElementById("amount").value),
    mpin: document.getElementById("mpin").value
  };
  const keyBase64 = "MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=";
  const keyBytes = Uint8Array.from(atob(keyBase64), (c) => c.charCodeAt(0));
  const key = await crypto.subtle.importKey("raw", keyBytes, { name: "AES-GCM" }, false, ["encrypt"]);
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const messageId = `sms-${crypto.randomUUID()}`;
  const payloadBytes = new TextEncoder().encode(JSON.stringify(pushPayload));
  const encrypted = await crypto.subtle.encrypt(
    { name: "AES-GCM", iv, additionalData: new TextEncoder().encode(messageId) },
    key,
    payloadBytes
  );

  const req = {
    messageId,
    ivBase64: btoa(String.fromCharCode(...iv)),
    cipherTextBase64: btoa(String.fromCharCode(...new Uint8Array(encrypted))),
    timestampEpochSeconds: Math.floor(Date.now() / 1000)
  };
  const res = await fetch("/api/offline/sms/submit", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(req)
  });
  const data = await res.json();
  if (!res.ok || !data.accepted) {
    appendTimeline(`OFFLINE ERROR: ${JSON.stringify(data)}`);
    return;
  }

  currentTxId = data.transactionId;
  document.getElementById("txId").value = currentTxId;
  txState.textContent = "OFFLINE_ACCEPTED";
  txState.className = "pill neutral";
  connectTxSocket(currentTxId);
  connectPayeeSocket(pushPayload.payeeVpa);
  await loadTimeline(currentTxId);
}

async function refreshCurrentTx() {
  const txId = document.getElementById("txId").value.trim();
  if (!txId) return;
  await loadTimeline(txId);
  const res = await fetch(`/api/v1/transactions/${txId}`);
  if (res.ok) {
    const tx = await res.json();
    updateTerminalPill(tx.state);
  }
}

function connectTxSocket(txId) {
  if (txSocket) txSocket.close();
  const wsProto = location.protocol === "https:" ? "wss" : "ws";
  txSocket = new WebSocket(`${wsProto}://${location.host}/ws/transactions/${encodeURIComponent(txId)}`);
  txSocket.onopen = () => appendTimeline(`WS tx connected: ${txId}`);
  txSocket.onclose = () => appendTimeline("WS tx disconnected");
  txSocket.onmessage = (evt) => {
    const msg = JSON.parse(evt.data);
    if (msg.eventType !== "STAGE_DECISION") return;
    enqueueStageEvent(msg);
  };
}

function connectPayeeSocket(payeeVpa) {
  if (payeeSocket) payeeSocket.close();
  const wsProto = location.protocol === "https:" ? "wss" : "ws";
  payeeSocket = new WebSocket(`${wsProto}://${location.host}/ws/users/${encodeURIComponent(payeeVpa)}`);
  payeeSocket.onopen = () => {
    payeeConn.textContent = "CONNECTED";
    payeeConn.className = "pill ok";
  };
  payeeSocket.onclose = () => {
    payeeConn.textContent = "DISCONNECTED";
    payeeConn.className = "pill neutral";
  };
  payeeSocket.onmessage = (evt) => {
    const msg = JSON.parse(evt.data);
    if (msg.eventType !== "STAGE_DECISION") return;
    const stage = msg.stage || "UNKNOWN";
    payeeLatest.textContent = `Latest recipient event: ${stage} (${msg.status})`;
    prepend(payeeLog, `${msg.endedAt || msg.createdAt} | ${stage} | ${msg.reason}`);
  };
}

async function loadTimeline(txId) {
  const res = await fetch(`/api/v1/transactions/${txId}/timeline`);
  if (!res.ok) return;
  const steps = await res.json();
  steps.forEach((step) => enqueueStageEvent({
    eventType: "STAGE_DECISION",
    stepId: step.id,
    transactionId: txId,
    stage: step.stage,
    status: step.status,
    actor: step.actor,
    reason: step.reason,
    nextStage: step.nextStage,
    processingMs: step.processingMs,
    branch: step.branch,
    startedAt: step.startedAt,
    endedAt: step.endedAt
  }));
}

function enqueueStageEvent(evt) {
  const key = evt.stepId ? String(evt.stepId) : `${evt.stage}|${evt.endedAt}|${evt.reason}`;
  if (seenSteps.has(key)) return;
  seenSteps.add(key);
  eventQueue.push(evt);
  processQueue();
}

async function processQueue() {
  if (processingQueue || paused || eventQueue.length === 0) return;
  processingQueue = true;
  const evt = eventQueue.shift();
  const stage = evt.stage;
  const delay = Math.max(80, Math.floor((evt.processingMs || 250) / speedFactor));

  setNodeState(stage, "active");
  highlightEdge(stage, evt.status, evt.branch);
  appendTimeline(`${evt.endedAt || evt.createdAt} | ${stage} | ${evt.status} | ${evt.reason}`);

  await wait(delay);

  setNodeState(stage, evt.status === "FAIL" ? "fail" : "done");
  if (stage === "TERMINAL" || evt.nextStage === "TERMINAL" || evt.status === "FAIL") {
    updateTerminalPill(evt.reason.includes("success") || evt.reason.includes("completed") ? "COMPLETED" : "FAILED");
  }
  processingQueue = false;
  processQueue();
}

function initFlowSvg() {
  flowSvg.innerHTML = `
    <defs>
      <marker id="arrowOk" markerWidth="8" markerHeight="8" refX="8" refY="4" orient="auto">
        <path d="M0,0 L8,4 L0,8 z" fill="#0f766e"></path>
      </marker>
      <marker id="arrowFail" markerWidth="8" markerHeight="8" refX="8" refY="4" orient="auto">
        <path d="M0,0 L8,4 L0,8 z" fill="#b91c1c"></path>
      </marker>
      <marker id="arrowRev" markerWidth="8" markerHeight="8" refX="8" refY="4" orient="auto">
        <path d="M0,0 L8,4 L0,8 z" fill="#a16207"></path>
      </marker>
    </defs>
  `;

  nodes.forEach((n) => {
    const g = document.createElementNS("http://www.w3.org/2000/svg", "g");
    g.setAttribute("id", `node-${n.id}`);
    g.setAttribute("class", "flow-node");

    const rect = document.createElementNS("http://www.w3.org/2000/svg", "rect");
    rect.setAttribute("x", String(n.x - 54));
    rect.setAttribute("y", String(n.y - 28));
    rect.setAttribute("rx", "12");
    rect.setAttribute("ry", "12");
    rect.setAttribute("width", "108");
    rect.setAttribute("height", "56");
    rect.setAttribute("class", "node-box");
    g.appendChild(rect);

    const text = document.createElementNS("http://www.w3.org/2000/svg", "text");
    text.setAttribute("x", String(n.x));
    text.setAttribute("y", String(n.y + 5));
    text.setAttribute("text-anchor", "middle");
    text.setAttribute("class", "node-label");
    text.textContent = n.label;
    g.appendChild(text);
    flowSvg.appendChild(g);
  });
}

function highlightEdge(stage, status, branch) {
  const edge = edgeMap[stage];
  if (!edge) return;
  const from = nodes.find((n) => n.id === edge[0]);
  const to = nodes.find((n) => n.id === edge[1]);
  if (!from || !to) return;

  const color = branch === "REVERSAL" ? "#a16207" : status === "FAIL" ? "#b91c1c" : "#0f766e";
  const marker = branch === "REVERSAL" ? "url(#arrowRev)" : status === "FAIL" ? "url(#arrowFail)" : "url(#arrowOk)";

  const line = document.createElementNS("http://www.w3.org/2000/svg", "line");
  line.setAttribute("x1", String(from.x + 56));
  line.setAttribute("y1", String(from.y));
  line.setAttribute("x2", String(to.x - 56));
  line.setAttribute("y2", String(to.y));
  line.setAttribute("stroke", color);
  line.setAttribute("stroke-width", "3");
  line.setAttribute("marker-end", marker);
  line.classList.add("edge-anim");
  flowSvg.appendChild(line);
  setTimeout(() => line.classList.add("fade"), 1500);
}

function setNodeState(stage, state) {
  const node = document.getElementById(`node-${stage}`);
  if (!node) return;
  node.classList.remove("active", "done", "fail");
  if (state) node.classList.add(state);
}

function resetFlowView() {
  seenSteps = new Set();
  eventQueue = [];
  processingQueue = false;
  paused = false;
  timelineLog.textContent = "";
  payeeLog.textContent = "";
  payeeLatest.textContent = "No recipient events yet.";
  flowSvg.querySelectorAll(".edge-anim").forEach((edge) => edge.remove());
  nodes.forEach((n) => setNodeState(n.id, ""));
}

async function saveDelayProfile() {
  const payload = {
    baseDelayMs: Number(document.getElementById("baseDelayMs").value),
    jitterMs: Number(document.getElementById("jitterMs").value)
  };
  await fetch("/api/v1/admin/delay-profile", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
}

async function loadDelayProfile() {
  const res = await fetch("/api/v1/admin/delay-profile");
  if (!res.ok) return;
  const profile = await res.json();
  document.getElementById("baseDelayMs").value = profile.baseDelayMs;
  document.getElementById("jitterMs").value = profile.jitterMs;
}

async function queryAccounts() {
  const vpa = document.getElementById("queryUserVpa").value.trim();
  const bankType = document.getElementById("queryBankType").value;
  const params = new URLSearchParams();
  if (vpa) params.set("userVpa", vpa);
  if (bankType) params.set("bankType", bankType);

  const res = await fetch(`/api/v1/admin/accounts?${params.toString()}`);
  const data = res.ok ? await res.json() : [];
  document.getElementById("accountDump").textContent = JSON.stringify(data, null, 2);
}

function appendTimeline(text) {
  prepend(timelineLog, text);
}

function updateTerminalPill(state) {
  txState.textContent = state;
  txState.className = "pill neutral";
  if (state === "COMPLETED") txState.className = "pill ok";
  if (state === "FAILED" || state === "FAILED_PRE_DEBIT" || state === "REVERSED") txState.className = "pill fail";
}

function prepend(container, text) {
  const p = document.createElement("p");
  p.textContent = text;
  container.prepend(p);
}

function wait(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
