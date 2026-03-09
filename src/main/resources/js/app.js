import "bootstrap";
import "scss/app.scss";
import "scss/main.scss";

document.addEventListener("DOMContentLoaded", function () {
    const searchBox = document.getElementById("gnd-query");
    const clearButton = document.querySelector(".ui-autocomplete-clear");
    if (searchBox && clearButton) {
        function updateClearButton() {
            clearButton.style.display = searchBox.value ? "inline-block" : "none";
        }
        clearButton.addEventListener("click", function () {
            searchBox.value = "";
            updateClearButton();
            searchBox.focus();
        });
        searchBox.addEventListener("input", updateClearButton);
        updateClearButton();
    }
});
