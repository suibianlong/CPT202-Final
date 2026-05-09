const { createInstrumenter } = require("istanbul-lib-instrument");

function buildCoverageAwareSource(source, filename) {
    const instrumenter = createInstrumenter({
        coverageVariable: "__coverage__",
        produceSourceMap: false,
        esModules: false,
        compact: false
    });

    return instrumenter.instrumentSync(source, filename);
}

function evalWithCoverage(source, filename) {
    const normalizedFilename = String(filename).replaceAll("\\", "/");
    const instrumented = buildCoverageAwareSource(source, filename);
    window.eval(`${instrumented}\n//# sourceURL=${normalizedFilename}`);
}

module.exports = {
    evalWithCoverage
};
