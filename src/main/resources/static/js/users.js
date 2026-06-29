let modal;

document.addEventListener("DOMContentLoaded", async () => {

    modal = new bootstrap.Modal(
        document.getElementById("userModal")
    );

    await loadUsers();

});

async function loadUsers() {

    const response = await api("/api/users");

    const tbody = document.getElementById("tableBody");

    tbody.innerHTML = "";

    response.data.forEach(user => {

        tbody.innerHTML += `

        <tr>

            <td>${user.email}</td>

            <td>${user.fullName}</td>

            <td>${user.country}</td>

            <td>

                <small>

                    ${user.stripeCustomerId ?? "-"}

                </small>

            </td>
            <td>${statusBadge(user.active)}</td>

            <td>

                <button
                        class="btn btn-warning btn-sm"
                        onclick="edit('${user.id}')">

                    Edit

                </button>

                <button
                        class="btn btn-danger btn-sm"
                        onclick="removeUser('${user.id}')">

                    Delete

                </button>

            </td>

        </tr>

        `;

    });

}

function statusBadge(status) {

    switch (status) {
        case true:
            return '<span class="badge bg-success">ACTIVE</span>';
        case false:
            return '<span class="badge bg-danger">INACTIVE</span>';
        default:
            return `<span class="badge bg-light text-dark">${status}</span>`;
    }

}


function clearForm() {

    document.getElementById("id").value = "";

    document.getElementById("email").value = "";

    document.getElementById("fullName").value = "";

    document.getElementById("password").value = "";

    document.getElementById("country").value = "";

}

function openCreateModal() {

    clearForm();

    document.getElementById("modalTitle").innerText =
        "Create Customer";

    modal.show();

}

async function edit(id) {

    const response = await api(`/api/users/${id}`);

    const user = response.data;

    document.getElementById("modalTitle").innerText =
        "Edit Customer";

    document.getElementById("id").value = user.id;

    document.getElementById("email").value = user.email;

    document.getElementById("fullName").value =
        user.fullName;

    document.getElementById("country").value =
        user.country;

    document.getElementById("password").value = "";

    modal.show();

}

async function save() {

    const id = document.getElementById("id").value;

    const body = {

        email:
        document.getElementById("email").value,

        fullName:
        document.getElementById("fullName").value,

        country:
        document.getElementById("country").value

    };

    const password =
        document.getElementById("password").value;

    if (password) {
        body.password = password;
    }

    if (id) {

        await api(`/api/users/${id}`, {

            method: "PUT",

            body: JSON.stringify(body)

        });

    } else {

        body.password = password;

        await api("/api/users", {

            method: "POST",

            body: JSON.stringify(body)

        });

    }

    modal.hide();

    await loadUsers();

}

async function removeUser(id) {

    if (!confirm("Delete this customer?"))
        return;

    await api(`/api/users/${id}`, {

        method: "DELETE"

    });

    await loadUsers();

}