import base64
import contextlib
import io
import os
import subprocess
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import debug_identity as identity


class DebugIdentityTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tmp = tempfile.TemporaryDirectory()
        cls.root = Path(cls.tmp.name)
        cls.directory = cls.root / 'identity'
        with contextlib.redirect_stdout(io.StringIO()):
            identity.init(cls.directory)
        cls.key, cls.data = identity.load(cls.directory)

    @classmethod
    def tearDownClass(cls):
        cls.tmp.cleanup()

    def prepare(self, name, **overrides):
        target = self.root / name
        env = dict(self.data, DEBUG_STORE_FILE=str(target),
                   DEBUG_KEYSTORE_BASE64=base64.b64encode(self.key.read_bytes()).decode())
        env.update(overrides)
        with patch.dict(os.environ, env, clear=True), contextlib.redirect_stdout(io.StringIO()):
            # Preserve executable discovery/JAVA_HOME for keytool, not any signing settings.
            os.environ['PATH'] = self.path
            if self.java_home:
                os.environ['JAVA_HOME'] = self.java_home
            identity.prepare_ci()
        return target

    def setUp(self):
        self.path = os.environ['PATH']
        self.java_home = os.environ.get('JAVA_HOME')

    def test_reinitialization_preserves_key_and_config(self):
        before = [(p.name, p.read_bytes()) for p in self.directory.iterdir()]
        with contextlib.redirect_stdout(io.StringIO()):
            identity.init(self.directory)
        self.assertEqual(before, [(p.name, p.read_bytes()) for p in self.directory.iterdir()])

    def test_two_runners_restore_identical_identity(self):
        first, second = self.prepare('runner-a.keystore'), self.prepare('runner-b.keystore')
        self.assertEqual(first.read_bytes(), second.read_bytes())
        for path in (first, second):
            self.assertEqual(path.stat().st_mode & 0o777, 0o600)
            self.assertEqual(identity.certificate(path, self.data['DEBUG_STORE_PASSWORD'],
                                                 self.data['DEBUG_KEY_ALIAS']), self.data['DEBUG_CERT_SHA256'])

    def test_missing_secret_stops_before_file_creation(self):
        with self.assertRaisesRegex(ValueError, 'Missing debug signing'):
            self.prepare('missing.keystore', DEBUG_KEY_PASSWORD='')
        self.assertFalse((self.root / 'missing.keystore').exists())

    def test_wrong_fingerprint_removes_decoded_key(self):
        with self.assertRaisesRegex(ValueError, 'does not match'):
            self.prepare('wrong.keystore', DEBUG_CERT_SHA256='0' * 64)
        self.assertFalse((self.root / 'wrong.keystore').exists())

    def test_invalid_base64_stops_before_file_creation(self):
        with self.assertRaises(ValueError):
            self.prepare('invalid.keystore', DEBUG_KEYSTORE_BASE64='!invalid!')
        self.assertFalse((self.root / 'invalid.keystore').exists())

    def test_existing_destination_is_never_overwritten(self):
        target = self.root / 'existing.keystore'
        target.write_bytes(b'keep')
        with self.assertRaises(FileExistsError):
            self.prepare('existing.keystore')
        self.assertEqual(target.read_bytes(), b'keep')

    def test_incomplete_identity_is_never_replaced(self):
        target = self.root / 'incomplete'
        target.mkdir()
        with self.assertRaises(FileNotFoundError):
            identity.init(target)
        self.assertEqual(list(target.iterdir()), [])


class DiagnosticsTest(unittest.TestCase):
    def test_missing_java_reports_next_action(self):
        error = subprocess.CalledProcessError(1, ['keytool'], stderr=b'Unable to locate a Java Runtime.')
        with patch.object(identity.subprocess, 'run', side_effect=error):
            with self.assertRaisesRegex(identity.CommandError, 'Set JAVA_HOME'):
                identity.run(['keytool', '-exportcert'])

    def test_failure_redacts_password_token_and_uploaded_input(self):
        error = subprocess.CalledProcessError(1, ['gh'],
            stderr=b'HTTP 403: password-1234 private-keystore-base64 ghp_exampleToken')
        with patch.object(identity.subprocess, 'run', side_effect=error):
            with self.assertRaises(identity.CommandError) as caught:
                identity.run(['gh', 'secret', 'set'], input=b'private-keystore-base64',
                             env={'DEBUG_STORE_PASSWORD': 'password-1234'})
        message = str(caught.exception)
        self.assertIn('gh failed (exit 1)', message)
        self.assertIn('HTTP 403', message)
        for value in ['password-1234', 'private-keystore-base64', 'ghp_exampleToken']:
            self.assertNotIn(value, message)

    def test_keytool_respects_java_home_even_with_default_path(self):
        with tempfile.TemporaryDirectory() as directory:
            tool = Path(directory) / 'bin/keytool'
            tool.parent.mkdir()
            tool.touch()
            with patch.dict(os.environ, {'JAVA_HOME': directory}):
                self.assertEqual(identity.keytool(), str(tool))


if __name__ == '__main__':
    unittest.main()
