const statusDump = document.getElementById("statusDump");

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

async function refresh() {
  const res = await fetch("/api/admin/status");
  const data = await res.json();
  statusDump.textContent = JSON.stringify(data, null, 2);
}

refresh();

