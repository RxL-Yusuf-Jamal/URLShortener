import axios from "axios";

const api = axios.create({
    baseURL: import.meta.env.VITE_BACKEND_URL,
});

// If an authenticated request comes back 401/403, the token is dead - expired, or revoked by a
// logout (possibly from a different tab). Handle that in one place instead of leaving every
// component to notice and react individually. The login/register endpoints are excluded since
// their 401s (e.g. wrong password) are normal, expected failures already handled by the caller,
// not a sign that a previously-valid session just died.
api.interceptors.response.use(
    (response) => response,
    (error) => {
        const isPublicAuthEndpoint = error.config?.url?.includes("/api/auth/public/");
        const status = error.response?.status;
        if (!isPublicAuthEndpoint && (status === 401 || status === 403)) {
            localStorage.removeItem("JWT_TOKEN");
            window.location.href = "/login";
        }
        return Promise.reject(error);
    }
);

export default api;
