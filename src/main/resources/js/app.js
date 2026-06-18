import "scss/app.scss";
import "scss/main.scss";
import $ from "jquery";
import { Tooltip, Tab } from "bootstrap";
import { DataSet, Network } from "vis-network/standalone/esm/vis-network.mjs";
import "jquery-ui/ui/widgets/autocomplete";
import L from "leaflet";

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

    const info = document.getElementById("infotext");
    const icon = document.querySelector(".collapse-icon");

    if (info && icon) {
        info.addEventListener("show.bs.collapse", function () {
            icon.classList.remove("bi-chevron-right");
            icon.classList.add("bi-chevron-down");
        });

        info.addEventListener("hide.bs.collapse", function () {
            icon.classList.remove("bi-chevron-down");
            icon.classList.add("bi-chevron-right");
        });
    }

    const mapElement = document.getElementById("authority-map");

    if (mapElement) {
        let zoom = 10; // default
        const type = mapElement.dataset.type;
        if (type.includes("Country")) {
            zoom = 3;
        } else if (type.includes("MemberState")) {
            zoom = 5;
        }

        const layer = L.tileLayer("https://lobid.org/tiles/{z}/{x}/{y}.png", {
            attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors',
        });

        let [lat, lon] = mapElement.dataset.geo.split(",");
        const center = L.latLng(lat, lon);
        const map = L.map("authority-map", {
            center: center,
            zoom: zoom,
            maxZoom: 17,
            scrollWheelZoom: true,
            attributionControl: true,
            zoomControl: true,
        });

        L.Icon.Default.imagePath = "/gnd/assets/images/leaflet/";
        const marker = L.marker(center, { title: mapElement.dataset.title });

        marker.addTo(map);
        map.addLayer(layer);
    }

    const networkElement = document.getElementById("gnd-network");

    if (networkElement) {
        const nodes = new DataSet(JSON.parse(networkElement.dataset.nodes));
        const edges = new DataSet(JSON.parse(networkElement.dataset.edges));

        const container = document.getElementById("gnd-network");

        const options = {
            interaction: {
                hover: true,
                navigationButtons: false,
                keyboard: false,
            },
            edges: { chosen: false },
            layout: { randomSeed: 2 },
            physics: {
                forceAtlas2Based: {
                    springLength: 175,
                    centralGravity: Math.min(0.0015 * edges.length, 0.015),
                    avoidOverlap: 1,
                },
                solver: "forceAtlas2Based",
                stabilization: { enabled: true },
            },
        };

        const network = new Network(container, { nodes, edges }, options);
        network.selectNodes([networkElement.dataset.entityId], false);

        function changeCursor(cursor) {
            const canvas = container.querySelector("canvas");
            if (canvas) canvas.style.cursor = cursor;
        }

        function valid(t, regex) {
            if (!t) return false;
            var match = t.match(regex);
            return match && match[0].length == t.length;
        }

        function validTarget(target) {
            return valid(target, /^\d.*/);
        }

        function validEdge(edge) {
            return valid(edge, /[a-zA-Z]+/);
        }

        network.on("stabilizationIterationsDone", function () {
            network.setOptions({ physics: false });
            changeCursor("grab");
        });

        network.on("hoverNode", function (params) {
            var target = this.getNodeAt(params.pointer.DOM);
            if (validTarget(target)) {
                changeCursor("pointer");
            }
        });

        network.on("blurNode", function () {
            changeCursor("grab");
        });

        network.on("hoverEdge", function (params) {
            var edge = this.getEdgeAt(params.pointer.DOM).split("_")[0];
            if (validEdge(edge)) {
                changeCursor("pointer");
            }
        });

        network.on("blurEdge", function () {
            changeCursor("grab");
        });

        network.on("click", function (params) {
            const target = this.getNodeAt(params.pointer.DOM);
            if (target && /^\d/.test(target)) {
                window.location.href = target + "#rels";
            } else {
                const edgeId = params.edges[0];
                if (edgeId) {
                    const [rel, to] = edgeId.split("_");
                    if (rel && /^[a-zA-Z]+$/.test(rel)) {
                        window.location.href = `/gnd/search?q=${rel}.id:"https://d-nb.info/gnd/${to}"`;
                    }
                }
            }
        });

        document.getElementById("rels").addEventListener("shown.bs.tab", function () {
            network.fit();
        });

        if (window.location.href.split("#")[1] === "rels") {
            setTimeout(() => {
                new Tab(document.getElementById("rels")).show();
            }, 0);
        }
    }
});

$(function () {
    document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach((trigger) => {
        new Tooltip(trigger);
    });
});

function result(string, link, res) {
    if (res) {
        link.trigger("copied", ["Kopiert: " + string]);
    }
    return res;
}

function copyToClipboard(link) {
    const text = link.data("copy-text");
    const info = link.data("copy-info");
    link.bind("copied", function (event, message) {
        const tooltip = Tooltip.getOrCreateInstance(this);
        tooltip.setContent({ ".tooltip-inner": message });
        tooltip.show();
    });
    link.on("mouseleave", function () {
        const tooltip = Tooltip.getInstance(this);
        if (tooltip) {
            tooltip.hide();
            tooltip.setContent({ ".tooltip-inner": info });
        }
    });
    if (window.clipboardData && window.clipboardData.setData) {
        return result(text, link, clipboardData.setData("Text", text));
    } else if (document.queryCommandSupported && document.queryCommandSupported("copy")) {
        var temp = document.createElement("textarea");
        temp.textContent = text;
        temp.style.position = "fixed";
        document.body.appendChild(temp);
        temp.select();
        try {
            return result(text, link, document.execCommand("copy"));
        } catch (ex) {
            console.warn("Copy to clipboard failed.", ex);
            window.prompt("Kopieren: Strg+C, Enter", text);
        } finally {
            document.body.removeChild(temp);
        }
    }
}

window.copyToClipboard = copyToClipboard;
window.$ = $;
