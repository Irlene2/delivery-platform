document.addEventListener("DOMContentLoaded", () => {
    const themeButton = document.querySelector("[data-theme-toggle]");
    const themeIcon = themeButton?.querySelector(".material-icons");

    const savedTheme = localStorage.getItem("aki-theme") || "light";
    document.documentElement.setAttribute("data-theme", savedTheme);

    function updateThemeIcon(theme) {
        if (!themeIcon) return;
        themeIcon.textContent = theme === "dark" ? "light_mode" : "dark_mode";
    }

    updateThemeIcon(savedTheme);

    themeButton?.addEventListener("click", () => {
        const currentTheme = document.documentElement.getAttribute("data-theme");
        const nextTheme = currentTheme === "dark" ? "light" : "dark";

        document.documentElement.setAttribute("data-theme", nextTheme);
        localStorage.setItem("aki-theme", nextTheme);
        updateThemeIcon(nextTheme);
    });
});
