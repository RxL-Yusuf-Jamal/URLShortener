
import { subDomainList } from "./constant";

// Local-date "today", formatted as YYYY-MM-DD - matches ISO_LOCAL_DATE, what
// the totalClicks endpoint expects for its endDate query param.
export const getTodayDate = () => {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
};

// End of today, formatted as YYYY-MM-DDT23:59:59 - matches ISO_LOCAL_DATE_TIME,
// what the per-link analytics endpoint expects for its endDate query param.
export const getEndOfTodayDateTime = () => `${getTodayDate()}T23:59:59`;

export const getApps = () => {
    const subdomain = getSubDomain(window.location.hostname);

    const mainApp = subDomainList.find((app) => app.main);
    if (subdomain === "") return mainApp.app;

    const apps = subDomainList.find((app) => subdomain === app.subdomain);

    return apps ? apps.app : mainApp.app;
}

// url.localhost
// url.urlbestshort.com
export const getSubDomain = (location) => {
    const locationParts = location.split(".");
    const isLocalhost = locationParts.slice(-1)[0] === "localhost";
    const sliceTill = isLocalhost ? -1 : -2;
    return locationParts.slice(0, sliceTill).join("");
};
