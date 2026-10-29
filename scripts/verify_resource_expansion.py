"""Compare every source resource with actual Gradle output and the staged JAR."""
import argparse
import hashlib
import json
from pathlib import Path
import zipfile


def verify_resources(source: Path, processed: Path, jar: Path, version: str) -> dict:
    if not version or '\n' in version or '\r' in version:
        raise ValueError('Expected a non-empty single-line version')
    files = {path.relative_to(source).as_posix(): path for path in source.rglob('*') if path.is_file()}
    if not files or 'plugin.yml' not in files:
        raise ValueError('Missing source resources or plugin.yml')
    output = {path.relative_to(processed).as_posix() for path in processed.rglob('*') if path.is_file()}
    if output != set(files):
        raise ValueError('Processed resource file set differs from source')
    records = {}
    with zipfile.ZipFile(jar) as archive:
        names = archive.namelist()
        for name, path in sorted(files.items()):
            expected = path.read_bytes()
            if name.endswith('.yml'):
                expected = expected.replace(b'${version}', version.encode('utf-8'))
            if (processed / name).read_bytes() != expected:
                raise ValueError('Processed resource bytes differ: ' + name)
            if names.count(name) != 1:
                raise ValueError('Missing or duplicate packaged resource: ' + name)
            if archive.read(name) != expected:
                raise ValueError('Packaged resource bytes differ: ' + name)
            records[name] = hashlib.sha256(expected).hexdigest()
    return {'status': 'PASS', 'version': version, 'resource_count': len(records), 'sha256': records}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, default=Path('src/main/resources'))
    parser.add_argument('--processed', type=Path, default=Path('build/resources/main'))
    parser.add_argument('--jar', type=Path, required=True)
    parser.add_argument('--version', required=True)
    args = parser.parse_args()
    print(json.dumps(verify_resources(args.source, args.processed, args.jar, args.version), indent=2))


if __name__ == '__main__':
    main()
