let modal;

document.addEventListener("DOMContentLoaded", async () => {

    modal = new bootstrap.Modal(document.getElementById("planModal"));

    await loadPlans();

});

async function loadPlans() {

    try {

        const response = await api("/api/plans");

        const tbody = document.getElementById("tableBody");

        tbody.innerHTML = "";

        response.data.forEach(plan => {

            tbody.innerHTML += `
                <tr>

                    <td>${plan.name}</td>

                    <td>${plan.description ?? ""}</td>

                    <td>${plan.amount}</td>

                    <td>${plan.currency}</td>

                    <td>${plan.country}</td>

                    <td>${plan.billingInterval}</td>

                    <td>
                        ${badge(plan.active)}
                    </td>

                    <td>

                        <button
                            class="btn btn-warning btn-sm"
                            onclick="edit('${plan.id}')">

                            Edit

                        </button>

                        <button
                            class="btn btn-danger btn-sm"
                            onclick="removePlan('${plan.id}')">

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

function badge(active) {

    if (active) {
        return `<span class="badge bg-success">Active</span>`;
    }

    return `<span class="badge bg-secondary">Inactive</span>`;

}

function clearForm() {

    document.getElementById("id").value = "";

    document.getElementById("name").value = "";

    document.getElementById("description").value = "";

    document.getElementById("amount").value = "";

    document.getElementById("currency").value = "";

    document.getElementById("country").value = "";

    document.getElementById("billingInterval").value = "MONTH";

    document.getElementById("active").checked = true;

}

function openCreateModal() {

    clearForm();

    document.getElementById("modalTitle").innerText = "Create Plan";

    modal.show();

}

async function edit(id) {

    try {

        const response = await api(`/api/plans/${id}`);

        const plan = response.data;

        document.getElementById("modalTitle").innerText = "Edit Plan";

        document.getElementById("id").value = plan.id;

        document.getElementById("name").value = plan.name;

        document.getElementById("description").value =
            plan.description ?? "";

        document.getElementById("amount").value = plan.amount;

        document.getElementById("currency").value = plan.currency;

        document.getElementById("country").value = plan.country;

        document.getElementById("billingInterval").value =
            plan.billingInterval;

        document.getElementById("active").checked = plan.active;

        modal.show();

    } catch (e) {

        alert(e.message);

    }

}

async function save() {

    const id = document.getElementById("id").value;

    const body = {

        name: document.getElementById("name").value,

        description: document.getElementById("description").value,

        amount: Number(document.getElementById("amount").value),

        currency: document.getElementById("currency").value,

        country: document.getElementById("country").value,

        billingInterval:
        document.getElementById("billingInterval").value,

        active:
        document.getElementById("active").checked

    };

    try {

        if (id) {

            await api(`/api/plans/${id}`, {

                method: "PUT",

                body: JSON.stringify(body)

            });

        } else {

            await api("/api/plans", {

                method: "POST",

                body: JSON.stringify(body)

            });

        }

        modal.hide();

        await loadPlans();

    } catch (e) {

        alert(e.message);

    }

}

async function removePlan(id) {

    if (!confirm("Delete this plan?")) {
        return;
    }

    try {

        await api(`/api/plans/${id}`, {

            method: "DELETE"

        });

        await loadPlans();

    } catch (e) {

        alert(e.message);

    }

}