#!/usr/bin/env python3
"""Persistent debug-only identity. Never print secret values or replace an existing key."""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import subprocess
import sys

SECRET_NAMES = ('DEBUG_KEYSTORE_BASE64', 'DEBUG_STORE_PASSWORD', 'DEBUG_KEY_ALIAS',
                'DEBUG_KEY_PASSWORD', 'DEBUG_CERT_SHA256')


class CommandError(RuntimeError):
    """Actionable subprocess failure with secret-safe diagnostics."""


def run(args, **kwargs):
    try:
        return subprocess.run(args, check=True, stdout=subprocess.PIPE,
                              stderr=subprocess.PIPE, **kwargs).stdout
    except subprocess.CalledProcessError as error:
        details = error.stderr or error.stdout or "No diagnostic output"
        if isinstance(details, bytes):
            details = details.decode("utf-8", errors="replace")
        env = kwargs.get("env", os.environ)
        hidden = [env.get(name, "") for name in (
            "DEBUG_STORE_PASSWORD", "DEBUG_KEY_PASSWORD", "DEBUG_KEYSTORE_BASE64",
            "CASHIRO_KEY_PASSWORD", "GH_TOKEN", "GITHUB_TOKEN", "GH_ENTERPRISE_TOKEN")]
        value = kwargs.get("input", "")
        hidden.append(value.decode("utf-8", errors="replace") if isinstance(value, bytes) else value)
        for value in sorted((v for v in hidden if v), key=len, reverse=True):
            details = details.replace(value, "[REDACTED]")
        details = re.sub(r"(?:gh[pousr]_[A-Za-z0-9_]+|github_pat_[A-Za-z0-9_]+)", "[REDACTED]", details)
        program = Path(str(args[0])).name
        if program == "keytool" and "Unable to locate a Java Runtime" in details:
            details = "Java runtime not found. Set JAVA_HOME to an installed JDK and add $JAVA_HOME/bin to PATH, then retry. Keep the existing signing identity."
        raise CommandError(f"{program} failed (exit {error.returncode}): {details.strip()[:2000]}") from None


def keytool():
    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        executable = Path(java_home) / "bin" / "keytool"
        if not executable.is_file():
            raise ValueError("JAVA_HOME does not contain bin/keytool; select an installed JDK")
        return str(executable)
    return "keytool"


def certificate(path, password, alias):
    env = dict(os.environ, CASHIRO_KEY_PASSWORD=password)
    data = run([keytool(), '-exportcert', '-keystore', str(path), '-alias', alias,
                '-storepass:env', 'CASHIRO_KEY_PASSWORD'], env=env)
    return hashlib.sha256(data).hexdigest()


def fingerprint(value):
    normalized = value.replace(':', '').strip().lower()
    if not re.fullmatch(r'[0-9a-f]{64}', normalized):
        raise ValueError('Expected a SHA-256 certificate fingerprint')
    return normalized


def load(directory):
    directory = Path(directory).resolve()
    data = json.loads((directory / 'identity.json').read_text())
    path = directory / 'debug.keystore'
    if certificate(path, data['DEBUG_STORE_PASSWORD'], data['DEBUG_KEY_ALIAS']) != fingerprint(data['DEBUG_CERT_SHA256']):
        raise ValueError('Stored debug identity fingerprint mismatch')
    return path, data


def init(directory):
    directory = Path(directory).resolve()
    if directory.exists():
        load(directory)  # Reuse, or fail closed if incomplete. Never silently rotate.
        print('Existing debug identity verified; unchanged.')
        return
    directory.mkdir(parents=True, mode=0o700)
    os.chmod(directory, 0o700)
    password = secrets.token_urlsafe(32)
    env = dict(os.environ, CASHIRO_KEY_PASSWORD=password)
    path = directory / 'debug.keystore'
    run([keytool(), '-genkeypair', '-noprompt', '-storetype', 'PKCS12',
         '-keystore', str(path), '-alias', 'cashiro-debug', '-keyalg', 'RSA',
         '-keysize', '3072', '-validity', '10000', '-dname', 'CN=Cashiro Debug',
         '-storepass:env', 'CASHIRO_KEY_PASSWORD', '-keypass:env', 'CASHIRO_KEY_PASSWORD'], env=env)
    os.chmod(path, 0o600)
    data = {'DEBUG_STORE_PASSWORD': password, 'DEBUG_KEY_PASSWORD': password,
            'DEBUG_KEY_ALIAS': 'cashiro-debug',
            'DEBUG_CERT_SHA256': certificate(path, password, 'cashiro-debug')}
    config = directory / 'identity.json'
    config.write_text(json.dumps(data, indent=2) + '\n')
    os.chmod(config, 0o600)
    print('Created private debug identity:', directory)
    print('Public certificate SHA-256:', data['DEBUG_CERT_SHA256'])


def prepare_ci():
    missing = [name for name in (*SECRET_NAMES, 'DEBUG_STORE_FILE') if not os.environ.get(name, '').strip()]
    if missing:
        raise ValueError('Missing debug signing settings: ' + ', '.join(missing))
    expected = fingerprint(os.environ['DEBUG_CERT_SHA256'])
    path = Path(os.environ['DEBUG_STORE_FILE'])
    # Never clobber an existing identity, even after a failed rerun.
    path.parent.mkdir(parents=True, exist_ok=True)
    data = base64.b64decode(''.join(os.environ['DEBUG_KEYSTORE_BASE64'].split()), validate=True)
    with path.open('xb') as f:
        os.chmod(path, 0o600)
        f.write(data)
    try:
        actual = certificate(path, os.environ['DEBUG_STORE_PASSWORD'], os.environ['DEBUG_KEY_ALIAS'])
        if actual != expected:
            raise ValueError('Debug certificate SHA-256 does not match the pinned identity')
    except Exception:
        path.unlink(missing_ok=True)
        raise
    print('Persistent debug identity verified:', actual)


def sdk_tool(name):
    root = os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    if not root:
        raise ValueError('ANDROID_HOME or ANDROID_SDK_ROOT is required')
    dirs = sorted((Path(root) / 'build-tools').glob('*'),
                  key=lambda p: tuple(int(v) for v in re.findall(r'\d+', p.name)), reverse=True)
    for directory in dirs:
        if (directory / name).is_file():
            return str(directory / name)
    raise ValueError('Android build tool missing: ' + name)


def apk_certificates(report):
    """Read leaf signer certificates from both legacy and Build Tools 37 reports.

    A certificate can be reported once per signature scheme. Require every
    reported signer to match the pin; never treat an unparsed report as valid.
    """
    certificates = set()
    for line in report.splitlines():
        if 'certificate SHA-256 digest:' not in line:
            continue
        match = re.fullmatch(
            r'(?:Signer #\d+|V\d+(?:\.\d+)? Signer(?: #\d+)?):? '
            r'certificate SHA-256 digest: ([0-9a-fA-F]{64})[ \t]*', line)
        if not match:
            raise ValueError('Unrecognized APK signer certificate report')
        certificates.add(fingerprint(match[1]))
    if not certificates:
        raise ValueError('No APK signer certificate found in apksigner report')
    return certificates


def verify_apk(apk):
    expected = fingerprint(os.environ.get('DEBUG_CERT_SHA256', ''))
    report = run([sdk_tool('apksigner'), 'verify', '--print-certs', apk], text=True)
    certs = apk_certificates(report)
    if certs != {expected}:
        raise ValueError('APK signer differs from pinned debug identity')
    badging = run([sdk_tool('aapt2'), 'dump', 'badging', apk], text=True)
    package = re.search(r"^package: name='([^']+)'", badging, re.M)
    if not package or package[1] != 'com.ritesh.cashiro.debug':
        raise ValueError('Unexpected debug application ID')
    if "application-label:'Cashiro Debug'" not in badging or 'application-debuggable' not in badging:
        raise ValueError('Unexpected debug application label or build type')
    print(json.dumps({'package': package[1], 'certificate_sha256': expected,
                      'label': 'Cashiro Debug', 'signature_verified': True}, indent=2))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest='command', required=True)
    sub.add_parser('init').add_argument('directory')
    sub.add_parser('prepare-ci')
    sub.add_parser('verify-apk').add_argument('apk')
    up = sub.add_parser('upload', help='Upload only debug secrets with an authenticated GitHub CLI')
    up.add_argument('directory')
    up.add_argument('--repo', required=True)
    build = sub.add_parser('build', help='Build locally using the same debug identity as CI')
    build.add_argument('directory')
    build.add_argument('gradle_args', nargs=argparse.REMAINDER)
    args = parser.parse_args()
    if args.command == 'init':
        init(args.directory)
    elif args.command == 'prepare-ci':
        prepare_ci()
    elif args.command == 'verify-apk':
        verify_apk(args.apk)
    else:
        path, data = load(args.directory)
        if args.command == 'upload':
            if not shutil.which('gh'):
                raise ValueError('Install and sign in to GitHub CLI before uploading secrets')
            run(['gh', 'auth', 'status'])
            data['DEBUG_KEYSTORE_BASE64'] = base64.b64encode(path.read_bytes()).decode('ascii')
            for name in SECRET_NAMES:
                run(['gh', 'secret', 'set', name, '--repo', args.repo], input=data[name].encode())
                print('Uploaded', name)  # Names only; secret values go through stdin.
        else:
            env = dict(os.environ, **data, DEBUG_STORE_FILE=str(path))
            root = Path(__file__).resolve().parents[2]
            gradle_args = args.gradle_args or [':app:assembleStandardDebug']
            if gradle_args[0] == '--':
                gradle_args = gradle_args[1:]
            subprocess.run(['./gradlew', *gradle_args, '-PrequireDebugSigning'],
                           cwd=root, env=env, check=True)


if __name__ == '__main__':
    try:
        main()
    except subprocess.CalledProcessError as e:
        # Do not echo subprocess argv/output which could contain signing secrets.
        print('Signing/build command failed (exit %s); identity was not replaced.' % e.returncode, file=sys.stderr)
        sys.exit(1)
    except (CommandError, ValueError, OSError, KeyError) as e:
        print(str(e), file=sys.stderr)
        sys.exit(1)
