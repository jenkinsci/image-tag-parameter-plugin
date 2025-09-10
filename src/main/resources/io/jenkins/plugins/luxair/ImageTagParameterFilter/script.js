'use strict';

window.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.image-tag-parameter-container').forEach((container) => {
        const selectElement = container.querySelector('.image-tag-parameter-select');
        const filterElement = container.querySelector('.image-tag-parameter-filter');
        const originalOptions = [...selectElement.options]

        filterElement.addEventListener('keyup', () => {
            const keyword = filterElement.value.trim().toLowerCase();

            selectElement.innerHTML = '';

            originalOptions
                .filter(option => option.value.toLowerCase().includes(keyword))
                .forEach(option => selectElement.appendChild(option));

            if (selectElement.firstElementChild) {
                selectElement.firstElementChild.selected = true;
            }
        });
    });
});
