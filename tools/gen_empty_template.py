#!/usr/bin/env python3
"""Writes an empty structure template (gzipped nbt) for GameTests. usage: gen_empty_template.py <out.nbt> [size]"""
import gzip
import struct
import sys


def name(s):
    b = s.encode('utf-8')
    return struct.pack('>H', len(b)) + b


size = int(sys.argv[2]) if len(sys.argv) > 2 else 3
out = b'\x0a' + name('')
out += b'\x03' + name('DataVersion') + struct.pack('>i', 3955)
out += b'\x09' + name('size') + b'\x03' + struct.pack('>i', 3) + struct.pack('>iii', size, size, size)
out += b'\x09' + name('palette') + b'\x0a' + struct.pack('>i', 1) + b'\x08' + name('Name') + name('minecraft:air') + b'\x00'
out += b'\x09' + name('blocks') + b'\x0a' + struct.pack('>i', 0)
out += b'\x09' + name('entities') + b'\x0a' + struct.pack('>i', 0)
out += b'\x00'
with gzip.open(sys.argv[1], 'wb') as f:
    f.write(out)
print('written', sys.argv[1])
