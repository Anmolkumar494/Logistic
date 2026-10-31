const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => [...document.querySelectorAll(selector)];
const roleLabels = { SUPER_ADMIN: "Super administrator", ADMIN: "Administrator", WAREHOUSE_STAFF: "Warehouse staff", DELIVERY_PERSONNEL: "Delivery personnel" };
let shipments = [];
let currentUser = null;
let toastTimer;

async function api(url, options = {}) {
  const response = await fetch(url, {
    credentials: "same-origin",
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options
  });
  const result = await response.json().catch(() => ({}));
  if (!response.ok) {
    if (response.status === 401) showLogin();
    throw new Error(result.error || `Request failed (${response.status})`);
  }
  return result;
}

function showToast(message, isError = false) {
  const toast = $("#toast");
  toast.textContent = message;
  toast.classList.toggle("error", isError);
  toast.classList.add("visible");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove("visible"), 3200);
}

function showLogin() {
  $("#app").classList.add("hidden");
  $("#login-screen").classList.remove("hidden");
}

function initials(name) {
  return (name || "P").split(/\s+/).slice(0, 2).map((part) => part[0]).join("").toUpperCase();
}

function setUser(user) {
  currentUser = user;
  $("#login-screen").classList.add("hidden");
  $("#app").classList.remove("hidden");
  $("#profile-name").textContent = user.name;
  $("#profile-role").textContent = roleLabels[user.role] || user.role;
  $("#avatar-initials").textContent = initials(user.name);
  $("#top-avatar").textContent = initials(user.name);
  $("#greeting-name").textContent = user.name.split(" ")[0];
  const canCreate = ["SUPER_ADMIN", "ADMIN", "WAREHOUSE_STAFF"].includes(user.role);
  ["#new-shipment", "#new-shipment-alt", "#new-order", "#new-order-alt"].forEach((selector) => {
    const button = $(selector);
    if (button) button.classList.toggle("hidden", !canCreate);
  });
}

function statusPill(status) {
  const key = String(status || "").toLowerCase();
  return `<span class="status-pill status-${key}">${escapeHtml(String(status || "").replaceAll("_", " "))}</span>`;
}

function priorityPill(priority) {
  const key = String(priority || "NORMAL").toLowerCase();
  return `<span class="priority-pill priority-${key}">${escapeHtml(priority || "NORMAL")}</span>`;
}

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, (char) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  })[char]);
}

function renderShipments(rows, target, showActions = false) {
  const body = $(target);
  if (!rows.length) {
    body.innerHTML = `<tr><td colspan="${showActions ? 8 : 5}" class="empty-state">Nothing to see here just yet.</td></tr>`;
    return;
  }
  body.innerHTML = rows.map((s) => `<tr>
    <td><button class="tracking history-link" data-history="${escapeHtml(s.trackingNumber)}">${escapeHtml(s.trackingNumber)}</button></td>
    <td class="recipient">${escapeHtml(s.recipientName)}</td>
    <td>${escapeHtml(s.destination)}</td>
    ${showActions ? `<td>${escapeHtml((s.createdAt || "").slice(0, 10))}</td>` : ""}
    <td>${statusPill(s.status)}</td><td>${priorityPill(s.priority)}</td>
    ${showActions ? `<td>$${Number(s.estimatedCost).toFixed(2)}</td><td>${["DELIVERED", "CANCELLED"].includes(s.status) ? "—" : `<select class="status-select" data-status-for="${escapeHtml(s.trackingNumber)}"><option value="">Update</option>${["PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED", "DELAYED", "CANCELLED"].map((status) => `<option>${status}</option>`).join("")}</select><button class="update-button" data-update="${escapeHtml(s.trackingNumber)}">Save</button>`}</td>` : ""}
  </tr>`).join("");
}

async function loadDashboard() {
  const data = await api("api/dashboard");
  setUser(data.user);
  const metrics = data.metrics;
  $("#metric-orders").textContent = metrics.orders;
  $("#metric-pending").textContent = metrics.pendingOrders;
  $("#metric-active").textContent = metrics.activeShipments;
  $("#metric-delivered").textContent = metrics.delivered;
  $("#metric-delayed").textContent = metrics.delayed;
  $("#metric-stock").textContent = metrics.lowStock;
  $("#insight-delayed").textContent = metrics.delayed;
  $("#insight-stock").textContent = metrics.lowStock;
  $("#nav-active-count").textContent = metrics.activeShipments;
  shipments = data.recentShipments;
  renderShipments(shipments, "#recent-shipments");
}

async function loadShipments() {
  const data = await api("api/shipments");
  shipments = data.shipments;
  renderShipments(shipments, "#all-shipments", true);
}

async function loadOrders() {
  const data = await api("api/orders");
  $("#order-rows").innerHTML = data.orders.map((order) => `<tr>
    <td class="tracking">${escapeHtml(order.orderNumber)}</td><td class="recipient">${escapeHtml(order.customerName)}</td>
    <td>${escapeHtml(order.destination)}</td><td>${Number(order.weightKg).toLocaleString()} kg</td>
    <td>${priorityPill(order.priority)}</td><td>${statusPill(order.status)}</td>
    <td>${escapeHtml((order.createdAt || "").slice(0, 10))}</td>
    <td><button class="adjust-button" data-order-shipment="${escapeHtml(order.id)}" data-recipient="${escapeHtml(order.customerName)}" data-destination="${escapeHtml(order.destination)}" data-weight="${Number(order.weightKg)}">Create shipment →</button></td>
  </tr>`).join("") || `<tr><td colspan="8" class="empty-state">No orders yet. Create your first order to get started.</td></tr>`;
}

async function loadInventory() {
  const data = await api("api/inventory");
  $("#inventory-rows").innerHTML = data.inventory.map((item) => `<tr>
    <td class="recipient">${escapeHtml(item.productName)}</td><td>${escapeHtml(item.sku)}</td>
    <td>${escapeHtml(item.warehouseId)}</td><td>${item.quantity}</td><td>${item.reorderLevel}</td>
    <td>${item.lowStock ? '<span class="inventory-low">Reorder needed</span>' : '<span class="status-pill status-delivered">Healthy</span>'}</td>
    <td><button class="adjust-button" data-adjust="${escapeHtml(item.id)}" data-change="5">＋5</button> <button class="adjust-button" data-adjust="${escapeHtml(item.id)}" data-change="-1">−1</button></td>
  </tr>`).join("") || `<tr><td colspan="7" class="empty-state">No inventory records yet.</td></tr>`;
}

async function loadFleet() {
  const [data, driverData] = await Promise.all([api("api/vehicles"), api("api/drivers")]);
  $("#fleet-grid").innerHTML = data.vehicles.map((v) => `<article class="vehicle-card">
    <div class="vehicle-head"><span class="vehicle-glyph">${v.vehicleType === "BIKE" ? "⌁" : "▰"}</span>${statusPill(v.status)}</div>
    <h3>${escapeHtml(v.registrationNumber)}</h3><p>${escapeHtml(v.vehicleType)} · ${Number(v.capacityKg).toLocaleString()} kg capacity</p>
    <div class="vehicle-details"><span>Cost / km</span><strong>$${Number(v.costPerKm).toFixed(2)}</strong></div>
  </article>`).join("") || `<div class="empty-state">No fleet records yet. Add vehicles in the database seed script.</div>`;
  const canManageDrivers = currentUser && ["ADMIN", "WAREHOUSE_STAFF"].includes(currentUser.role);
  $("#driver-rows").innerHTML = driverData.drivers.map((driver) => `<tr>
    <td class="recipient">${escapeHtml(driver.fullName)}</td><td>${escapeHtml(driver.phone)}</td>
    <td>${escapeHtml(driver.warehouseId)}</td><td>${statusPill(driver.status)}</td>
    <td>${canManageDrivers ? `<button class="adjust-button" data-driver="${escapeHtml(driver.id)}" data-next-status="${driver.status === "AVAILABLE" ? "OFF_DUTY" : "AVAILABLE"}">${driver.status === "AVAILABLE" ? "Set off duty" : "Set available"}</button>` : "—"}</td>
  </tr>`).join("") || `<tr><td colspan="5" class="empty-state">No driver records yet.</td></tr>`;
}

async function openView(name) {
  $$(".view").forEach((view) => view.classList.add("hidden"));
  $(`#view-${name}`).classList.remove("hidden");
  $$(".nav-item").forEach((item) => item.classList.toggle("active", item.dataset.view === name));
  const labels = { overview: "Overview", orders: "Orders", shipments: "Shipments", inventory: "Inventory", fleet: "Fleet & drivers" };
  $("#page-title").textContent = labels[name];
  if (name === "orders") await loadOrders();
  if (name === "shipments") await loadShipments();
  if (name === "inventory") await loadInventory();
  if (name === "fleet") await loadFleet();
}

$("#login-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  $("#login-error").textContent = "";
  const form = new FormData(event.currentTarget);
  try {
    const data = await api("login", { method: "POST", body: JSON.stringify({ username: form.get("username"), password: form.get("password") }) });
    setUser(data.user);
    await loadDashboard();
  } catch (error) {
    $("#login-error").textContent = error.message;
  }
});

$("#logout-button").addEventListener("click", async () => {
  try {
    await api("logout", { method: "POST", body: "{}" });
    showLogin();
    $("#password").value = "";
  } catch (error) {
    showToast(error.message, true);
  }
});

$$(".nav-item").forEach((item) => item.addEventListener("click", () => openView(item.dataset.view).catch((error) => showToast(error.message, true))));
$$("[data-go]").forEach((item) => item.addEventListener("click", () => openView(item.dataset.go).catch((error) => showToast(error.message, true))));
$("#new-shipment").addEventListener("click", () => $("#shipment-dialog").showModal());
$("#new-shipment-alt").addEventListener("click", () => $("#shipment-dialog").showModal());
$("#new-order").addEventListener("click", () => $("#order-dialog").showModal());
$("#new-order-alt").addEventListener("click", () => $("#order-dialog").showModal());
$$(".modal-close").forEach((button) => button.addEventListener("click", () => $("#shipment-dialog").close()));
$$(".order-modal-close").forEach((button) => button.addEventListener("click", () => $("#order-dialog").close()));
$("#shipment-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  $("#shipment-error").textContent = "";
  const values = Object.fromEntries(new FormData(event.currentTarget));
  values.weightKg = Number(values.weightKg);
  values.distanceKm = Number(values.distanceKm);
  if (!values.orderId) delete values.orderId;
  try {
    const shipment = await api("api/shipments", { method: "POST", body: JSON.stringify(values) });
    $("#shipment-dialog").close();
    event.currentTarget.reset();
    showToast(`Shipment ${shipment.trackingNumber} created`);
    await loadDashboard();
    if (!$("#view-shipments").classList.contains("hidden")) await loadShipments();
  } catch (error) {
    $("#shipment-error").textContent = error.message;
  }
});

$("#order-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  $("#order-error").textContent = "";
  const values = Object.fromEntries(new FormData(event.currentTarget));
  values.weightKg = Number(values.weightKg);
  try {
    const order = await api("api/orders", { method: "POST", body: JSON.stringify(values) });
    $("#order-dialog").close();
    event.currentTarget.reset();
    showToast(`Order ${order.orderNumber} created`);
    if (!$("#view-orders").classList.contains("hidden")) await loadOrders();
    await loadDashboard();
  } catch (error) {
    $("#order-error").textContent = error.message;
  }
});

$("#order-rows").addEventListener("click", (event) => {
  const button = event.target.closest("[data-order-shipment]");
  if (!button) return;
  $("#shipment-form").elements.orderId.value = button.dataset.orderShipment;
  $("#shipment-form").elements.recipientName.value = button.dataset.recipient;
  $("#shipment-form").elements.destination.value = button.dataset.destination;
  $("#shipment-form").elements.weightKg.value = button.dataset.weight;
  $("#shipment-dialog").showModal();
});

$("#all-shipments").addEventListener("click", async (event) => {
  const historyButton = event.target.closest("[data-history]");
  if (historyButton) return openHistory(historyButton.dataset.history);
  const button = event.target.closest("[data-update]");
  if (!button) return;
  const tracking = button.dataset.update;
  const select = $(`[data-status-for="${CSS.escape(tracking)}"]`);
  if (!select || !select.value) return showToast("Choose a status first.", true);
  try {
    await api(`api/shipments/${encodeURIComponent(tracking)}/status`, { method: "POST", body: JSON.stringify({ status: select.value }) });
    showToast(`Shipment ${tracking} updated`);
    await loadShipments();
    await loadDashboard();
  } catch (error) {
    showToast(error.message, true);
  }
});

async function openHistory(trackingNumber) {
  $("#history-title").textContent = trackingNumber;
  $("#history-events").innerHTML = `<p class="empty-state">Loading tracking events…</p>`;
  $("#history-dialog").showModal();
  try {
    const data = await api(`api/shipments/${encodeURIComponent(trackingNumber)}/history`);
    $("#history-events").innerHTML = data.events.length ? data.events.map((event) => `<div class="history-event">
      <span class="history-dot"></span><div><strong>${escapeHtml(event.status.replaceAll("_", " "))}</strong><p>${escapeHtml(event.note || "Status updated")}</p><small>${escapeHtml(event.occurredAt.replace("T", " ").slice(0, 16))}</small></div>
    </div>`).join("") : `<p class="empty-state">No tracking events have been recorded.</p>`;
  } catch (error) {
    $("#history-events").innerHTML = `<p class="form-error">${escapeHtml(error.message)}</p>`;
  }
}

$$(".history-close").forEach((button) => button.addEventListener("click", () => $("#history-dialog").close()));

$("#inventory-rows").addEventListener("click", async (event) => {
  const button = event.target.closest("[data-adjust]");
  if (!button) return;
  try {
    await api("api/inventory/adjust", { method: "POST", body: JSON.stringify({ id: button.dataset.adjust, quantityChange: Number(button.dataset.change) }) });
    showToast("Inventory adjustment saved");
    await loadInventory();
  } catch (error) {
    showToast(error.message, true);
  }
});

$("#driver-rows").addEventListener("click", async (event) => {
  const button = event.target.closest("[data-driver]");
  if (!button) return;
  try {
    await api(`api/drivers/${encodeURIComponent(button.dataset.driver)}/availability`, {
      method: "POST", body: JSON.stringify({ status: button.dataset.nextStatus })
    });
    showToast("Driver availability updated");
    await loadFleet();
  } catch (error) {
    showToast(error.message, true);
  }
});

$("#route-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const values = new FormData(event.currentTarget);
  const params = new URLSearchParams({ from: values.get("from"), to: values.get("to") });
  try {
    const result = await api(`api/route?${params}`);
    $("#route-result").innerHTML = `<span>RECOMMENDED ROUTE</span><strong>${result.path.map(escapeHtml).join(" <i>→</i> ")}</strong><small>${result.distanceKm} km · Dijkstra shortest path</small>`;
  } catch (error) {
    showToast(error.message, true);
  }
});

$("#recent-shipments").addEventListener("click", (event) => {
  const button = event.target.closest("[data-history]");
  if (button) openHistory(button.dataset.history);
});

$("#shipment-search").addEventListener("input", (event) => {
  const query = event.target.value.toLowerCase();
  renderShipments(shipments.filter((s) => `${s.trackingNumber} ${s.recipientName} ${s.destination}`.toLowerCase().includes(query)), "#all-shipments", true);
});

$("#today-date").textContent = new Intl.DateTimeFormat("en", { weekday: "short", month: "short", day: "numeric" }).format(new Date());
api("api/dashboard").then((data) => {
  setUser(data.user);
  $("#metric-orders").textContent = data.metrics.orders;
  $("#metric-pending").textContent = data.metrics.pendingOrders;
  $("#metric-active").textContent = data.metrics.activeShipments;
  $("#metric-delivered").textContent = data.metrics.delivered;
  $("#metric-delayed").textContent = data.metrics.delayed;
  $("#metric-stock").textContent = data.metrics.lowStock;
  $("#insight-delayed").textContent = data.metrics.delayed;
  $("#insight-stock").textContent = data.metrics.lowStock;
  $("#nav-active-count").textContent = data.metrics.activeShipments;
  shipments = data.recentShipments;
  renderShipments(shipments, "#recent-shipments");
}).catch(() => showLogin());
