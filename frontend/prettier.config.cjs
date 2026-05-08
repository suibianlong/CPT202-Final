/** @type {import("prettier").Config} */
module.exports = {
    endOfLine: "lf",
    printWidth: 120,
    tabWidth: 4,
    trailingComma: "none",
    overrides: [
        {
            files: "**/*.json",
            options: {
                tabWidth: 2
            }
        }
    ]
};
