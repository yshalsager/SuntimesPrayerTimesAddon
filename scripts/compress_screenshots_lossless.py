# /// script
# requires-python = ">=3.12"
# dependencies = [
#   "pillow",
# ]
# ///

from pathlib import Path

from PIL import Image


def compress_png(path: Path) -> tuple[int, int]:
    before_size = path.stat().st_size
    tmp_path = path.with_suffix('.tmp.png')
    with Image.open(path) as img:
        img.save(tmp_path, format='PNG', optimize=True, compress_level=9)
    after_size = tmp_path.stat().st_size
    if after_size <= before_size:
        tmp_path.replace(path)
    else:
        tmp_path.unlink()
        after_size = before_size
    return before_size, after_size


def main():
    root = Path('fastlane/metadata/android')
    files = sorted(root.glob('*/images/phoneScreenshots/*.png'))
    if not files:
        print('no screenshots found')
        return
    total_before = 0
    total_after = 0
    for path in files:
        before_size, after_size = compress_png(path)
        total_before += before_size
        total_after += after_size
        print(f'{path}: {before_size} -> {after_size}')
    saved = total_before - total_after
    print(f'total: {total_before} -> {total_after} (saved {saved} bytes)')


if __name__ == '__main__':
    main()
