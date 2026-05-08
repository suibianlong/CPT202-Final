module.exports = {
    testEnvironment: "jsdom",
    testEnvironmentOptions: {
        url: "http://localhost/"
    },
    roots: ["<rootDir>/tests"],
    testMatch: ["**/*.test.js"],
    clearMocks: true,
    restoreMocks: true,
    collectCoverageFrom: ["shared/**/*.js"]
};
