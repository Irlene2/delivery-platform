document.addEventListener("DOMContentLoaded", () => {
    const loginForm = document.getElementById("loginForm");
    const emailInput = document.getElementById("emailInput");
    const passwordInput = document.getElementById("passwordInput");
    const roleInput = document.getElementById("roleInput");

    const emailError = document.getElementById("emailError");
    const passwordError = document.getElementById("passwordError");
    const roleError = document.getElementById("roleError");

    const passwordToggle = document.getElementById("passwordToggle");
    const submitButton = document.getElementById("submitBtn");
    const btnText = document.getElementById("btnText");

    const forgotPassword = document.getElementById("forgotPassword");
    const createAccount = document.getElementById("createAccount");
    const googleLogin = document.getElementById("googleLogin");
    const appleLogin = document.getElementById("appleLogin");

    function clearErrors() {
        emailError.textContent = "";
        passwordError.textContent = "";
        roleError.textContent = "";

        emailInput.classList.remove("input-error");
        passwordInput.classList.remove("input-error");
        roleInput.classList.remove("input-error");
    }

    function isValidEmail(value) {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
    }

    function validateForm() {
        clearErrors();

        let valid = true;
        const email = emailInput.value.trim();
        const password = passwordInput.value.trim();
        const role = roleInput.value;

        if (!email) {
            emailError.textContent = "Digite seu e-mail ou telefone.";
            valid = false;
        } else if (email.includes("@") && !isValidEmail(email)) {
            emailError.textContent = "Digite um e-mail válido.";
            valid = false;
        }

        if (!password) {
            passwordError.textContent = "Digite sua senha.";
            valid = false;
        } else if (password.length < 6) {
            passwordError.textContent = "A senha deve ter pelo menos 6 caracteres.";
            valid = false;
        }

        if (!role) {
            roleError.textContent = "Selecione o tipo de acesso.";
            valid = false;
        }

        return valid;
    }

    passwordToggle.addEventListener("click", () => {
        const isPassword = passwordInput.type === "password";
        passwordInput.type = isPassword ? "text" : "password";

        passwordToggle.querySelector(".material-icons").textContent =
            isPassword ? "visibility" : "visibility_off";

        passwordToggle.setAttribute(
            "aria-label",
            isPassword ? "Ocultar senha" : "Mostrar senha"
        );
    });

    loginForm.addEventListener("submit", (event) => {
        event.preventDefault();

        if (!validateForm()) return;

        submitButton.classList.add("loading");
        submitButton.disabled = true;
        btnText.textContent = "Entrando";

        const role = roleInput.value;

        setTimeout(() => {
            const pages = {
                cliente: "pages/cliente.html",
                administrador: "pages/administrador.html",
                entregador: "pages/entregador.html"
            };

            alert("Login demonstrativo realizado com sucesso!");

            submitButton.classList.remove("loading");
            submitButton.disabled = false;
            btnText.textContent = "Entrar";

            // Redirecionamento para a página escolhida.
            // Remova o comentário da linha abaixo quando quiser ativá-lo:
            // window.location.href = pages[role];
        }, 900);
    });

    forgotPassword.addEventListener("click", (event) => {
        event.preventDefault();
        alert("A recuperação de senha será disponibilizada em uma próxima etapa.");
    });

    createAccount.addEventListener("click", (event) => {
        event.preventDefault();
        alert("A tela de cadastro será disponibilizada em uma próxima etapa.");
    });

    googleLogin.addEventListener("click", () => {
        alert("O login com Google precisa ser conectado a um serviço de autenticação.");
    });

    appleLogin.addEventListener("click", () => {
        alert("O login com Apple precisa ser conectado a um serviço de autenticação.");
    });
});
