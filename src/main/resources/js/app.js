import "bootstrap";
import "scss/app.scss";
import "scss/main.scss";
import $ from "jquery";
import "jquery-ui/ui/widgets/autocomplete";

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

    // Create custom autocomplete widget: add categories to autocomplete widget
    $.widget("custom.categoryAutocomplete", $.ui.autocomplete, {
        _renderItem: function (ul, item) {
            var labels = "";
            var img = "";
            if (item.image) {
                img = "<img class='hbz-logo' src='" + item.image + "'/>&nbsp;";
            }
            var categories = item.category.split(" | ");
            for (var category in categories) {
                labels +=
                    "&nbsp;<small><span class='badge text-bg-primary'>" +
                    categories[category] +
                    "</span></small>";
            }
            return $("<li></li>")
                .data("item.autocomplete", item)
                .append("<a>" + img + $("<textarea/>").text(item.label).html() + labels + "</a>")
                .appendTo(ul);
        },
    });

    $("#gnd-query").categoryAutocomplete({
        source: function (request, response) {
            $.ajax({
                url: API_BASE_URL + "/search",
                dataType: "jsonp",
                data: {
                    q: request.term,
                    size: 50,
                    format: "json:suggest",
                },
                success: function (data) {
                    response(data);
                },
            });
        },
        focus: function (event, ui) {
            event.preventDefault();
        },
        select: function (event, ui) {
            window.location.href = ui.item.id.replace("https://d-nb.info", "");
            event.preventDefault();
        },
    });

    $(document).on("click", ".facets-toggle", function (event) {
        event.preventDefault();
        const facetKey = $(this).data("facet-key");
        const showMore = $(this).attr("id").includes("more-link");
        const hidden = "d-none";
        if (showMore) {
            $("." + facetKey + "-more-item").removeClass(hidden);
            $("#" + facetKey + "-more-link").addClass(hidden);
            $("#" + facetKey + "-less-link").removeClass(hidden);
        } else {
            $("." + facetKey + "-more-item").addClass(hidden);
            $("#" + facetKey + "-more-link").removeClass(hidden);
            $("#" + facetKey + "-less-link").addClass(hidden);
        }
    });
});
