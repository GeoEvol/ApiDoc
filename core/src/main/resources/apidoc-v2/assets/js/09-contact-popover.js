
  var contactToggle = document.querySelector(".ad-contact-toggle");
  var contactPopover = document.getElementById("ad-contact-popover");
  var contactClose = contactPopover ? contactPopover.querySelector(".ad-contact-close") : null;

  function isContactOpen() {
    return !!(contactPopover && !contactPopover.hidden);
  }

  function setContactOpen(open, restoreFocus) {
    if (!contactToggle || !contactPopover) return;

    contactPopover.hidden = !open;
    contactToggle.setAttribute("aria-expanded", open ? "true" : "false");

    if (open) {
      window.requestAnimationFrame(function () {
        if (contactClose) {
          contactClose.focus();
        } else {
          contactPopover.focus();
        }
      });
    } else if (restoreFocus) {
      contactToggle.focus();
    }
  }

  if (contactToggle && contactPopover) {
    contactToggle.addEventListener("click", function () {
      setContactOpen(!isContactOpen(), false);
    });

    if (contactClose) {
      contactClose.addEventListener("click", function () {
        setContactOpen(false, true);
      });
    }

    document.addEventListener("click", function (event) {
      if (!isContactOpen()) return;
      if (contactToggle.contains(event.target)) return;
      if (contactPopover.contains(event.target)) return;
      setContactOpen(false, false);
    });

    document.addEventListener("keydown", function (event) {
      if (event.key !== "Escape") return;
      if (!isContactOpen()) return;

      event.preventDefault();
      setContactOpen(false, true);
    });
  }
