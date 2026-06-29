const BASE_URL = "http://localhost:8080";

function getToken() {
    return sessionStorage.getItem("token");
}

async function api(url, options = {}) {

    options.headers = {
        "Content-Type": "application/json",
        ...(options.headers || {}),
        Authorization: `Bearer ${getToken()}`
    };

    const response = await fetch(BASE_URL + url, options);

    if (response.status === 401 || response.status === 403) {

        sessionStorage.clear();

        window.location = "/login";

        return;
    }

    if (!response.ok) {

        let message = "Request failed";

        try {
            const error = await response.json();
            message = error.message || message;
        } catch (e) {}

        throw new Error(message);
    }

    if (response.status === 204) {
        return null;
    }

    return response.json();
}

function logout() {
    sessionStorage.clear();
    window.location = "/login";
}