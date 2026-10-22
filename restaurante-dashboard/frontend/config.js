// Em produção (Vercel) a API é acessada por /api/* (ver vercel.json).
// Em desenvolvimento local o backend roda na porta 8080.
window.API_BASE = ["localhost", "127.0.0.1"].includes(location.hostname) ? "http://localhost:8080" : "";
