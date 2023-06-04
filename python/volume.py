#!/usr/bin/python3
import argparse
import os
import shutil
from datetime import datetime, timedelta
from glob import glob
import traceback

from pydub import AudioSegment


def get_files(folder, recursive):
    if os.path.isfile(folder):
        return [folder]
    if not recursive:
        return glob(folder + u"/*.mp3")
    matches = []
    for f in glob(folder + u"/*"):
        if f.endswith(u".mp3"):
            matches.append(f)
        elif os.path.isdir(f):
            matches += get_files(f, True)
    return matches


def main(args):
    cut_off_date = None
    if args.since > 0:
        cut_off_date = datetime.now() - timedelta(days=args.since)
        print(u"Only selecting files newer than %s" % cut_off_date)
    files = get_files(args.dir, args.recursive)
    files = [f for f in files if not cut_off_date or datetime.fromtimestamp(os.path.getmtime(f)) > cut_off_date]
    print(u"Total %d files matched" % len(files))
    index = 1
    for mp3 in files:
        if args.show:
            print(u"%s : %s" % (datetime.fromtimestamp(os.path.getmtime(mp3)), mp3))
            continue
        tmp = mp3 + u".tmp"
        try:
            song = AudioSegment.from_mp3(mp3)
            current_dbs = song.dBFS
            dbs_to_add = args.target - current_dbs
            print(u"%d/%d %s: volume %.2f max %.2f, need to adjust %.2f db" % (
                index, len(files), mp3, current_dbs, song.max_dBFS, dbs_to_add))
            if dbs_to_add > args.minChange:
                if args.update:
                    print(u"Exporting %s, add %.2f db" % (mp3, dbs_to_add))
                    song = song + dbs_to_add
                    song.export(tmp, format=u"mp3")
                    shutil.move(tmp, mp3)
                else:
                    print(u"Skip under dry run mode.")
            else:
                print(u"Volume is okay, ignore")
        except Exception as e:
            print(u"%s: failed  due to exception." % mp3)
            traceback.print_exc()
        finally:
            index += 1
            if os.path.exists(tmp):
                os.remove(tmp)


if __name__ == u"__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument(u'--update', default=False, action=u'store_true', help=u'Update it or not.')
    parser.add_argument(u"-r", u'--recursive', default=False, action=u'store_true', help=u'Recursive update')
    parser.add_argument(u"-s", u'--show', default=False, action=u'store_true', help=u'Print files only')
    parser.add_argument(u'--since', type=float, default=-1, help=u'Work only on files days ago')
    parser.add_argument(u'-d', u'--dir', required=True, help=u'Directory to find mp3 files')
    parser.add_argument(u'-t', u'--target', default=-17, type=float, help=u'Target DB')
    parser.add_argument(u'-m', u'--minChange', default=1, type=float, help=u'Min db to add')
    args = parser.parse_args()
    main(args)
