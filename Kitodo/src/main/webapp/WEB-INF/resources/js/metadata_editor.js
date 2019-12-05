/**
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */
/* globals select, setGalleryViewMode, destruct, initialize, scrollToSelectedThumbnail, changeToMapView, PF */

var metadataEditor = {
    dragging: false,
    handleMouseDown(event) {
        let target = $(event.target);
        if (target.closest(".stripe").length === 1) {
            this.stripes.handleMouseDown(event);
        } else if (target.closest(".thumbnail-container").length === 1) {
            this.pages.handleMouseDown(event, target.closest(".thumbnail-container"));
        }
    },
    handleMouseUp(event) {
        let target = $(event.target);
        if (target.closest(".thumbnail-container").length === 1) {
            this.pages.handleMouseUp(event, target.closest(".thumbnail-container"));
        }
    },
    handleDragStart(event) {
        this.pages.handleDragStart(event);
    },
    pages: {
        handleMouseDown(event, target) {
            if (target.closest(".thumbnail-parent").find(".selected").length === 0) {
                this.select(event, target);
            }
        },
        handleMouseUp(event, target) {
            metadataEditor.dragdrop.removeDragAmountIcon();
            if (metadataEditor.dragging) {
                metadataEditor.dragging = false;
            } else if (event.button !== 2 || target.closest(".thumbnail-parent").find(".selected").length === 0) {
                this.select(event, target);
            }
        },
        handleDragStart(event) {
            metadataEditor.dragging = true;
            metadataEditor.dragdrop.addDragAmountIcon(event);
        },
        select(event, target) {
            if (event.metaKey || event.ctrlKey) {
                metadataEditor.select(target[0].dataset.order, target[0].dataset.stripe, "multi");
            } else if (event.shiftKey) {
                metadataEditor.select(target[0].dataset.order, target[0].dataset.stripe, "range");
            } else {
                metadataEditor.select(target[0].dataset.order, target[0].dataset.stripe, "default");
            }
        }
    },
    stripes: {
        handleMouseDown(event) {
            if (!$(event.target).hasClass("selected")) {
                metadataEditor.select(null, event.target.dataset.stripe, "default");
            }
        },
    },
    select(pageIndex, stripeIndex, selectionType) {
        // call the remoteCommand in gallery.xhtml
        select([
            {name: "page", value: pageIndex},
            {name: "stripe", value: stripeIndex},
            {name: "selectionType", value: selectionType}
        ]);
    }
};

metadataEditor.contextMenu = {
    listen() {
        document.oncontextmenu = function() {
            return false;
        };
        $(document).on("mousedown.thumbnail", ".thumbnail-parent", function(event) {
            if (event.originalEvent.button === 2) {
                PF("mediaContextMenu").show(event);
            }
        });
        $(document).on("mousedown.stripe", ".stripe", function(event) {
            if (event.originalEvent.button === 2) {
                PF("stripeContextMenu").show(event);
            }
        });
    }
};

metadataEditor.dragdrop = {
    addDragAmountIcon(event) {
        var dragAmount = document.querySelectorAll(".thumbnail.selected").length;
        if (dragAmount > 1) {
            var element = document.createElement("div");
            element.id = "dragAmount";
            element.innerText = dragAmount;
            event.target.appendChild(element);
        }
    },
    removeDragAmountIcon() {
        var element = document.getElementById("dragAmount");
        if (element !== null) {
            element.parentNode.removeChild(element);
        }
    }
};

metadataEditor.shortcuts = {
    KEYS: {
        HELP: ["Shift", "Minus"],
        STRUCTURED_VIEW: ["Control", "Shift", "Slash"],
        DETAIL_VIEW: ["Control", "Shift", "BracketRight"],
        NEXT_IMAGE: ["Control", "Shift", "ArrowRight"],
        PREVIOUS_IMAGE: ["Control", "Shift", "ArrowLeft"],
        NEXT_IMAGE_MULTI: ["Control", "Shift", "ArrowUp"],
        PREVIOUS_IMAGE_MULTI: ["Control", "Shift", "ArrowDown"]
    },
    getGalleryViewMode() {
        return $("#imagePreviewForm\\:galleryViewMode ").text().toUpperCase();
    },
    changeView(galleryViewMode) {
        if (this.getGalleryViewMode() !== galleryViewMode) {
            setGalleryViewMode([{name: "galleryViewMode", value: galleryViewMode}]);
        }
    },
    jumpToGalleryImage(gallery, index, delta) {
        let newOrder = parseInt(index) + parseInt(delta);
        let mediaList = gallery.find(".thumbnail + .thumbnail-container[data-order='" + newOrder + "']");
        if (mediaList.length === 1) {
            metadataEditor.select(mediaList[0].dataset.order, mediaList[0].dataset.stripe, "default");
            let galleryViewMode = this.getGalleryViewMode();
            if (galleryViewMode === "LIST") {
                scrollToStructureThumbnail(mediaList.first(), $("#imagePreviewForm\\:structuredPagesField"));
            } else if (galleryViewMode === "PREVIEW") {
                scrollToPreviewThumbnail(mediaList.first().prev(), $("#thumbnailStripeScrollableContent"));
            }
            return true;
        }
        return false;
    },
    jumpToSelectedImage(delta) {
        let gallery = $("#galleryWrapperPanel");
        let lastSelection = gallery.find(".thumbnail.last-selection + .thumbnail-container");
        if (lastSelection.length === 1) {
            let order = lastSelection[0].dataset.order;
            if (delta > 0) {
                for (; delta > 0; delta--) {
                    if (this.jumpToGalleryImage(gallery, order, delta)) {
                        break;
                    }
                }
            } else if (delta < 0) {
                for (; delta < 0; delta++) {
                    if (this.jumpToGalleryImage(gallery, order, delta)) {
                        break;
                    }
                }
            }
        }
    },
    handleShortcut(shortcut) {
        switch (shortcut) {
            case "HELP":
                if (!(document.activeElement.tagName === "INPUT" || document.activeElement.tagName === "TEXTAREA")) {
                    PF("helpDialog").show();
                }
                break;
            case "STRUCTURED_VIEW":
                metadataEditor.shortcuts.changeView("LIST");
                break;
            case "DETAIL_VIEW":
                metadataEditor.shortcuts.changeView("PREVIEW");
                break;
            case "NEXT_IMAGE":
                console.log("Next image");
                metadataEditor.shortcuts.jumpToSelectedImage(1);
                break;
            case "PREVIOUS_IMAGE":
                console.log("Previous image");
                metadataEditor.shortcuts.jumpToSelectedImage(-1);
                break;
            case "NEXT_IMAGE_MULTI":
                console.log("Next image multi");
                metadataEditor.shortcuts.jumpToSelectedImage(10);
                break;
            case "PREVIOUS_IMAGE_MULTI":
                console.log("Previous image multi");
                metadataEditor.shortcuts.jumpToSelectedImage(-10);
                break;
            default:
                console.warn("Shortcut '" + shortcut + "' not implemented.");
        }
    },
    evaluateKeys(event) {
        Object.keys(metadataEditor.shortcuts.KEYS).forEach(key => {
            let keyCombination = metadataEditor.shortcuts.KEYS[key];
            if (event.ctrlKey === keyCombination.includes("Control")
                && event.shiftKey === keyCombination.includes("Shift")
                && event.metaKey === keyCombination.includes("Meta")
                && event.altKey === keyCombination.includes("Alt")
                && keyCombination.includes(event.code)
            ) {
                metadataEditor.shortcuts.handleShortcut(key);
                event.preventDefault();
            }
        });
    },
    listen() {
        $(document).on("keydown.shortcuts", function (event) {
            metadataEditor.shortcuts.evaluateKeys(event.originalEvent);
        });
    },
    ignore() {
        $(document).off("keydown.shortcuts");
    },
    updateViews() {
        switch ($("#imagePreviewForm\\:galleryViewMode ").text().toUpperCase()) {
            case "LIST":
                destruct();
                break;
            case "PREVIEW":
                initialize();
                scrollToSelectedThumbnail();
                changeToMapView();
                break;
        }
    }
};

$(document).ready(function () {
    metadataEditor.shortcuts.listen();
    metadataEditor.contextMenu.listen();
});
