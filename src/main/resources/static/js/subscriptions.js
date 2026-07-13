let modal;

let page = 0;

let totalPages = 0;

document.addEventListener("DOMContentLoaded", async () => {

    modal = new bootstrap.Modal(
        document.getElementById("subscriptionModal")
    );

    await loadUsers();

    await loadPlans();

    await searchSubscriptions();

});

function statusBadge(status) {

    switch (status) {

        case "ACTIVE":
            return '<span class="badge bg-success">ACTIVE</span>';

        case "TRIALING":
            return '<span class="badge bg-info text-dark">TRIALING</span>';

        case "PAST_DUE":
            return '<span class="badge bg-warning text-dark">PAST DUE</span>';

        case "CANCELED":
            return '<span class="badge bg-danger">CANCELED</span>';

        case "UNPAID":
            return '<span class="badge bg-dark">UNPAID</span>';

        case "PAUSED":
            return '<span class="badge bg-secondary">PAUSED</span>';

        case "INCOMPLETE":
            return '<span class="badge bg-secondary">INCOMPLETE</span>';

        case "INCOMPLETE_EXPIRED":
            return '<span class="badge bg-secondary">EXPIRED</span>';

        default:
            return `<span class="badge bg-light text-dark">${status}</span>`;
    }

}

function formatDate(date) {

    if (!date) {
        return "";
    }

    return new Date(date).toLocaleString();

}

function toInputDateTime(date) {

    if (!date) {
        return "";
    }

    const d = new Date(date);

    d.setMinutes(d.getMinutes() - d.getTimezoneOffset());

    return d.toISOString().slice(0, 16);

}

async function loadUsers() {

    const response = await api("/api/users");

    const filter = document.getElementById("filterUserId");

    const modal = document.getElementById("userId");

    filter.innerHTML = '<option value="">All Users</option>';

    modal.innerHTML = "";

    response.data.forEach(user => {

        filter.innerHTML += `
            <option value="${user.id}">
                ${user.email}
            </option>
        `;

        modal.innerHTML += `
            <option value="${user.id}">
                ${user.email}
            </option>
        `;

    });

}

async function loadPlans() {

    const response = await api("/api/plans");

    const filter = document.getElementById("filterPlanId");

    const modal = document.getElementById("planId");

    filter.innerHTML = '<option value="">All Plans</option>';

    modal.innerHTML = "";

    response.data.forEach(plan => {

        filter.innerHTML += `
            <option value="${plan.id}">
                ${plan.name}
            </option>
        `;

        modal.innerHTML += `
            <option value="${plan.id}">
                ${plan.name}
            </option>
        `;

    });

}

async function searchSubscriptions() {

    const params = new URLSearchParams();

    params.append("page", page);

    params.append("size", document.getElementById("pageSize").value);

    params.append("sortBy", "createdAt");

    params.append("direction", "desc");

    const userId = document.getElementById("filterUserId").value;

    const planId = document.getElementById("filterPlanId").value;

    const status = document.getElementById("filterStatus").value;

    if (userId)
        params.append("userId", userId);

    if (planId)
        params.append("planId", planId);

    if (status)
        params.append("status", status);

    const response = await api(
        "/api/subscriptions?" + params.toString()
    );

    renderTable(response.data);

}
async function edit(id) {

    try {

        const response = await api(`/api/subscriptions/${id}`);

        const subscription = response.data;

        document.getElementById("modalTitle").innerText =
            "Edit Subscription";

        document.getElementById("id").value =
            subscription.id;

        document.getElementById("userId").value =
            subscription.userId;

        document.getElementById("planId").value =
            subscription.planId;

        document.getElementById("currentPeriodStart").value =
            toInputDateTime(subscription.currentPeriodStart);

        document.getElementById("currentPeriodEnd").value =
            toInputDateTime(subscription.currentPeriodEnd);

        document.getElementById("cancelAtPeriodEnd").checked =
            subscription.cancelAtPeriodEnd;

        modal.show();

    } catch (e) {

        alert(e.message);

    }

}


function openCreateModal() {


    document.getElementById("modalTitle").innerText = "Create Subscription";

    document.getElementById("id").value = "";

    document.getElementById("currentPeriodStart").value = "";

    document.getElementById("currentPeriodEnd").value = "";

    document.getElementById("cancelAtPeriodEnd").checked = false;

    modal.show();

}


async function save() {

    const id = document.getElementById("id").value;

    const currentPeriodStart =
        document.getElementById("currentPeriodStart").value;

    const currentPeriodEnd =
        document.getElementById("currentPeriodEnd").value;

    const body = {

        userId: document.getElementById("userId").value,

        planId: document.getElementById("planId").value,

        currentPeriodStart:
        currentPeriodStart ? new Date(currentPeriodStart).toISOString() : null,

        currentPeriodEnd:
        currentPeriodEnd ? new Date(currentPeriodEnd).toISOString() : null,

        cancelAtPeriodEnd:
        document.getElementById("cancelAtPeriodEnd").checked

    };

    try {

        if (id) {

            await api(`/api/subscriptions/${id}`, {

                method: "PUT",

                body: JSON.stringify(body)

            });

        } else {

            await api("/api/subscriptions", {

                method: "POST",

                body: JSON.stringify(body)

            });

        }

        modal.hide();

        await searchSubscriptions();

    } catch (e) {

        alert(e.message);

    }

}


async function removeSubscription(id) {

    const immediately = confirm(
        "OK = Cancel Immediately\nCancel = Cancel At Period End"
    );

    try {

        await api(
            `/api/subscriptions/${id}?immediately=${immediately}`,
            {
                method: "DELETE"
            }
        );

        await searchSubscriptions();

    } catch (e) {

        alert(e.message);

    }

}

function renderTable(data) {

    page = data.number;

    totalPages = data.totalPages;

    document.getElementById("pageInfo").innerText =
        `Page ${page + 1} of ${totalPages}`;

    const tbody = document.getElementById("tableBody");

    tbody.innerHTML = "";

    data.content.forEach(subscription => {

        tbody.innerHTML += `

        <tr>

            <td>${subscription.userEmail}</td>

            <td>${subscription.planName}</td>

            <td>${statusBadge(subscription.status)}</td>

            <td>

                ${formatDate(subscription.currentPeriodStart)}

                <br>

                ${formatDate(subscription.currentPeriodEnd)}

            </td>

            <td>

                ${
            subscription.cancelAtPeriodEnd
                ? '<span class="badge bg-warning text-dark">Yes</span>'
                : '<span class="badge bg-success">No</span>'
        }

            </td>

            <td>

                <small>

                    ${subscription.stripeSubscriptionId}

                </small>

            </td>

            <td>

                <button
                    class="btn btn-warning btn-sm"
                    onclick="edit('${subscription.id}')">

                    Edit

                </button>

                <button
                    class="btn btn-danger btn-sm"
                    onclick="removeSubscription('${subscription.id}')">

                    Delete

                </button>

            </td>

        </tr>

        `;

    });

}

function previousPage() {

    if (page === 0)
        return;

    page--;

    searchSubscriptions();

}

function nextPage() {

    if (page >= totalPages - 1)
        return;

    page++;

    searchSubscriptions();

}