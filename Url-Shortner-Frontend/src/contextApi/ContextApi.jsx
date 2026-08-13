
import { createContext, useContext, useEffect, useState } from "react";

const ContextApi = createContext();

export const ContextProvider = ({ children }) => {
    const getToken = localStorage.getItem("JWT_TOKEN")
        ? JSON.parse(localStorage.getItem("JWT_TOKEN"))
        : null;

    const [token, setToken] = useState(getToken);

    // The storage event fires in every OTHER tab (never the one that made the change) whenever
    // localStorage changes, so a tab that logs out can tell every other open tab to log out too -
    // otherwise they'd keep rendering as "logged in" until their next API call happens to fail.
    useEffect(() => {
        const onStorageChange = (event) => {
            if (event.key === "JWT_TOKEN" && event.newValue === null) {
                setToken(null);
                window.location.href = "/login";
            }
        };
        window.addEventListener("storage", onStorageChange);
        return () => window.removeEventListener("storage", onStorageChange);
    }, []);

    const sendData = {
        token,
        setToken,
    };

    return <ContextApi.Provider value={sendData}>{children}</ContextApi.Provider>
};


export const useStoreContext = () => {
    const context = useContext(ContextApi);
    return context;
}
