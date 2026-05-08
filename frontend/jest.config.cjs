module.exports = {
    testEnvironment: "jsdom",
    testEnvironmentOptions: {
        url: "http://localhost/"
    },
    roots: ["<rootDir>/tests"],
    testMatch: ["**/*.test.js"],
    clearMocks: true,
    restoreMocks: true,
    collectCoverageFrom: [
        "module1/**/*.js",
        "module2/**/*.js",
        "module3/**/*.js",
        "module5/**/*.js",
        "module6/**/*.js",
        "module7/**/*.js",
        "shared/**/*.js"
    ],
    coverageDirectory: "coverage",
    coverageReporters: [
        "text",
        "html",
        "lcov",
        "clover",
        "json-summary"
    ]
};
