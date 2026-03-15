const statusDump = document.getElementById("statusDump");
const dashboardUserId = document.getElementById("dashboardUserId");
const dashboardLimit = document.getElementById("dashboardLimit");
const dashboardTxBody = document.getElementById("dashboardTxBody");
const stateBreakdown = document.getElementById("stateBreakdown");
const kpiTotalTx = document.getElementById("kpiTotalTx");
const kpiLast24h = document.getElementById("kpiLast24h");
const kpiSuccessRate = document.getElementById("kpiSuccessRate");
const kpiCompletedAmount = document.getElementById("kpiCompletedAmount");

document.getElementById("setProfileBtn").addEventListener("click", async () => {
  const profile = document.getElementById("loadProfile").value;
  await fetch("/api/admin/load-profile", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ profile })
  });
  await refresh();
});

document.querySelectorAll(".scenarioBtn").forEach((btn) => {
  btn.addEventListener("click", async () => {
    const scenario = btn.dataset.scenario;
    const enable = btn.dataset.enable === "true";
    await fetch(`/api/admin/failure-scenarios/${scenario}/${enable ? "enable" : "disable"}`, { method: "POST" });
    await refresh();
  });
});

document.getElementById("refreshStatusBtn").addEventListener("click", refresh);
document.getElementById("refreshDashboardBtn").addEventListener("click", refreshDashboard);

async function refresh() {
  try {
    const res = await fetch("/api/admin/status");
    const data = await res.json();
    statusDump.textContent = JSON.stringify(data, null, 2);
  } catch (e) {
    statusDump.textContent = `Failed to load status: ${e.message}`;
  }
}


function getJwt() {
  return localStorage.getItem('jwt');
}

function requireAuth() {
  const jwt = getJwt();
  if (!jwt) {
    window.location.href = '/login.html';
    return false;
  }
  return true;
}

async function refreshDashboard() {
  if (!requireAuth()) return;
  try {
    const limit = dashboardLimit.value;
    const userId = dashboardUserId.value.trim();
    const params = new URLSearchParams({ limit });
    if (userId) params.set("userId", userId);
    const res = await fetch(`/api/admin/dashboard?${params.toString()}`, {
      headers: { 'Authorization': 'Bearer ' + getJwt() }
    });
    const data = await res.json();
    if (res.status === 401) {
      localStorage.removeItem('jwt');
      window.location.href = '/login.html';
      return;
    }
    if (!res.ok) {
      throw new Error(data.error || "dashboard_failed");
    }
    renderDashboard(data);
  } catch (e) {
    dashboardTxBody.innerHTML = `<tr><td colspan="7">Failed to load dashboard: ${e.message}</td></tr>`;
  }
}

function renderDashboard(data) {
  kpiTotalTx.textContent = formatNumber(data.totalTransactions);
  kpiLast24h.textContent = formatNumber(data.transactionsLast24Hours);
  kpiSuccessRate.textContent = `${data.successRatePercent}%`;
  kpiCompletedAmount.textContent = formatCurrency(data.completedAmount);

  // Render KPI charts if data is available
  if (window.renderKpiCharts && data.kpiCharts) {
    renderKpiCharts(data.kpiCharts);
  }

  stateBreakdown.innerHTML = "";
  const sortedStates = Object.keys(data.stateBreakdown || {}).sort();
  for (const state of sortedStates) {
    const count = data.stateBreakdown[state];
    const chip = document.createElement("span");
    chip.className = "chip";
    chip.textContent = `${state}: ${count}`;
    stateBreakdown.appendChild(chip);
  }

  dashboardTxBody.innerHTML = "";
  if (!data.recentTransactions || data.recentTransactions.length === 0) {
    dashboardTxBody.innerHTML = "<tr><td colspan=\"7\">No transactions found for this filter.</td></tr>";
    return;
  }

  for (const tx of data.recentTransactions) {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td title="${tx.transactionId}">${shortId(tx.transactionId)}</td>
      <td>${tx.payerVpa}</td>
      <td>${tx.payeeVpa}</td>
      <td>${formatCurrency(tx.amount)}</td>
      <td>${tx.state}</td>
      <td>${tx.source}</td>
      <td>${formatNumber(tx.processingTimeMs)}</td>
    `;
    dashboardTxBody.appendChild(row);
  }
}

function shortId(value) {
  if (!value) return "NA";
  return value.length > 12 ? `${value.slice(0, 12)}...` : value;
}

function formatNumber(value) {
  if (value === null || value === undefined) return "0";
  return new Intl.NumberFormat("en-IN").format(value);
}

function formatCurrency(value) {
  const amount = Number(value || 0);
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2
  }).format(amount);
}

Promise.all([refresh(), refreshDashboard()]);


