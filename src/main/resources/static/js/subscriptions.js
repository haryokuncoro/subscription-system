let modal;

document.addEventListener("DOMContentLoaded", async () => {

    modal = new bootstrap.Modal(
        document.getElementById("subscriptionModal")
    );

    await loadUsers();

    await loadPlans();

    await loadSubscriptions();

});

async function loadUsers() {

    const response = await api("/api/users");

    const select = document.getElementById("userId");

    select.innerHTML = "";

    response.data.forEach(user => {

        select.innerHTML += `
            <option value="${user.id}">
                ${user.email}
            </option>
        `;

    });

}

async function loadPlans() {

    const response = await api("/api/plans");

    const select = document.getElementById("planId");

    select.innerHTML = "";

    response.data.forEach(plan => {

        select.innerHTML += `
            <option value="${plan.id}">
                ${plan.name}
            </option>
        `;

    });

}

async function loadSubscriptions() {

    try {

        const response = await api("/api/subscriptions");

        const tbody = document.getElementById("tableBody");

        tbody.innerHTML = "";

        response.data.forEach(subscription => {

            tbody.innerHTML += `
            <tr>

                <td>${subscription.userEmail}</td>

                <td>${subscription.planName}</td>

                <td>${statusBadge(subscription.status)}</td>

                <td>${formatDate(subscription.currentPeriodStart)}</td>

                <td>${formatDate(subscription.currentPeriodEnd)}</td>

                <td>
                    ${subscription.cancelAtPeriodEnd ? "Yes" : "No"}
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

    } catch (e) {

        alert(e.message);

    }

}

function statusBadge(status) {

    switch (status) {

        case "ACTIVE":
            return '<span class="badge bg-success">ACTIVE</span>';

        case "TRIALING":
            return '<span class="badge bg-info">TRIALING</span>';

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

function clearForm() {

    document.getElementById("id").value = "";

    document.getElementById("userId").selectedIndex = 0;

    document.getElementById("planId").selectedIndex = 0;

    document.getElementById("currentPeriodStart").value = "";

    document.getElementById("currentPeriodEnd").value = "";

    document.getElementById("cancelAtPeriodEnd").checked = false;

}

function openCreateModal() {

    clearForm();

    document.getElementById("modalTitle").innerText = "Create Subscription";

    modal.show();

}

async function edit(id) {

    try {

        const response = await api(`/api/subscriptions/${id}`);

        const subscription = response.data;

        document.getElementById("modalTitle").innerText = "Edit Subscription";

        document.getElementById("id").value = subscription.id;

        document.getElementById("userId").value = subscription.userId;

        document.getElementById("planId").value = subscription.planId;

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

async function save() {

    const id = document.getElementById("id").value;

    const body = {

        userId: document.getElementById("userId").value,

        planId: document.getElementById("planId").value,

        currentPeriodStart:
            document.getElementById("currentPeriodStart").value
                ? new Date(document.getElementById("currentPeriodStart").value).toISOString()
                : null,

        currentPeriodEnd:
            document.getElementById("currentPeriodEnd").value
                ? new Date(document.getElementById("currentPeriodEnd").value).toISOString()
                : null,

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

        await loadSubscriptions();

    } catch (e) {

        alert(e.message);

    }

}

async function removeSubscription(id) {

    const immediately = confirm(
        "Press OK to cancel immediately.\nPress Cancel to cancel at period end."
    );

    try {

        await api(
            `/api/subscriptions/${id}?immediately=${immediately}`,
            {
                method: "DELETE"
            }
        );

        await loadSubscriptions();

    } catch (e) {

        alert(e.message);

    }

}