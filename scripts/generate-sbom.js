#!/usr/bin/env node
'use strict';

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const ROOT = path.resolve(__dirname, '..');
const DEFAULT_OUT = path.join(ROOT, 'build', 'reports', 'sbom', 'bom.cdx.json');

function read(file) {
  return fs.readFileSync(path.join(ROOT, file), 'utf8');
}

function parseVersionsToml(source) {
  const versions = {};
  const libraries = {};
  let section = '';

  for (const rawLine of source.split(/\r?\n/)) {
    const line = rawLine.replace(/#.*$/, '').trim();
    if (!line) continue;
    const sectionMatch = line.match(/^\[(.+)]$/);
    if (sectionMatch) {
      section = sectionMatch[1];
      continue;
    }
    if (section === 'versions') {
      const match = line.match(/^([\w-]+)\s*=\s*"([^"]+)"/);
      if (match) versions[match[1]] = match[2];
    } else if (section === 'libraries') {
      const match = line.match(/^([\w-]+)\s*=\s*\{(.+)}$/);
      if (!match) continue;
      const fields = {};
      for (const field of match[2].split(',')) {
        const fieldMatch = field.trim().match(/^([\w.]+)\s*=\s*"([^"]+)"/);
        if (fieldMatch) fields[fieldMatch[1]] = fieldMatch[2];
      }
      libraries[match[1]] = {
        group: fields.group,
        name: fields.name,
        version: fields.version || versions[fields['version.ref']],
      };
    }
  }

  return { libraries };
}

function aliasName(libsExpression) {
  return libsExpression.replace(/^libs\./, '').replace(/\./g, '-');
}

function licenseForMaven(group, name) {
  if (group === 'junit' && name === 'junit') return { id: 'EPL-1.0' };
  if (group === 'org.json' && name === 'json') return { name: 'JSON License' };
  return { id: 'Apache-2.0' };
}

function scopeFor(configuration) {
  return configuration === 'implementation' ? 'required' : 'excluded';
}

function purlMaven(group, name, version) {
  return `pkg:maven/${group}/${name}${version ? `@${version}` : ''}`;
}

function purlNpm(name, version) {
  const encoded = name.startsWith('@') ? `@${name.slice(1).replace('/', '%2F')}` : name;
  return `pkg:npm/${encoded}${version ? `@${version}` : ''}`;
}

function addComponent(components, component) {
  const existing = components.get(component['bom-ref']);
  if (!existing) {
    components.set(component['bom-ref'], component);
    return;
  }
  if (existing.scope !== 'required' && component.scope === 'required') {
    existing.scope = 'required';
  }
}

function parseGradleDependencies() {
  const catalog = parseVersionsToml(read('gradle/libs.versions.toml'));
  const build = read('app/build.gradle.kts');
  const components = new Map();
  const localVals = {};

  for (const match of build.matchAll(/val\s+(\w+)\s*=\s*platform\((libs\.[\w.]+)\)/g)) {
    localVals[match[1]] = match[2];
  }

  const depLine = /^\s*(implementation|debugImplementation|testImplementation|androidTestImplementation|ksp)\((.+)\)/gm;
  for (const match of build.matchAll(depLine)) {
    const configuration = match[1];
    const expression = match[2].replace(/\s*\/\/.*$/, '').trim();
    let coordinate;

    const stringMatch = expression.match(/^"([^"]+)"/);
    const libsMatch = expression.match(/^(libs\.[\w.]+)/);
    const localMatch = expression.match(/^(\w+)$/);

    if (stringMatch) {
      const [group, name, version] = stringMatch[1].split(':');
      coordinate = { group, name, version };
    } else if (libsMatch) {
      coordinate = catalog.libraries[aliasName(libsMatch[1])];
    } else if (localMatch && localVals[localMatch[1]]) {
      coordinate = catalog.libraries[aliasName(localVals[localMatch[1]])];
    }
    if (!coordinate?.group || !coordinate?.name) continue;

    const bomRef = purlMaven(coordinate.group, coordinate.name, coordinate.version);
    addComponent(components, {
      type: 'library',
      'bom-ref': bomRef,
      group: coordinate.group,
      name: coordinate.name,
      ...(coordinate.version ? { version: coordinate.version } : {}),
      purl: bomRef,
      scope: scopeFor(configuration),
      licenses: [{ license: licenseForMaven(coordinate.group, coordinate.name) }],
    });
  }

  return [...components.values()].sort((a, b) => a['bom-ref'].localeCompare(b['bom-ref']));
}

function parseWebDependencies() {
  const lockPath = path.join(ROOT, 'web', 'package-lock.json');
  if (!fs.existsSync(lockPath)) return [];
  const lock = JSON.parse(fs.readFileSync(lockPath, 'utf8'));
  const direct = lock.packages?.['']?.devDependencies || {};

  return Object.keys(direct).sort().map((name) => {
    const pkg = lock.packages?.[`node_modules/${name}`] || {};
    const version = pkg.version || String(direct[name]).replace(/^[^\d]*/, '');
    const bomRef = purlNpm(name, version);
    const license = pkg.license && /^[A-Za-z0-9-.+]+$/.test(pkg.license)
      ? { id: pkg.license }
      : { name: pkg.license || 'NOASSERTION' };
    return {
      type: 'library',
      'bom-ref': bomRef,
      name,
      ...(version ? { version } : {}),
      purl: bomRef,
      scope: 'excluded',
      licenses: [{ license }],
    };
  });
}

function appVersion() {
  return read('app/build.gradle.kts').match(/versionName\s*=\s*"([^"]+)"/)?.[1] || 'unknown';
}

function gitCommit() {
  try {
    return require('child_process')
      .execFileSync('git', ['rev-parse', 'HEAD'], { cwd: ROOT, encoding: 'utf8' })
      .trim();
  } catch (_) {
    return 'unknown';
  }
}

function validateBom(bom) {
  if (bom.bomFormat !== 'CycloneDX') throw new Error('bomFormat must be CycloneDX');
  if (!bom.specVersion) throw new Error('specVersion missing');
  if (!Array.isArray(bom.components) || bom.components.length === 0) {
    throw new Error('components missing');
  }
  const seen = new Set();
  for (const component of bom.components) {
    for (const key of ['type', 'bom-ref', 'name', 'scope']) {
      if (!component[key]) throw new Error(`component missing ${key}`);
    }
    if (seen.has(component['bom-ref'])) throw new Error(`duplicate bom-ref ${component['bom-ref']}`);
    seen.add(component['bom-ref']);
  }
}

function main() {
  const commit = gitCommit();
  const components = [...parseGradleDependencies(), ...parseWebDependencies()];
  const bom = {
    bomFormat: 'CycloneDX',
    specVersion: '1.6',
    serialNumber: `urn:uuid:${crypto.randomUUID()}`,
    version: 1,
    metadata: {
      timestamp: new Date().toISOString(),
      tools: [{ type: 'application', name: 'scripts/generate-sbom.js', version: '1' }],
      component: {
        type: 'application',
        'bom-ref': `pkg:github/GurucharanSavanth/Seachem-Dosing@${commit}`,
        name: 'Seachem Dosing Calculator',
        version: appVersion(),
        licenses: [{ license: { id: 'Apache-2.0' } }],
        externalReferences: [
          { type: 'vcs', url: `https://github.com/GurucharanSavanth/Seachem-Dosing/tree/${commit}` },
        ],
      },
    },
    components,
  };

  validateBom(bom);
  fs.mkdirSync(path.dirname(DEFAULT_OUT), { recursive: true });
  fs.writeFileSync(DEFAULT_OUT, `${JSON.stringify(bom, null, 2)}\n`);
  console.log(`Wrote ${path.relative(ROOT, DEFAULT_OUT)} (${components.length} components).`);
}

main();
