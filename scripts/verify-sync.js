#!/usr/bin/env node
/**
 * verify-sync.js — Cross-platform calculation coefficient sync check
 *
 * Compares numeric constants between:
 *   - app/src/main/java/com/example/seachem_dosing/logic/Calculations.kt   (Kotlin)
 *   - Base_Template/js/utils.js                                            (JS)
 *   - Base_Template/js/dosingCalculations.js                               (JS)
 *
 * Constants in SYNC_KEYS must exist in both Kotlin and JS with values within
 * EPSILON of each other. Any drift fails the build.
 *
 * Run:   node scripts/verify-sync.js
 * Exit:  0 = all in sync, 1 = mismatch/missing, 2 = file load error
 *
 * NOTE: This is a static-text comparison. It evaluates simple arithmetic
 * expressions (digits, dots, +-*\/, parens) but refuses anything else.
 */

'use strict';

const fs = require('fs');
const path = require('path');

const REPO_ROOT = path.resolve(__dirname, '..');
const KOTLIN_FILE = path.join(REPO_ROOT, 'app', 'src', 'main', 'java',
    'com', 'example', 'seachem_dosing', 'logic', 'Calculations.kt');
const JS_UTILS = path.join(REPO_ROOT, 'Base_Template', 'js', 'utils.js');
const JS_CALC = path.join(REPO_ROOT, 'Base_Template', 'js', 'dosingCalculations.js');

// Constants required to be in sync. SeachemCalculations.kt has no JS
// counterpart (no sync invariant there); BigDecimal-based constants there
// will join this list once Phase 5 (web parity) lands.
const SYNC_KEYS = [
    'US_GAL_TO_L', 'UK_GAL_TO_L',
    'CM3_TO_L', 'IN3_TO_L', 'FT3_TO_L',
    'PPM_TO_DH',
    'COEFF_KHCO3_STOICH',
    'COEFF_EQUILIBRIUM',
    'GPL_MIN_NR', 'GPL_MAX_NR',
    'COEFF_ACID',
    'COEFF_GOLD_FULL',
    'COEFF_SAFE',
    'COEFF_APT_80PCT',
    'APT_NITRATE_EST_PER_ML',
    'PRIME_ML_PER_L', 'STABILITY_ML_PER_L'
];

const EPSILON = 1e-9;

function safeEval(expr) {
    if (!/^[\d.\s+\-*/()eE]+$/.test(expr)) {
        throw new Error(`refusing to eval: ${expr}`);
    }
    return Function(`"use strict"; return (${expr});`)();
}

function stripCommentAndType(expr) {
    let s = expr.trim();
    const cIdx = s.indexOf('//');
    if (cIdx !== -1) s = s.slice(0, cIdx);
    s = s.replace(/[fFlL]\b/g, '');
    return s.trim();
}

function extractKotlinConsts(source) {
    const re = /(?:private\s+|internal\s+|public\s+)?const\s+val\s+(\w+)\s*(?::\s*\w+\s*)?=\s*(.+)$/gm;
    const out = {};
    let m;
    while ((m = re.exec(source)) !== null) {
        const name = m[1];
        const expr = stripCommentAndType(m[2]);
        try { out[name] = safeEval(expr); } catch (_) { /* skip non-numeric */ }
    }
    return out;
}

function extractJsConsts(source) {
    const re = /(?:^|\n)\s*const\s+(\w+)\s*=\s*([^;]+);/g;
    const out = {};
    let m;
    while ((m = re.exec(source)) !== null) {
        const name = m[1];
        const expr = stripCommentAndType(m[2]);
        try { out[name] = safeEval(expr); } catch (_) { /* skip non-numeric */ }
    }
    return out;
}

function loadOrFail(p, label) {
    try {
        return fs.readFileSync(p, 'utf8');
    } catch (e) {
        console.error(`[FAIL] ${label}: ${e.message}`);
        process.exit(2);
    }
}

function main() {
    const argv = process.argv.slice(2);
    const emitJson = argv.includes('--emit-json');

    const ktSrc = loadOrFail(KOTLIN_FILE, 'Calculations.kt');
    const utilsSrc = loadOrFail(JS_UTILS, 'utils.js');
    const calcSrc = loadOrFail(JS_CALC, 'dosingCalculations.js');

    const kt = extractKotlinConsts(ktSrc);
    const js = Object.assign({}, extractJsConsts(utilsSrc), extractJsConsts(calcSrc));

    if (emitJson) {
        const payload = {};
        for (const key of SYNC_KEYS) {
            payload[key] = { kotlin: kt[key], js: js[key] };
        }
        process.stdout.write(JSON.stringify(payload, null, 2) + '\n');
        process.exit(0);
    }

    let mismatches = 0, missing = 0, ok = 0;
    const lines = [];

    for (const key of SYNC_KEYS) {
        const ktv = kt[key];
        const jsv = js[key];
        if (ktv === undefined && jsv === undefined) {
            lines.push(`  MISSING_BOTH  ${key}`);
            missing++;
        } else if (ktv === undefined) {
            lines.push(`  MISSING_KT    ${key}: js=${jsv}`);
            missing++;
        } else if (jsv === undefined) {
            lines.push(`  MISSING_JS    ${key}: kt=${ktv}`);
            missing++;
        } else if (Math.abs(ktv - jsv) > EPSILON) {
            lines.push(`  DRIFT         ${key}: kt=${ktv} js=${jsv} delta=${Math.abs(ktv - jsv)}`);
            mismatches++;
        } else {
            lines.push(`  OK            ${key}: ${ktv}`);
            ok++;
        }
    }

    console.log('Calculation Sync Check');
    console.log('======================');
    console.log(`Kotlin: ${path.relative(REPO_ROOT, KOTLIN_FILE)}`);
    console.log(`JS:     ${path.relative(REPO_ROOT, JS_UTILS)}`);
    console.log(`        ${path.relative(REPO_ROOT, JS_CALC)}`);
    console.log(`Epsilon: ${EPSILON}`);
    console.log('');
    lines.forEach(l => console.log(l));
    console.log('');
    console.log(`Total=${SYNC_KEYS.length}  OK=${ok}  Drift=${mismatches}  Missing=${missing}`);

    if (mismatches > 0 || missing > 0) {
        console.error('');
        console.error('FAIL: dosing constants out of sync.');
        console.error('Update Calculations.kt and dosingCalculations.js (or utils.js) atomically.');
        console.error('See CLAUDE.md "calculation sync invariant" for the rule.');
        process.exit(1);
    }

    console.log('');
    console.log('PASS: all shared constants in sync.');
    process.exit(0);
}

main();
