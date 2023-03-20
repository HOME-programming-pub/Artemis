import { registerMultilanguageCoveragePlugin } from '@heddendorp/cypress-plugin-multilanguage-coverage';
import path from 'path';
/// <reference types="cypress" />
// ***********************************************************
// This example plugins/index.js can be used to load plugins
//
// You can change the location of this file or turn off loading
// the plugins file with the 'pluginsFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/plugins-guide
// ***********************************************************

// This function is called when a project is opened or re-opened (e.g. due to
// the project's config changing)

/**
 * @type {Cypress.PluginConfig}
 */
// eslint-disable-next-line @typescript-eslint/no-unused-vars
module.exports = (on: (arg0: string, arg1: {}) => void, config: any) => {
    process.env.CYPRESS_COLLECT_COVERAGE === 'true' &&
        registerMultilanguageCoveragePlugin({ workingDirectory: path.join(__dirname, '..'), saveRawCoverage: true, distributionFile: '../../../build/libs/Artemis-5.11.4.war' })(
            on,
            config,
        );
    // `on` is used to hook into various events Cypress emits
    // `config` is the resolved Cypress config
    on('task', {
        log(message: string) {
            console.log('[37m', 'LOG: ', message, '[0m');
            return null;
        },
        error(message: string) {
            console.error('\x1b[31m', 'ERROR: ', message, '\x1b[0m');
            return null;
        },
        warn(message: string) {
            console.error('\x1b[33m', 'WARNING: ', message, '\x1b[0m');
            return null;
        },
    });
};
