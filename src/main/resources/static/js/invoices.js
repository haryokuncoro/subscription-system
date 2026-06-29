let page = 0;
const size = 10;
let totalPages = 0;

document.addEventListener("DOMContentLoaded", async () => {

    await loadUsers();

    await searchInvoices();

});

async function loadUsers() {

    const response = await api("/api/users");

    const select = document.getElementById("userId");

    response.data.forEach(user => {

        select.innerHTML += `
            <option value="${user.id}">
                ${user.email}
            </option>
        `;

    });

}

async function searchInvoices() {

    const params = new URLSearchParams();

    params.append("page", page);
    params.append("size", size);

    if (userId.value)
        params.append("userId", userId.value);

    if (subscriptionId.value)
        params.append("subscriptionId", subscriptionId.value);

    if (status.value)
        params.append("status", status.value);

    if (stripeInvoiceId.value)
        params.append("stripeInvoiceId", stripeInvoiceId.value);

    const response =
        await api("/api/invoices?" + params.toString());

    renderTable(response.data);

}

function renderTable(pageData) {

    totalPages = pageData.totalPages;

    page = pageData.number;

    pageInfo.innerText =
        `Page ${page + 1} of ${totalPages}`;

    tableBody.innerHTML = "";

    pageData.content.forEach(invoice => {

        tableBody.innerHTML += `

        <tr>

            <td>${invoice.invoiceNumber}</td>

            <td>${statusBadge(invoice.status)}</td>

            <td>${invoice.total}</td>

            <td>${invoice.amountPaid}</td>

            <td>${invoice.currency}</td>

            <td>

                ${formatDate(invoice.periodStart)}

                <br>

                ${formatDate(invoice.periodEnd)}

            </td>

            <td>

                ${
            invoice.invoicePdf
                ? `<a
                           href="${invoice.invoicePdf}"
                           target="_blank"
                           class="btn btn-sm btn-primary">

                           PDF

                       </a>`
                : "-"
        }

            </td>

        </tr>

        `;

    });

}

function previousPage() {

    if (page === 0)
        return;

    page--;

    searchInvoices();

}

function nextPage() {

    if (page >= totalPages - 1)
        return;

    page++;

    searchInvoices();

}

function statusBadge(status) {

    switch (status) {

        case "PAID":
            return '<span class="badge bg-success">PAID</span>';

        case "OPEN":
            return '<span class="badge bg-warning text-dark">OPEN</span>';

        case "DRAFT":
            return '<span class="badge bg-secondary">DRAFT</span>';

        case "VOID":
            return '<span class="badge bg-danger">VOID</span>';

        case "UNCOLLECTIBLE":
            return '<span class="badge bg-dark">UNCOLLECTIBLE</span>';

        default:
            return status;

    }

}

function formatDate(date) {

    if (!date)
        return "";

    return new Date(date).toLocaleString();

}