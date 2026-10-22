const API = window.API_BASE || "";
const brl = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const $ = (s) => document.querySelector(s);
const esc = (t) => String(t ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const CANAIS = { IFOOD: "iFood", NOVE_NOVE: "99Food", RAPPI: "Rappi", UBER_EATS: "Uber Eats", PROPRIO: "Delivery próprio", MESA: "Mesa" };
const PAGAMENTOS = { PIX: "Pix", CREDITO: "Crédito", DEBITO: "Débito", DINHEIRO: "Dinheiro", VALE_REFEICAO: "Vale-refeição", ONLINE_APP: "Pago no app" };
const PROXIMO = { NOVO: ["EM_PREPARO", "Iniciar preparo"], EM_PREPARO: ["PRONTO", "Marcar pronto"], PRONTO: ["CONCLUIDO", "Concluir"] };
const ROTULO_STATUS = { NOVO: "Novo", EM_PREPARO: "Em preparo", PRONTO: "Pronto" };
const CORES = ["#b8431f", "#3a6ea5", "#2f7d4f", "#c98a12", "#7b4fa0", "#5a8f8f"];

let periodo = "DIARIO";
let abaAtual = "analises";
const graficos = {};
let timer;

const guardar = (t) => { try { t ? sessionStorage.setItem("token", t) : sessionStorage.removeItem("token"); } catch {} };
const token = () => { try { return sessionStorage.getItem("token"); } catch { return null; } };

async function api(path, opts = {}) {
  const headers = { "Content-Type": "application/json" };
  if (token()) headers.Authorization = "Bearer " + token();
  const r = await fetch(API + path, { ...opts, headers });
  if (r.status === 401 && path !== "/api/auth/login") {
    mostrarLogin();
    throw new Error("Sessão expirada. Entre novamente.");
  }
  if (!r.ok) {
    let msg = "Erro " + r.status;
    try { msg = (await r.json()).erro || msg; } catch {}
    throw new Error(msg);
  }
  return r.status === 204 ? null : r.json();
}
function aviso(msg) {
  const el = $("#aviso");
  el.textContent = msg;
  el.hidden = !msg;
  if (msg) setTimeout(() => (el.hidden = true), 6000);
}
function mostrarLogin() {
  clearTimeout(timer);
  guardar(null);
  $("#login").hidden = false;
  $("#conta").hidden = true;
  $("#lSenha").value = "";
  $("#lUser").focus();
}
$("#formLogin").addEventListener("submit", async (e) => {
  e.preventDefault();
  const erro = $("#lErro");
  erro.hidden = true;
  try {
    const r = await api("/api/auth/login", { method: "POST", body: JSON.stringify({ usuario: $("#lUser").value, senha: $("#lSenha").value }) });
    guardar(r.token);
    $("#login").hidden = true;
    $("#conta").hidden = false;
    atualizar();
  } catch (err) {
    erro.textContent = err.message === "Failed to fetch" ? "Sem conexão com o servidor." : err.message;
    erro.hidden = false;
  }
});
$("#btnSair").addEventListener("click", mostrarLogin);
$("#btnSenha").addEventListener("click", async () => {
  const atual = prompt("Senha atual:");
  if (!atual) return;
  const nova = prompt("Nova senha (mínimo 8 caracteres):");
  if (!nova) return;
  try {
    await api("/api/auth/senha", { method: "POST", body: JSON.stringify({ atual, nova }) });
    aviso("Senha alterada.");
  } catch (err) { aviso(err.message); }
});
const seguro = (fn) => async (...a) => { try { await fn(...a); } catch (e) { aviso(e.message === "Failed to fetch" ? "Sem conexão com o servidor." : e.message); } };

/* ---------- Abas ---------- */
document.querySelectorAll("#abas button").forEach((b) => b.addEventListener("click", () => abrirAba(b.dataset.aba)));
function abrirAba(nome) {
  abaAtual = nome;
  document.querySelectorAll("#abas button").forEach((b) => b.classList.toggle("ativa", b.dataset.aba === nome));
  document.querySelectorAll("main > section").forEach((s) => (s.hidden = s.id !== "aba-" + nome));
  atualizar();
}
function atualizar() {
  clearTimeout(timer);
  const f = { analises: carregarAnalises, delivery: carregarDelivery, mesas: carregarMesas }[abaAtual];
  if (f) seguro(f)();
  if (abaAtual === "delivery" || abaAtual === "mesas") timer = setTimeout(atualizar, 15000);
}

/* ---------- Análises ---------- */
$("#data").value = new Date().toLocaleDateString("sv-SE");
$("#data").addEventListener("change", atualizar);
document.querySelectorAll("#periodo button").forEach((b) => b.addEventListener("click", () => {
  periodo = b.dataset.p;
  document.querySelectorAll("#periodo button").forEach((x) => x.classList.toggle("ativa", x === b));
  atualizar();
}));

function grafico(id, tipo, labels, dados, opts = {}) {
  graficos[id]?.destroy();
  const cores = opts.uma ? CORES[0] : labels.map((_, i) => CORES[i % CORES.length]);
  graficos[id] = new Chart($("#" + id), {
    type: tipo,
    data: { labels, datasets: [{ data: dados, backgroundColor: cores, borderWidth: 0, borderRadius: tipo === "bar" ? 4 : 0 }] },
    options: {
      indexAxis: opts.horizontal ? "y" : "x", responsive: true, maintainAspectRatio: false,
      plugins: { legend: { display: tipo === "doughnut", position: "bottom" }, tooltip: { callbacks: { label: (c) => opts.fmt ? opts.fmt(c.parsed.y ?? c.parsed.x ?? c.parsed, c) : c.formattedValue } } },
      scales: tipo === "doughnut" ? {} : { x: { grid: { display: false } }, y: { beginAtZero: true } },
    },
  });
}

async function carregarAnalises() {
  const d = await api(`/api/analises?periodo=${periodo}&data=${$("#data").value}`);
  $("#kpis").innerHTML = [
    ["Faturamento", brl.format(d.faturamento)], ["Pedidos", d.totalPedidos],
    ["Ticket médio", brl.format(d.ticketMedio)], ["Cancelados", d.cancelados],
  ].map(([k, v]) => `<div class="kpi"><small>${k}</small><strong>${v}</strong></div>`).join("");
  $("#tituloSerie").textContent = periodo === "DIARIO" ? "Faturamento por hora" : "Faturamento por dia";
  grafico("gSerie", "bar", d.serie.map((p) => p.rotulo), d.serie.map((p) => p.valor), { uma: true, fmt: (v) => brl.format(v) });
  grafico("gPag", "doughnut", d.pagamentos.map((p) => p.rotulo), d.pagamentos.map((p) => p.pedidos), { fmt: (v, c) => `${c.label}: ${v} pedidos` });
  grafico("gPlat", "bar", d.plataformas.map((p) => p.rotulo), d.plataformas.map((p) => p.pedidos), { horizontal: true });
  grafico("gItens", "bar", d.topItens.map((i) => i.nome), d.topItens.map((i) => i.quantidade), { horizontal: true, uma: true });
}

/* ---------- Pedidos (cartões) ---------- */
function cartaoPedido(p) {
  const prox = PROXIMO[p.status];
  const origem = p.canal === "MESA" ? `Mesa ${p.numeroMesa}` : esc(p.cliente || "Cliente");
  return `<article class="pedido">
    <div>
      <strong>#${p.id} · ${esc(p.canalRotulo)}</strong> <span class="tag ${p.status}">${ROTULO_STATUS[p.status] || p.status}</span>
      <div>${origem}${p.codigoExterno ? " · " + esc(p.codigoExterno) : ""} · ${new Date(p.criadoEm).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })}</div>
      <ul>${p.itens.map((i) => `<li>${i.quantidade}× ${esc(i.nome)}</li>`).join("")}</ul>
    </div>
    <div class="dir">
      <strong>${brl.format(p.total)}</strong>
      ${prox ? `<button data-id="${p.id}" data-st="${prox[0]}" ${prox[0] === "CONCLUIDO" && !p.pagamento ? "disabled title='Feche a conta na aba Mesas'" : ""}>${prox[1]}</button>` : ""}
      <button class="perigo" data-id="${p.id}" data-st="CANCELADO">Cancelar</button>
    </div>
  </article>`;
}
async function mudarStatus(id, status) {
  if (status === "CANCELADO" && !confirm("Cancelar este pedido?")) return;
  await api(`/api/pedidos/${id}/status`, { method: "PATCH", body: JSON.stringify({ status }) });
  atualizar();
}
document.addEventListener("click", seguro(async (e) => {
  const b = e.target.closest("button[data-st]");
  if (b) await mudarStatus(b.dataset.id, b.dataset.st);
}));

/* ---------- Delivery ---------- */
async function carregarDelivery() {
  const [apps, pedidos] = await Promise.all([api("/api/apps"), api("/api/pedidos?abertos=true&delivery=true")]);
  $("#apps").innerHTML = apps.map((a) => `
    <div class="card app" data-id="${a.id}">
      <header><strong>${esc(a.rotulo)}</strong><span class="tag ${a.ativo ? "on" : "off"}">${a.ativo ? "Aberto" : "Pausado"}</span></header>
      <div>${a.pedidosAbertos} pedido(s) em andamento</div>
      <label>Comissão (%) <input type="number" min="0" max="100" step="0.5" value="${a.comissaoPercentual}" data-com></label>
      <label>Receber pedidos <input type="checkbox" ${a.ativo ? "checked" : ""} data-ativo></label>
    </div>`).join("");
  $("#listaDelivery").innerHTML = pedidos.map(cartaoPedido).join("") || `<p class="vazio">Nenhum pedido em andamento.</p>`;
}
$("#apps").addEventListener("change", seguro(async (e) => {
  const card = e.target.closest(".app");
  const body = e.target.matches("[data-ativo]") ? { ativo: e.target.checked } : { comissaoPercentual: Number(e.target.value) };
  await api(`/api/apps/${card.dataset.id}`, { method: "PATCH", body: JSON.stringify(body) });
  carregarDelivery();
}));

/* ---------- Mesas ---------- */
async function carregarMesas() {
  const mesas = await api("/api/mesas");
  $("#mesas").innerHTML = mesas.map((m) => `
    <div class="card mesa" data-mesa="${m.numero}">
      <header><strong>Mesa ${m.numero}</strong><span class="tag">${m.pedidos.length} pedido(s)</span></header>
      ${m.pedidos.map((p) => `<div class="linha"><span>#${p.id} · ${ROTULO_STATUS[p.status] || p.status}</span><span>${brl.format(p.total)}</span></div>
        <ul>${p.itens.map((i) => `<li>${i.quantidade}× ${esc(i.nome)}</li>`).join("")}</ul>
        ${PROXIMO[p.status] && p.status !== "PRONTO" ? `<button data-id="${p.id}" data-st="${PROXIMO[p.status][0]}">${PROXIMO[p.status][1]}</button>` : ""}`).join("")}
      <div class="total">${brl.format(m.total)}</div>
      <select data-pag>${Object.entries(PAGAMENTOS).filter(([k]) => k !== "ONLINE_APP").map(([k, v]) => `<option value="${k}">${v}</option>`).join("")}</select>
      <button data-fechar>Fechar conta</button>
    </div>`).join("") || `<p class="vazio">Nenhuma mesa com conta aberta.</p>`;
}
$("#mesas").addEventListener("click", seguro(async (e) => {
  if (!e.target.matches("[data-fechar]")) return;
  const card = e.target.closest(".mesa");
  if (!confirm(`Fechar a conta da mesa ${card.dataset.mesa}?`)) return;
  await api(`/api/mesas/${card.dataset.mesa}/fechar`, { method: "POST", body: JSON.stringify({ pagamento: card.querySelector("[data-pag]").value }) });
  carregarMesas();
}));

/* ---------- Novo pedido ---------- */
const INDISPONIVEIS = ["IFOOD", "NOVE_NOVE"]; // aguardando acesso às integrações
$("#fCanal").innerHTML = Object.entries(CANAIS).filter(([k]) => !INDISPONIVEIS.includes(k)).map(([k, v]) => `<option value="${k}">${v}</option>`).join("");
$("#fPag").innerHTML += Object.entries(PAGAMENTOS).map(([k, v]) => `<option value="${k}">${v}</option>`).join("");
function ajustaForm() {
  const mesa = $("#fCanal").value === "MESA";
  $("#lMesa").hidden = !mesa; $("#lCliente").hidden = mesa; $("#fMesa").required = mesa;
}
$("#fCanal").addEventListener("change", ajustaForm);
function addItem() {
  const d = document.createElement("div");
  d.className = "item";
  d.innerHTML = `<input placeholder="Item" required maxlength="120"><input type="number" min="1" value="1" required aria-label="Quantidade"><input type="number" min="0.01" step="0.01" placeholder="Preço" required aria-label="Preço"><button type="button" class="perigo" aria-label="Remover item">×</button>`;
  d.querySelector("button").onclick = () => $("#itens").children.length > 1 && d.remove();
  $("#itens").append(d);
}
$("#addItem").addEventListener("click", addItem);
$("#form").addEventListener("submit", seguro(async (e) => {
  e.preventDefault();
  const canal = $("#fCanal").value;
  const body = {
    canal, numeroMesa: canal === "MESA" ? Number($("#fMesa").value) : null,
    cliente: canal === "MESA" ? null : $("#fCliente").value || null, pagamento: $("#fPag").value || null,
    itens: [...$("#itens").children].map((d) => { const [n, q, p] = d.querySelectorAll("input"); return { nome: n.value, quantidade: Number(q.value), precoUnitario: Number(p.value) }; }),
  };
  await api("/api/pedidos", { method: "POST", body: JSON.stringify(body) });
  e.target.reset(); $("#itens").innerHTML = ""; addItem(); ajustaForm();
  abrirAba(canal === "MESA" ? "mesas" : "delivery");
}));
ajustaForm(); addItem();

/* ---------- Início ---------- */
if (token()) { $("#conta").hidden = false; atualizar(); } else mostrarLogin();
