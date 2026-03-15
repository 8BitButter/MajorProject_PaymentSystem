// Chart.js loader for admin dashboard
// Dynamically loads Chart.js and renders charts for KPIs

function loadChartJs(callback) {
  if (window.Chart) return callback();
  const script = document.createElement('script');
  script.src = 'https://cdn.jsdelivr.net/npm/chart.js';
  script.onload = callback;
  document.head.appendChild(script);
}

function renderKpiCharts(data) {
  loadChartJs(() => {
    // Transaction Volume Chart
    const ctxTx = document.getElementById('chartTxVolume').getContext('2d');
    new Chart(ctxTx, {
      type: 'line',
      data: {
        labels: data.txVolume.labels,
        datasets: [{
          label: 'Transactions',
          data: data.txVolume.values,
          borderColor: '#0f766e',
          backgroundColor: 'rgba(15, 118, 110, 0.1)',
          fill: true,
        }]
      },
      options: { responsive: true, plugins: { legend: { display: false } } }
    });

    // Error Rate Chart
    const ctxErr = document.getElementById('chartErrorRate').getContext('2d');
    new Chart(ctxErr, {
      type: 'bar',
      data: {
        labels: data.errorRate.labels,
        datasets: [{
          label: 'Error Rate',
          data: data.errorRate.values,
          backgroundColor: '#b91c1c',
        }]
      },
      options: { responsive: true, plugins: { legend: { display: false } } }
    });

    // GMV Chart
    const ctxGmv = document.getElementById('chartGmv').getContext('2d');
    new Chart(ctxGmv, {
      type: 'line',
      data: {
        labels: data.gmv.labels,
        datasets: [{
          label: 'GMV',
          data: data.gmv.values,
          borderColor: '#15803d',
          backgroundColor: 'rgba(21, 128, 61, 0.1)',
          fill: true,
        }]
      },
      options: { responsive: true, plugins: { legend: { display: false } } }
    });
  });
}

// Example usage: call renderKpiCharts with data from backend
// renderKpiCharts({ txVolume: {labels: [...], values: [...]}, errorRate: {...}, gmv: {...} });
