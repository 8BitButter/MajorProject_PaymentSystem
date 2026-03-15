// login.js - Handles login form and JWT storage

document.getElementById('loginForm').addEventListener('submit', async function(e) {
  e.preventDefault();
  const username = document.getElementById('username').value.trim();
  const password = document.getElementById('password').value;
  const errorEl = document.getElementById('loginError');
  errorEl.style.display = 'none';
  try {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });
    const data = await res.json();
    if (!res.ok || !data.token) {
      errorEl.textContent = data.error || 'Invalid credentials';
      errorEl.style.display = 'block';
      return;
    }
    localStorage.setItem('jwt', data.token);
    window.location.href = '/admin.html';
  } catch (err) {
    errorEl.textContent = 'Login failed. Please try again.';
    errorEl.style.display = 'block';
  }
});
