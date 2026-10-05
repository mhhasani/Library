/*
 * Graphical password-strength meter for the password-setting pages (update password and
 * registration). Purely advisory: the realm password policy is what enforces the rules.
 */
(function () {
  "use strict";

  var SEQUENCES = "abcdefghijklmnopqrstuvwxyz0123456789";

  function hasSequence(value) {
    var v = value.toLowerCase();
    for (var i = 0; i + 3 <= v.length; i++) {
      var chunk = v.substr(i, 3);
      if (/(.)\1\1/.test(chunk)) return true;
      if (SEQUENCES.indexOf(chunk) >= 0) return true;
      if (SEQUENCES.indexOf(chunk.split("").reverse().join("")) >= 0) return true;
    }
    return false;
  }

  function score(value) {
    if (!value) return 0;
    var classes = [/[a-z]/, /[A-Z]/, /[0-9]/, /[^A-Za-z0-9]/].filter(function (r) {
      return r.test(value);
    }).length;
    var s = 0;
    if (value.length >= 8) s++;
    if (value.length >= 12) s++;
    s += Math.max(0, classes - 1);
    if (hasSequence(value)) s -= 2;
    return Math.max(0, Math.min(4, s));
  }

  function attach(meter, input) {
    var levels = (meter.getAttribute("data-levels") || "").split("|");
    var label = meter.getAttribute("data-label") || "";
    var bar = document.createElement("div");
    bar.className = "library-strength-bar";
    var fill = document.createElement("div");
    fill.className = "library-strength-fill";
    bar.appendChild(fill);
    var text = document.createElement("span");
    text.className = "library-strength-text";
    meter.appendChild(bar);
    meter.appendChild(text);

    function update() {
      var s = input.value ? score(input.value) : -1;
      meter.setAttribute("data-score", String(s));
      fill.style.width = s < 0 ? "0" : ((s + 1) * 20) + "%";
      text.textContent = s < 0 ? "" : label + " " + (levels[s] || "");
    }
    input.addEventListener("input", update);
    update();
  }

  function init() {
    // Update-password page: meters declared in the template
    document.querySelectorAll("[data-strength-for]").forEach(function (meter) {
      var input = document.getElementById(meter.getAttribute("data-strength-for"));
      if (input) attach(meter, input);
    });
    // Registration page: add a meter under the password field
    var register = document.getElementById("kc-register-form");
    var password = register && register.querySelector("#password");
    if (password && !document.querySelector("[data-strength-for='password']")) {
      var meter = document.createElement("div");
      meter.className = "library-strength";
      meter.setAttribute("data-strength-for", "password");
      var lang = (document.documentElement.lang || "").indexOf("fa") === 0;
      meter.setAttribute("data-label", lang ? "قدرت رمز عبور:" : "Password strength:");
      meter.setAttribute("data-levels", lang
        ? "خیلی ضعیف|ضعیف|متوسط|قوی|خیلی قوی"
        : "Very weak|Weak|Medium|Strong|Very strong");
      var group = password.closest(".pf-v5-c-form__group") || password.parentNode;
      group.appendChild(meter);
      attach(meter, password);
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
