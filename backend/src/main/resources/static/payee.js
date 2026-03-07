let userSocket = null;

const connectionState = document.getElementById("connectionState");
const payeeEventLog = document.getElementById("payeeEventLog");
const latestState = document.getElementById("latestState");
const recipientTx = document.getElementById("recipientTx");

document.getElementById("connectBtn").addEventListener("click", connect);
document.getElementById("disconnectBtn").addEventListener("click", disconnect);

function connect() {
  const user = document.getElementById("payeeUserId").value.trim();
  if (!user) return;
  if (userSocket) userSocket.close();
  const protocol = location.protocol === "https:" ? "wss" : "ws";
  userSocket = new WebSocket(`${protocol}://${location.host}/ws/users/${encodeURIComponent(user)}`);
  userSocket.onopen = () => setState("CONNECTED", "status-ok");
  userSocket.onclose = () => setState("DISCONNECTED", "");
  userSocket.onmessage = (evt) => {
    const msg = JSON.parse(evt.data);
    appendLog(`${msg.createdAt} ${msg.transactionId} ${msg.toState} ${msg.reason}`);
    latestState.textContent = `Latest state: ${msg.toState} (${msg.reason})`;
    prependTx(msg);
  };
}

function disconnect() {
  if (userSocket) userSocket.close();
}

function setState(text, cls) {
  connectionState.textContent = text;
  connectionState.className = `status-pill ${cls}`;
}

function appendLog(text) {
  const p = document.createElement("p");
  p.textContent = text;
  payeeEventLog.prepend(p);
}

function prependTx(msg) {
  const p = document.createElement("p");
  p.textContent = `${msg.transactionId} -> ${msg.toState}`;
  recipientTx.prepend(p);
}

