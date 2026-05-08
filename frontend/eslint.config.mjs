import js from "@eslint/js";
import globals from "globals";

export default [
    {
        ignores: ["node_modules/**", "coverage/**", "dist/**"]
    },
    js.configs.recommended,
    {
        files: ["**/*.js"],
        languageOptions: {
            ecmaVersion: "latest",
            sourceType: "script",
            globals: {
                ...globals.browser
            }
        },
        rules: {
            "no-empty": ["error", { allowEmptyCatch: true }],
            "no-unused-vars": "off"
        }
    },
    {
        files: ["tests/**/*.js"],
        languageOptions: {
            sourceType: "commonjs",
            globals: {
                ...globals.browser,
                ...globals.jest,
                ...globals.node
            }
        }
    },
    {
        files: ["**/*.cjs"],
        languageOptions: {
            sourceType: "commonjs",
            globals: {
                ...globals.node
            }
        }
    }
];
